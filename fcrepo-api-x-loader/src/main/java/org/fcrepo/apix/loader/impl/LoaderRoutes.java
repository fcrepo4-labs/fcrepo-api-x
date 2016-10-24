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

import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;

import java.io.InputStream;
import java.net.URI;

import org.fcrepo.apix.model.WebResource;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;

/**
 * @author apb@jhu.edu
 */
public class LoaderRoutes extends RouteBuilder {

    private static final String HEADER_SERVICE_URI = "service.uri";

    private static final String ROUTE_LOAD = "direct:load";

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
        restConfiguration().component("jetty").host("0.0.0.0").port("{{loader.port}}");

        rest("/load")
                .get()
                .produces("text/html").to("language:simple:resource:classpath:form.html")

                .post().consumes("application/x-www-form-urlencoded").route().to(ROUTE_LOAD);

        from(ROUTE_LOAD).id("load-service-uri")
                .removeHeaders("*", HEADER_SERVICE_URI).setBody(constant(null))
                .setHeader(Exchange.HTTP_METHOD, constant("OPTIONS"))
                .setHeader(Exchange.HTTP_URI, header(HEADER_SERVICE_URI))
                .setHeader("Accept", constant("text/turtle"))
                .to("jetty:http://localhost")
                .process(DEPOSIT_OBJECTS)
                .setHeader(HTTP_RESPONSE_CODE, constant(303))
                .setHeader("Location", constant("http://example.org"));
    }

    private final Processor DEPOSIT_OBJECTS = ex -> {

        final URI location = loaderService.load(WebResource.of(
                ex.getIn().getBody(InputStream.class),
                ex.getIn().getHeader("Content-Type", "text/turtle", String.class),
                URI.create(ex.getIn().getHeader(Exchange.HTTP_URI, String.class)), null));

        System.out.println("LOCATION IS " + location);

        ex.getOut().setHeader(HTTP_RESPONSE_CODE, 303);
        ex.getOut().setHeader("Location", location);
    };

}
