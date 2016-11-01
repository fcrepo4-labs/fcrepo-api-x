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

package org.fcrepo.apix.routing.impl;

import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_EXPOSED_SERVICE_URI;
import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_REPOSITORY_RESOURCE_URI;
import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_REPOSITORY_ROOT_URI;
import static org.fcrepo.apix.routing.Util.append;
import static org.fcrepo.apix.routing.impl.GenericInterceptExecution.HEADER_INVOKE_STATUS;
import static org.fcrepo.apix.routing.impl.GenericInterceptExecution.ROUTE_INTERCEPT_INCOMING;
import static org.fcrepo.apix.routing.impl.GenericInterceptExecution.ROUTE_INTERCEPT_OUTGOING;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.ServiceInstance;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.apix.model.components.ServiceDiscovery;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;
import org.fcrepo.apix.routing.impl.ExposedServiceUriAnalyzer.ServiceExposingBinding;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.io.IOUtils;

/**
 * Stub/placeholder Routing implementation that does nothing.
 *
 * @author apb@jhu.edu
 */
public class RoutingImpl extends RouteBuilder {

    private final Random random = new Random();

    private URI fcrepoBaseURI;

    public static final String EXECUTION_EXPOSE_MODALITY = "direct:execute_expose";

    public static final String EXPOSING_EXTENSION = "CamelApixExposingExtension";

    public static final String SERVICE_INSTANCE_URI = "CamelApixServiceInstanceUri";

    public static final String EXTENSION_NOT_FOUND = "direct:extension_not_found";

    public static final String BINDING = "CamelApixServiceExposureBinding";

    private ExposedServiceUriAnalyzer analyzer;

    private ServiceDiscovery serviceDiscovery;

    private ServiceRegistry serviceRegistry;

    private Routing routing;

    /**
     * Set Fedora's baseURI.
     *
     * @param uri the base URI.
     */
    public void setFcrepoBaseURI(final URI uri) {
        this.fcrepoBaseURI = uri;
    }

    /**
     * Set the discovery component for generating a service doc.
     *
     * @param disc service document generator.
     */
    public void setServiceDiscovery(final ServiceDiscovery disc) {
        this.serviceDiscovery = disc;
    }

    /**
     * Set the service registry.
     *
     * @param registry The registry.
     */
    public void setServiceRegistry(final ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }

    /**
     * Set the URI analyzer.
     *
     * @param analyzer Analyzer impl.
     */
    public void setExposedServiceURIAnalyzer(final ExposedServiceUriAnalyzer analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Set the routing.
     *
     * @param routing routing.
     */
    public void setRouting(final Routing routing) {
        this.routing = routing;
    }

    @Override
    public void configure() throws Exception {

        // It would be nice to use the rest DSL to do the service doc, if that is at all possible

        from("jetty:http://{{apix.listen.host}}:{{apix.port}}/{{apix.discoveryPath}}?matchOnUriPrefix=true")
                .process(WRITE_SERVICE_DOC);

        from("jetty:http://{{apix.listen.host}}:{{apix.port}}/{{apix.exposePath}}?matchOnUriPrefix=true")
                .routeId("endpoint-expose").routeDescription("Endpoint for exposed service mediation")
                .process(ANALYZE_URI)
                .choice()
                .when(header(EXPOSING_EXTENSION).isNull()).to(EXTENSION_NOT_FOUND)
                .otherwise().to(EXECUTION_EXPOSE_MODALITY);

        from("jetty:http://{{apix.listen.host}}:{{apix.port}}/{{apix.interceptPath}}?matchOnUriPrefix=true")
                .routeId("endpoint-intercept").routeDescription("Endpoint for intercept/proxy to Fedora")
                .to(ROUTE_INTERCEPT_INCOMING)
                .choice().when(e -> !e.getIn().getHeaders().containsKey(
                        HEADER_INVOKE_STATUS) || e.getIn().getHeader(
                                HEADER_INVOKE_STATUS, Integer.class) < 300)
                .to("jetty:" + fcrepoBaseURI +
                        "?bridgeEndpoint=true" +
                        "&throwExceptionOnFailure=false" +
                        "&disableStreamCache=true" +
                        "&preserveHostHeader=true")
                .process(ADD_SERVICE_HEADER)
                .to(ROUTE_INTERCEPT_OUTGOING);

        from(EXTENSION_NOT_FOUND).id("not-found-extension").routeDescription("Extension not found")
                .process(e -> e.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, 404));

        from(EXECUTION_EXPOSE_MODALITY)
                .routeId("apix-proxy-service-endpoint")
                .routeDescription("Proxies an exposed service to a service instance")
                .process(SELECT_SERVICE_INSTANCE)
                .setHeader(Exchange.HTTP_PATH).simple("${in.header." + BINDING + ".additionalPath}")
                .recipientList(simple(
                        "jetty://${in.header." + SERVICE_INSTANCE_URI + "}" +
                                "?bridgeEndpoint=true" +
                                "&preserveHostHeader=true" + "" +
                                "&throwExceptionOnFailure=false"));

    }

