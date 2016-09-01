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

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.fcrepo.apix.model.WebResource;

import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;

/**
 * Random useful building blocks for testing.
 *
 * @author apb@jhu.edu
 */
public abstract class TestUtil {

    public static String triple(final String s, final String p, final String o) {
        return String.format("<%s> <%s> <%s> .\n", s, p, o);
    }

    public static String ltriple(final String s, final String p, final String o) {
        return String.format("<%s> <%s> \"%s\" .\n", s, p, o);
    }

    public static Stream<Triple> query(final String sparql, final Model model) {
        final Iterable<Triple> i = () -> QueryExecutionFactory.create(QueryFactory.create(sparql), model)
                .execConstructTriples();
        return StreamSupport.stream(i.spliterator(), false);
    }

    public static Set<URI> subjectsOf(final Stream<Triple> triples) {
        return triples
                .map(Triple::getSubject)
                .map(Node::getURI)
                .map(URI::create)
                .collect(Collectors.toSet());
    }

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
