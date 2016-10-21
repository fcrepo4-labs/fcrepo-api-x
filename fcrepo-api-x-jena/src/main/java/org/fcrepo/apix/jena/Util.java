/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.apix.jena;

import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ResourceNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RDF utility functions.
 *
 * @author apb@jhu.edu
 */
public abstract class Util {

    private static final Logger LOG = LoggerFactory.getLogger(Util.class);

    /**
     * Parse serialized rdf into a jena Model.
     *
     * @param r resource containing serialized rdf
     * @return The model
     */
    public static Model parse(final WebResource r) {

        if (r instanceof JenaResource && ((JenaResource) r).model() != null) {
            return ((JenaResource) r).model();
        }

        final Model model =
                ModelFactory.createDefaultModel();

        final Lang lang = RDFLanguages.contentTypeToLang(r.contentType());

        LOG.debug("Parsing rdf from {}", r.uri());
        try (InputStream representation = r.representation()) {
            RDFDataMgr.read(model, representation, r.uri() != null ? r.uri().toString() : "", lang);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return model;
    }

    /**
     * Object resources of triples with the given subject and predicates.
     *
     * @param s Subject
     * @param p Predicate
     * @param model Model to search in
     * @return All matching object resources, as URIs.
     */
    public static List<URI> objectResourcesOf(final String s, final String p, final Model model) {
        return model.listStatements(
                model.getResource(s),
                model.getProperty(p),
                (RDFNode) null)
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isResource)
                .mapWith(RDFNode::asResource)
                .mapWith(Resource::getURI)
                .mapWith(URI::create)
                .toList();
    }

    /**
     * Object literals of triples with the given subject and predicates.
     *
     * @param s Subject
     * @param p Predicate
     * @param model Model to search in
     * @return All matching object literals, as strings
     */
    public static List<String> objectLiteralsOf(final String s, final String p, final Model model) {
        return model.listStatements(
                s != null ? model.getResource(s) : null,
                model.getProperty(p),
                (RDFNode) null)
                .mapWith(Statement::getObject)
                .filterKeep(RDFNode::isLiteral)
                .mapWith(RDFNode::asLiteral)
                .mapWith(Literal::getString)
                .toList();
    }

    /**
     * Single object literal of triples with the given subject and predicates.
     *
     * @param s Subject
     * @param p Predicate
     * @param model Model to search in
     * @return matching object literal, or runtime exception if there is more than one match.
     */
    public static String objectLiteralOf(final String s, final String p, final Model model) {
        return one(s, p, objectLiteralsOf(s, p, model));
    }

    /**
     * Single object resource of triples with the given subject and predicates.
     *
     * @param s Subject
     * @param p Predicate
     * @param model Model to search in
     * @return matching object resource
     * @throws ResourceNotFoundException if the number of matching resources is not exactly one.
     */
    public static URI objectResourceOf(final String s, final String p, final Model model) {
        return one(s, p, objectResourcesOf(s, p, model));
    }

    private static <T> T one(final String s, final String p, final List<T> list) {

        if (list.size() > 1) {
            throw new ResourceNotFoundException(String.format(
                    "Expected number of predicates in <%s>, <%s>, ? to be 0 or 1;  encountered  %d", s, p, list
                            .size()));
        }

        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Returns true if the given subject is of the given type.
     *
     * @param type Type URI
     * @param subject Subject URI
     * @param model model to search in
     * @return True if the subject is the given type.
     */
    public static boolean isA(final String type, final String subject, final Model model) {
        return objectResourcesOf(subject, RDF_TYPE, model).contains(URI.create(type));
    }

    /**
     * Produce N-triples string.
     *
     * @param s Subject URI
     * @param p Predicate URI
     * @param o Object URI
     * @return N-triples string.
     */
    public static String triple(final String s, final String p, final String o) {
        return String.format("<%s> <%s> <%s> .\n", s, p, o);
    }

    /**
     * Produce N-triples string.
     *
     * @param s Subject URI
     * @param p Predicate URI
     * @param o Object literal
     * @return N-triples string.
     */
    public static String ltriple(final String s, final String p, final String o) {
        return String.format("<%s> <%s> \"%s\" .\n", s, p, o);
    }

    /**
     * Perform a sparql query against a model.
     *
     * @param sparql A sparql CONSTRUCT query
     * @param model the model
     * @return Stream of matching triples.
     */
    public static Stream<Triple> query(final String sparql, final Model model) {
        final Iterable<Triple> i = () -> QueryExecutionFactory.create(QueryFactory.create(sparql), model)
                .execConstructTriples();
        return StreamSupport.stream(i.spliterator(), false);
    }

    /**
     * Find the set of subjects of the given triples.
     *
     * @param triples Stream of triples
     * @return Set of subject URIs.
     */
    public static Set<URI> subjectsOf(final Stream<Triple> triples) {
        return triples
                .map(Triple::getSubject)
                .map(Node::getURI)
                .map(URI::create)
                .collect(Collectors.toSet());
    }

    /**
     * Get all subjects of statements with the given predicate and object.
     *
     * @param p predicate URI
     * @param o object URI
     * @param model model to search in
     * @return list of subjects
     */
    public static List<URI> subjectsOf(final String p, final String o, final Model model) {
        return model.listSubjectsWithProperty(model.getProperty(p), model.getResource(o)).mapWith(
                Resource::getURI).mapWith(URI::create).toList();
    }

    /**
     * Get the singular subject of a statement with the given predicate and object.
     *
     * @param p predicate URI
     * @param o object URI
     * @param model model to search in
     * @return the matching URI
     * @throws ResourceNotFoundException if there is not exactly one match
     */
    public static URI subjectOf(final String p, final String o, final Model model) {
        final List<URI> subjects = subjectsOf(p, o, model);

        if (subjects.size() != 1) {
            throw new ResourceNotFoundException(
                    String.format("Expecting to find exactly one subject of ? <%s> <%s>", p, o));
        }

        return subjects.get(0);
    }

    /**
     * Create an N-triples resource.
     *
     * @param uri URI of the resource
     * @param rdf String containing n-triples.
     * @return web resource containing the given serialization.
     */
    public static WebResource rdfResource(final String uri, final String rdf) {

        return new WebResource() {

            @Override
            public void close() throws Exception {
                // nothing
            }

            @Override
            public URI uri() {
                return URI.create(uri);
            }

            @Override
            public InputStream representation() {
                return IOUtils.toInputStream(rdf, Charset.forName("UTF-8"));
            }

            @Override
            public Long length() {
                return 0l;
            }

            @Override
            public String contentType() {
                return "application/n-triples";
            }
        };
    }
}
