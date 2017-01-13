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

import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.builder.PredicateBuilder.or;
import static org.apache.http.entity.ContentType.parse;
import static org.apache.jena.riot.RDFLanguages.contentTypeToLang;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_NAMED_GRAPH;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fcrepo.camel.processor.EventProcessor;
import org.fcrepo.client.FcrepoLink;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.common.HttpOperationFailedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    static final String ROUTE_TRIGGER_REINDEX = "direct:trigger-reindex";

    static final String ROUTE_INDEX_PREPARE = "direct:index-service-doc";

    static final String ROUTE_EVENT_PROCESOR = "direct:event-processor";

    static final String ROUTE_GET_SERVICE_DOC_URI = "direct:get-servicedoc-uri";

    static final String ROUTE_PERFORM_INDEX = "direct:perform-index";

    static final String ROUTE_PERFORM_DELETE = "direct:perform-delete";

    static final String ROUTE_DELETE_SERVICE_DOC = "direct:delete-service-doc";

    static final String HEADER_SERVICE_DOC = "CamelApixServiceDocument";

    private static final String FCR_DELETE = "http://fedora.info/definitions/v4/event#ResourceDeletion";

    private static final String AS_DELETE = "https://www.w3.org/ns/activitystreams#Delete";

    private static final Logger LOG = LoggerFactory.getLogger(ServiceIndexingRoutes.class);

    private String extensionContainer;

    private String reindexStream;

    private static final EventProcessor EVENT_PROCESSOR = new EventProcessor();

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
                .routeId("from-index-stream")
                .to(ROUTE_EVENT_PROCESOR)

                // At the moment, this seems to be the only way to filter out messages for "hash resources"
                .filter(not(header(FCREPO_URI).contains("#")))

                // If any members of an extension registry are updated, reindex all objects
                .choice().when(header(FCREPO_URI).startsWith(extensionContainer))
                .enrich(ROUTE_TRIGGER_REINDEX, (i, o) -> i)
                .end()

                .choice()
                .when(or(header(FCREPO_EVENT_TYPE).contains(FCR_DELETE),
                        header(FCREPO_EVENT_TYPE).contains(AS_DELETE)))
                .to(ROUTE_DELETE_SERVICE_DOC)
                .otherwise()
                .to(ROUTE_INDEX_PREPARE);

        // It's set up this way to make testing a little easier.
        from(ROUTE_EVENT_PROCESOR)
                .routeId("event-processor")
                .process(EVENT_PROCESSOR);

        from("{{service.reindex.stream}}")
                .routeId("from-reindex-stream")
                .to(ROUTE_INDEX_PREPARE);

        from(ROUTE_GET_SERVICE_DOC_URI)
                .routeId("get-servicedoc-uri")
                .setHeader(Exchange.HTTP_METHOD, constant("HEAD"))
                .setHeader(Exchange.HTTP_URI, header(FCREPO_URI))
                .setHeader("Accept", constant("application/n-triples"))

                // This is annoying, no easy way around
                .doTry()
                .to("{{apix.baseUrl}}")
                .doCatch(HttpOperationFailedException.class)
                .to("direct:410")
                .doFinally()
                .process(GET_SERVICE_DOC_HEADER);

        from("direct:410")
                .id("handle-error")
                .choice()
                .when(e -> e.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class)
                        .getStatusCode() != 410)
                .process(e -> {
                    throw e.getProperty(Exchange.EXCEPTION_CAUGHT, HttpOperationFailedException.class);
                });

        from(ROUTE_INDEX_PREPARE)
                .routeId("prepare-for-index")
                .to(ROUTE_GET_SERVICE_DOC_URI)
                .split(header(HEADER_SERVICE_DOC)).to(ROUTE_PERFORM_INDEX);

        from(ROUTE_DELETE_SERVICE_DOC)
                .routeId("delete-service-doc")
                .to(ROUTE_GET_SERVICE_DOC_URI)
                .split(header(HEADER_SERVICE_DOC)).to(ROUTE_PERFORM_DELETE);

        from(ROUTE_PERFORM_DELETE)
                .routeId("perform-delete")
                .setHeader(FCREPO_NAMED_GRAPH, bodyAs(URI.class))
                .process(SPARQL_DELETE_PROCESSOR)
                .log(LoggingLevel.INFO, LOG,
                        "Deleting service doc of ${headers[CamelFcrepoUri]}")
                .to("{{triplestore.baseUrl}}");

        from(ROUTE_PERFORM_INDEX)
                .routeId("perform-index")
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_URI, bodyAs(URI.class))
                .setBody(constant(null))
                .to("http://localhost")
                .setHeader(FCREPO_NAMED_GRAPH, header(Exchange.HTTP_URI))
                .removeHeaders("CamelHttp*")
                .process(SPARQL_UPDATE_PROCESSOR)
                .log(LoggingLevel.INFO, LOG,
                        "Indexing service doc of ${headers[CamelFcrepoUri]}")
                .to("{{triplestore.baseUrl}}");

        from(ROUTE_TRIGGER_REINDEX)
                .id("trigger-reindex")
                .log(LoggingLevel.INFO, LOG,
                        "Triggering reindex to " + reindexStream +
                                " due update to extension ${headers[CamelFcrepoUri]}")
                .removeHeaders("*")
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant(String.format("[\"%s\"]", reindexStream)))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .to("{{reindexing.service.uri}}");
    }

    @SuppressWarnings("unchecked")
    static final Processor GET_SERVICE_DOC_HEADER = ex -> {

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

    private static final Processor SPARQL_UPDATE_PROCESSOR = ex -> {
        final ByteArrayOutputStream serializedGraph = new ByteArrayOutputStream();
        final String graph = ex.getIn().getHeader(FCREPO_NAMED_GRAPH).toString();

        final Model model = ModelFactory.createDefaultModel();

        RDFDataMgr.read(model, ex.getIn().getBody(InputStream.class),
                contentTypeToLang(parse(ex.getIn().getHeader(Exchange.CONTENT_TYPE, String.class)).getMimeType()));

        model.write(serializedGraph, "N-TRIPLE");

        ex.getIn().setBody(deleteGraph(graph) + ";\n" +
                insertGraph(serializedGraph.toString("utf8"), graph));

        ex.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        ex.getIn().setHeader("Content-Type", "application/sparql-update");
    };

    private static final Processor SPARQL_DELETE_PROCESSOR = ex -> {
        final String graph = ex.getIn().getHeader(FCREPO_NAMED_GRAPH).toString();

        ex.getIn().setBody(deleteGraph(graph));

        ex.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        ex.getIn().setHeader("Content-Type", "application/sparql-update");
    };

    static String deleteGraph(final String namedGraph) throws UnsupportedEncodingException {
        return "DELETE WHERE { " +
                "GRAPH <" + namedGraph + "> {" +
                "?s ?p ?o" +
                "}}";
    }

    static String insertGraph(final String content, final String namedGraph) throws UnsupportedEncodingException {
        return "INSERT DATA { " +
                "GRAPH <" + namedGraph + "> {" +
                content +
                "}}";
    }

}
