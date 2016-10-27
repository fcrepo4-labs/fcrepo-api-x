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

package org.fcrepo.apix.indexing.impl;

import static java.net.URLEncoder.encode;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.fcrepo.camel.processor.ProcessorUtils.langFromMimeType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.processor.ProcessorUtils;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.jena.rdf.model.Model;

/**
 * Represents a processor for creating the sparql-update message to be passed to an external triplestore.
 *
 * @author Aaron Coburn
 * @since Nov 8, 2014
 */
public class SparqlUpdateProcessor implements Processor {

    /**
     * Define how the message is processed.
     *
     * @param exchange the current camel message exchange
     */
    @Override
    public void process(final Exchange exchange) throws IOException {

        final Message in = exchange.getIn();

        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String subject = ProcessorUtils.getSubjectUri(in);
        final String namedGraph = in.getHeader(FcrepoHeaders.FCREPO_NAMED_GRAPH, String.class);
        final Model model = createDefaultModel().read(in.getBody(InputStream.class), subject,
                langFromMimeType(in.getHeader(Exchange.CONTENT_TYPE, String.class)));

        model.write(serializedGraph, "N-TRIPLE");

        final StringBuilder query = new StringBuilder();
        query.append(ProcessorUtils.deleteWhere(subject, namedGraph));
        query.append(";\n");
        query.append(ProcessorUtils.insertData(serializedGraph.toString("UTF-8"), namedGraph));

        in.setBody("update=" + encode(query.toString(), "UTF-8"));
        in.setHeader(Exchange.HTTP_METHOD, "POST");
        in.setHeader(Exchange.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
    }
}
