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

package org.fcrepo.apix.jena.impl;

import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.fcrepo.apix.model.WebResource;

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
abstract class Util {

    static final Logger LOG = LoggerFactory.getLogger(Util.class);

    static Model parse(final WebResource r) {

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

    static List<URI> objectResourcesOf(final String s, final String p, final Model model) {
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

    static List<String> objectLiteralsOf(final String s, final String p, final Model model) {
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

    static String objectLiteralOf(final String s, final String p, final Model model) {
        return one(s, p, objectLiteralsOf(s, p, model));
    }

    static URI objectResourceOf(final String s, final String p, final Model model) {
        return one(s, p, objectResourcesOf(s, p, model));
    }

    private static <T> T one(final String s, final String p, final List<T> list) {

        if (list.size() > 1) {
            throw new RuntimeException(String.format(
                    "Expected number of predicates in <%s>, <%s>, ? to be 0 or 1;  encountered  %d", s, p, list
                            .size()));
        }

        return list.isEmpty() ? null : list.get(0);
    }

    static boolean isA(final String type, final String subject, final Model model) {
        return objectResourcesOf(subject, RDF_TYPE, model).contains(URI.create(type));
    }
}
