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

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.JmsHeaders.EVENT_TYPE;
import static org.fcrepo.camel.JmsHeaders.IDENTIFIER;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fcrepo.camel.processor.SparqlUpdateProcessor;
import org.fcrepo.client.FcrepoLink;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;;

/**
 * Listens for updates to Fedora objects and indexes service docs.
 * <p>
 * Retrieves the service doc of added/updated objects and sends to triple store. Whenever an extension is added or
 * updated, it initiates a re-index of all objects
 * </p>
 * 
 * @author apb@jhu.edu
 */
public class ServiceIndexingRoutes extends RouteBuilder {

    static final String ROUTE_TRIGGER_REINDEX = "direct:reindex.trigger";

    static final String ROUTE_INDEX_SERVICE_DOC = "direct:index.services";

    static final String ROUTE_PERFORM_INDEX = "direct:index.services.perform";

    static final String ROUTE_DELETE_SERVICE_DOC = "direct:delete.services";

    static final String HEADER_SERVICE_DOC = "CamelApixServiceDocument";

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";

    private static final Logger LOG = LoggerFactory.getLogger(ServiceIndexingRoutes.class);

    private String extensionContainer;

    private String reindexStream;

    /**
     * set the extension container path.
     *
     * @param path
     */
    public void setExtensionContainer(final String path) {
        this.extensionContainer = path;
    }

    /**
     * Set the reindex stream.
     *
     * @param stream
     */
    public void setReindexStream(final String stream) {
        this.reindexStream = stream;
    }

    @Override
    public void configure() throws Exception {

        from("{{service.index.stream}}")
                .routeId("index-services")

                .choice().when(header(IDENTIFIER).contains("#")).stop().end()

                // If any members of an extension registry are updated, reindex all objects
                .choice().when(header(IDENTIFIER).startsWith(extensionContainer))
                .enrich(ROUTE_TRIGGER_REINDEX, (i, o) -> i)
                .end()

                .choice()
                .when(header(EVENT_TYPE).isEqualTo(RESOURCE_DELETION))
                .to(ROUTE_DELETE_SERVICE_DOC)
                .otherwise()
                .to(ROUTE_INDEX_SERVICE_DOC);

        // Reindex sends object ID in CamelFcrepoIdentifier header,
        // not org.fcrepo.jms.identifier as sent by Fedora
        from("{{service.reindex.stream}}")
                .routeId("reindex-services")
                .setHeader(IDENTIFIER, header("CamelFcrepoIdentifier"))
                .to(ROUTE_INDEX_SERVICE_DOC);

        from(ROUTE_INDEX_SERVICE_DOC)
                .routeId("index-service-doc")
                .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
                .setHeader(Exchange.HTTP_PATH, header(IDENTIFIER))
                .setHeader("Accept", constant("application/n-triples"))
                .to("{{apix.baseUrl}}")
                .process(GET_SERVICE_DOC_HEADER)
                .split(header(HEADER_SERVICE_DOC)).to(ROUTE_PERFORM_INDEX);

        from(ROUTE_PERFORM_INDEX).routeId("perform-index")
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_URI, bodyAs(URI.class))
                .setBody(constant(null))
                .to("http://localhost")
                .removeHeaders("CamelHttp*")
                .setHeader(FCREPO_NAMED_GRAPH, simple("{{triplestore.namedGraph}}"))
                .process(new SparqlUpdateProcessor())
                .log(LoggingLevel.INFO, LOG,
                        "Indexing service doc of ${headers[org.fcrepo.jms.identifier]}")
                .to("{{triplestore.baseUrl}}");

        from(ROUTE_TRIGGER_REINDEX).id("trigger-reindex")
                .log(LoggingLevel.INFO, LOG,
                        "Triggering reindex to " + reindexStream +
                                " due update to extension ${headers[org.fcrepo.jms.identifier]}")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant(String.format("[\"%s\"]", reindexStream)))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("{{reindexing.service.uri}}");
    }

    @SuppressWarnings("unchecked")
    final Processor GET_SERVICE_DOC_HEADER = ex -> {

        final Set<String> rawLinkHeaders = new HashSet<>();

        final Object linkHeader = ex.getIn().getHeader("Link");

        if (linkHeader instanceof Collection) {
            rawLinkHeaders.addAll((Collection<String>) linkHeader);
        } else if (linkHeader instanceof String) {
            rawLinkHeaders.add((String) linkHeader);
        }

        final List<URI> services = rawLinkHeaders.stream()
                .map(FcrepoLink::new)
                .filter(l -> l.getRel().equals("service"))
                .map(l -> l.getUri()).collect(Collectors.toList());

        ex.getIn().setHeader(HEADER_SERVICE_DOC, services);
    };

}