    final Processor ANALYZE_URI = (ex -> {
        final ServiceExposingBinding binding = analyzer.match(
                URI.create(ex.getIn().getHeader(Exchange.HTTP_URL, String.class)));

        if (binding == null) {
            return;
        }

        ex.getIn().setHeader(BINDING, binding);
        ex.getIn().setHeader(EXPOSING_EXTENSION, binding.extension);
        ex.getIn().setHeader(HTTP_HEADER_EXPOSED_SERVICE_URI, binding.getExposedURI());

        // resource URI is only conveyed for resource-scope services
        if (Scope.RESOURCE.equals(binding.extension.exposed().scope())) {
            ex.getIn().setHeader(HTTP_HEADER_REPOSITORY_RESOURCE_URI, binding.repositoryResourceURI);
        } else {
            ex.getIn().setHeader(HTTP_HEADER_REPOSITORY_ROOT_URI, fcrepoBaseURI);
        }
    });

    final Processor WRITE_SERVICE_DOC = (ex -> {
        final String accept = ex.getIn().getHeader("Accept", "text/turtle", String.class).split("\\s*,\\s*")[0];
        final URI resource = append(fcrepoBaseURI, ex.getIn().getHeader(Exchange.HTTP_PATH));

        try (WebResource serviceDoc = serviceDiscovery.getServiceDocumentFor(resource, accept)) {
            ex.getOut().setBody(IOUtils.toByteArray(serviceDoc.representation()));
            ex.getOut().setHeader(Exchange.CONTENT_TYPE, serviceDoc.contentType());
        }
    });

    final Processor SELECT_SERVICE_INSTANCE = (ex -> {
        final Extension extension = ex.getIn().getHeader(EXPOSING_EXTENSION, Extension.class);

        final Set<URI> services = new HashSet<>(extension.exposed().consumed());
        services.add(extension.exposed().exposedService());

        final URI consumedServiceURI = exactlyOne(services,
                "Exposed services must have exactly one consumed service in extension: " +
                        extension.uri());

        final ServiceInstanceRegistry instanceRegistry = serviceRegistry.instancesOf(serviceRegistry.getService(
                consumedServiceURI));

        if (instanceRegistry == null) {
            throw new ResourceNotFoundException("No instance registry for service " + consumedServiceURI);
        }

        final ServiceInstance instance = oneOf(instanceRegistry.instances(),
                "There must be at least one service instance for " + consumedServiceURI);

        ex.getIn().setHeader(SERVICE_INSTANCE_URI, oneOf(
                instance.endpoints(),
                "There must be at least one endpoint for instances of " + consumedServiceURI));

    });

    @SuppressWarnings("unchecked")
    final Processor ADD_SERVICE_HEADER = (ex -> {

        // Get all link headers, accounting for the fact that there may be zero, one, or multiple
        // (in the message, that means null, string, or list)
        final Set<String> rawLinkHeaders = new HashSet<>();

        final Object linkHeader = ex.getIn().getHeader("Link");

        if (linkHeader instanceof Collection) {
            rawLinkHeaders.addAll((Collection<String>) linkHeader);
        } else if (linkHeader instanceof String) {
            rawLinkHeaders.add((String) linkHeader);
        }

        rawLinkHeaders.add(String.format("<%s>; rel=\"service\"", routing.serviceDocFor(
                ex.getIn().getHeader(Exchange.HTTP_PATH, String.class))));

        ex.getIn().setHeader("Link", rawLinkHeaders);
    });

    private static <T> T exactlyOne(final Collection<T> of, final String errMsg) {
        if (of.size() != 1) {
            throw new ResourceNotFoundException(errMsg);
        }

        return of.iterator().next();
    }

    private <T> T oneOf(final List<T> of, final String errMsg) {
        if (of.isEmpty()) {
            throw new ResourceNotFoundException(errMsg);
        }

        return of.get(random.nextInt(of.size()));
    }
}
