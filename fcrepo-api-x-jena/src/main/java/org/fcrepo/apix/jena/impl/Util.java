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

import java.io.IOException;
import java.io.InputStream;

import org.fcrepo.apix.model.WebResource;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
class Util {

    private Util() {
    };

    static final Logger LOG = LoggerFactory.getLogger(Util.class);

    static Model parse(final WebResource r) {
        final Model model =
                ModelFactory.createDefaultModel();

        final Lang lang = RDFLanguages.contentTypeToLang(r.contentType());

        LOG.debug("Parsing rdf from {}", r.uri());
        try (InputStream representation = r.representation()) {
            RDFDataMgr.read(model, representation, r.uri() != null ? r.uri().toString() : null, lang);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return model;
    }
}
