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

package org.fcrepo.apix.loader.impl;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.apix.model.WebResource;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

/**
 * Routes which provide an HTTP api to the extension loader.
 *
 * @author apb@jhu.edu
 */
public class LoaderRoutes extends RouteBuilder {

    private static final String HEADER_SERVICE_URI = "service.uri";

    private static final String ROUTE_LOAD = "direct:load";

    private static final String ROUTE_PREPARE_LOAD = "direct:prepare_load";

    private static final String ROUTE_NO_SERVICE = "direct:no_service";

    private static final String ROUTE_OPTIONS = "direct:options";

    LoaderService loaderService;

    /**
     * Set the loader service.
     *
     * @param svc The service
     */
    public void setLoaderService(final LoaderService svc) {
        this.loaderService = svc;
    }

    @Override
    public void configure() throws Exception {

        from("jetty:http://0.0.0.0:{{loader.port}}/load" +
                "?matchOnUriPrefix=true&sendServerVersion=false&httpMethodRestrict=GET,OPTIONS,POST")
                        .id("loader-http")
                        .choice()

                        .when(header(Exchange.HTTP_METHOD).isEqualTo("GET"))
                        .setHeader("Content-Type", constant("text/html"))
                        .to("language:simple:resource:classpath:form.html")

                        .when(header(Exchange.HTTP_METHOD).isEqualTo("POST"))
                        .to(ROUTE_PREPARE_LOAD)

                        .otherwise().to(ROUTE_OPTIONS);

        from(ROUTE_OPTIONS).id("respond-options")
                .setHeader(CONTENT_TYPE).constant("text/turtle")
                .setHeader("Allow").constant("GET,OPTIONS,POST")
                .setHeader("Accept-Post").constant("application/x-www-form-urlencoded,text/plain")
                .to("language:simple:resource:classpath:options.ttl");

        from(ROUTE_PREPARE_LOAD).id("load-prepare")
                .choice().when(header("Content-Type").isEqualTo("text/plain"))
                .setHeader(HEADER_SERVICE_URI, bodyAs(String.class))
                .end()

                .choice().when(header(HEADER_SERVICE_URI).isNull()).to(ROUTE_NO_SERVICE)
                .otherwise().to(ROUTE_LOAD);

        from(ROUTE_LOAD).id("load-service-uri")
                .removeHeaders("*", HEADER_SERVICE_URI).setBody(constant(null))
                .setHeader(Exchange.HTTP_METHOD, constant("OPTIONS"))
                .setHeader(Exchange.HTTP_URI, header(HEADER_SERVICE_URI))
                .setHeader("Accept", constant("text/turtle"))
                .to("jetty:http://localhost")
                .process(DEPOSIT_OBJECTS)
                .setHeader(HTTP_RESPONSE_CODE, constant(303));

        from(ROUTE_NO_SERVICE)
                .setBody(constant("No service URI provided"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400));
    }

    private final Processor DEPOSIT_OBJECTS = ex -> {

        final URI location = loaderService.load(WebResource.of(
                ex.getIn().getBody(InputStream.class),
                ex.getIn().getHeader("Content-Type", "text/turtle", String.class),
                URI.create(ex.getIn().getHeader(Exchange.HTTP_URI, String.class)), null));

        ex.getOut().setHeader(HTTP_RESPONSE_CODE, 303);
        ex.getOut().setHeader("Location", location);
    };

}
