/**
 * Copyright 2015 Amherst College
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fcrepo.apix.poc.amherst;

import static org.apache.camel.Exchange.HTTP_PATH;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.fcrepo.apix.poc.amherst.ApixHeaders.SERVICE_ENDPOINT;
import static org.fcrepo.apix.poc.amherst.ApixHeaders.SERVICE_NAME;
import static org.fcrepo.apix.poc.amherst.ApixHeaders.SERVICE_REGISTRATION;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import org.apache.camel.builder.RouteBuilder;

/**
 * A content router for API-X.
 *
 * @author Aaron Coburn
 */
public class ApixRouter extends RouteBuilder {

    /**
     * Configure the message route workflow.
     */
    public void configure() throws Exception {

        /**
         * REST routing
         */
        rest("{{rest.registry}}")
            .get("/").to("direct:list")
            .get("/{service}").to("direct:describe")
            .put("/{service}").to("direct:register")
            .delete("/{service}").to("direct:unregister");

        rest("{{rest.binding}}")
            .get("/").to("direct:bindings")
            .get("/{service}").to("direct:list-bindings")
            .post("/{service}").to("direct:bind")
            .delete("/{service}/{identifier}").to("direct:unbind");

        from("direct:bindings")
            .routeId("ApixBindings")
            .setBody().constant("FIXME: Here is some information about the binding endpoint...");

        from("jetty:http://{{rest.host}}:{{rest.port}}{{rest.proxy}}?matchOnUriPrefix=true")
            .routeId("ApixRest")
            .routeDescription("The main API-X client REST interface")
            .process(new ServiceProcessor())
            .choice()
                .when(header(SERVICE_NAME).isEqualTo("list"))
                    .setHeader("Link").simple("<${headers.CamelApixRegistration}>; rel=\"describedby\"")
                    .to("direct:list")
                .when(header(SERVICE_NAME).isNotNull())
                    .to("direct:route-service")
                .otherwise()
                    .log("Headers: ${headers}")
                    .to("jetty:http://{{fcrepo.baseUrl}}?authUsername={{fcrepo.authUsername}}" +
                            "&authPassword={{fcrepo.authPassword}}&bridgeEndpoint=true" +
                            "&throwExceptionOnFailure=false&disableStreamCache=true" +
                            "&preserveHostHeader=true")
                    .setHeader("Link")
                        .simple("<${headers.CamelHttpUri}/{{apix.prefix}}list>; rel=\"service\"");

        from("direct:route-service")
            .routeId("ApixServiceRouter")
            .routeDescription("Route API-X service requests")
            .to("direct:check-registered")

            .choice()
                .when(header(SERVICE_ENDPOINT).isNotNull())
                    .to("direct:proxy-to-endpoint")
                .when(header(SERVICE_REGISTRATION).isNotNull())
                    .to("direct:unavailable")
                .otherwise()
                    .to("direct:not-found");

        from("direct:unavailable")
            .routeId("ApixServiceUnavailable")
            .routeDescription("Respond appropriately if the service is unavailable")
            .setHeader("Link").simple("<${headers.CamelApixRegistration}>; rel=\"describedby\"")
            .setHeader(HTTP_RESPONSE_CODE).constant(503)
            .transform().constant("Service Unavailable");

        from("direct:not-found")
            .routeId("ApixServiceNotFound")
            .routeDescription("Respond appropriately if the service has not been registered")
            .setHeader(HTTP_RESPONSE_CODE).constant(404)
            .transform().constant("Not Found");

        from("direct:proxy-to-endpoint")
            .routeId("ApixServiceProxy")
            .routeDescription("Proxy requests to the remote service")
            .setHeader("Link").simple("<${headers.CamelApixRegistration}>; rel=\"describedby\"")
            .setHeader(HTTP_URI).header(SERVICE_ENDPOINT)
            .setHeader(HTTP_PATH).header(FCREPO_IDENTIFIER)
            .to("jetty://localhost");

    }
}
