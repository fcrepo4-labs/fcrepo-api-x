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

import static org.apache.camel.builder.PredicateBuilder.and;
import static org.fcrepo.apix.routing.Util.append;
import static org.fcrepo.apix.routing.Util.interceptingServiceInstance;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;
import org.fcrepo.apix.model.components.Updateable;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class GenericInterceptExecution extends RouteBuilder implements Updateable {

    public static final String ROUTE_INTERCEPT_INCOMING = "direct:intercept_incoming";

    public static final String ROUTE_INTERCEPT_OUTGOING = "direct:intercept_outgoing";

    public static final String ROUTE_PERFORM_INCOMING = "direct:perform_incoming";

    public static final String ROUTE_PERFORM_OUTGOING = "direct:perform_outgoing";

    public static final String ROUTE_INVOKE_SERVICE = "direct:intercept_invoke";

    public static final String HEADER_INVOKE_STATUS = "CamelApixInvokeStatusCode";

    public static final String HEADER_SERVICE_ENDPOINTS = "CamelApixServiceEndpoints";

    public static final String HEADER_SERVICE_ENDPOINTS_OUTGOING = "CamelApixServiceEndpointsOutgoing";

    public static final String HTTP_HEADER_MODALITY = "Apix-Modality";

    public static final String MODALITY_INTERCEPT_INCOMING = "intercept; incoming";

    public static final String MODALITY_INTERCEPT_OUTGOING = "intercept; outgoing";

    private static final Logger LOG = LoggerFactory.getLogger(GenericInterceptExecution.class);

    private ExtensionBinding binding;

    private ExtensionRegistry extensionRegistry;

    private ServiceRegistry serviceRegistry;

    private URI proxyURI;

    /**
     * Set the extension binding.
     *
     * @param binding Extension binding.
     */
    public void setExtensionBinding(final ExtensionBinding binding) {
        this.binding = binding;
    }

    /**
     * Set the extension registry.
     *
     * @param registry The extension registry.
     */
    public void setExtensionRegistry(final ExtensionRegistry registry) {
        this.extensionRegistry = registry;
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
     * Set Fedora's baseURI.
     *
     * @param uri the base URI.
     */
    public void setProxyURI(final URI uri) {
        this.proxyURI = uri;
    }

    private final Collection<Extension> extensions = new ConcurrentHashSet<>();

    @Override
    public void update() {
        final List<Extension> found = extensionRegistry.list().stream()
                .map(extensionRegistry::getExtension)
                .filter(Extension::isIntercepting)
                .collect(Collectors.toList());

        extensions.addAll(found);
        extensions.removeIf(x -> !found.contains(x));
    }

    @Override
    public void update(final URI inResponseTo) {
        // TODO: optimize later
        update();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure() throws Exception {

        from(ROUTE_INTERCEPT_INCOMING).id("intercept-incoming").process(GET_ENDPOINTS)
                .setHeader(HTTP_HEADER_MODALITY).constant(MODALITY_INTERCEPT_INCOMING)
                .setHeader(Exchange.HTTP_RESPONSE_CODE).constant(200)
                .to(ROUTE_PERFORM_INCOMING);

        from(ROUTE_PERFORM_INCOMING)
                .choice().when(simple("${in.headers.CamelApixServiceEndpoints.size} > 0"))
                .to(ROUTE_INVOKE_SERVICE)
                .choice().when(
                        and(
                                simple("${in.header.CamelhttpResponseCode} range '200..299'"),
                                simple("${in.header.Apix-Modality} not contains 'terminal'")))
                .to(ROUTE_PERFORM_INCOMING)
                .end();

        from(ROUTE_INTERCEPT_OUTGOING).id("intercept-outgoing")
                .setHeader(HTTP_HEADER_MODALITY).constant(MODALITY_INTERCEPT_OUTGOING)
                .setHeader(HEADER_SERVICE_ENDPOINTS).header(HEADER_SERVICE_ENDPOINTS_OUTGOING)
                .setHeader(Exchange.HTTP_METHOD).constant("POST")
                .to(ROUTE_PERFORM_OUTGOING);

        from(ROUTE_PERFORM_OUTGOING)
                .choice().when(simple("${in.headers.CamelApixServiceEndpoints.size} > 0"))
                .to(ROUTE_INVOKE_SERVICE)
                .choice().when(simple("${in.header.CamelhttpResponseCode} range '200..299'"))
                .to(ROUTE_PERFORM_OUTGOING)
                .end();

        from(ROUTE_INVOKE_SERVICE).id("intercept-invoke")
                .process(e -> e.getIn().setHeader(
                        Exchange.HTTP_URI,
                        e.getIn().getHeader(HEADER_SERVICE_ENDPOINTS, Queue.class).remove()))
                .to("http://localhost?throwExceptionOnFailure=false" +
                        "&disableStreamCache=true" +
                        "&preserveHostHeader=true");
    }

    final Processor GET_ENDPOINTS = (ex -> {
        final URI fedoraResource = append(proxyURI, ex.getIn().getHeader(Exchange.HTTP_PATH));

        if (extensions.size() > 0) {
            final List<URI> exts =
                    binding.getExtensionsFor(fedoraResource, extensions)
                            .stream()
                            .map(e -> interceptingServiceInstance(e, serviceRegistry))
                            .collect(Collectors.toList());

            ex.getIn().setHeader(HEADER_SERVICE_ENDPOINTS, new LinkedList<>(exts));
            ex.getIn().setHeader(HEADER_SERVICE_ENDPOINTS_OUTGOING, new LinkedList<>(exts));

        }
    });

    // Handle the response from an extension invocation for
    final AggregationStrategy INCOMING_HANDLE_RESPONSE = ((req, resp) -> {

        final Map<String, Object> respHeaders = resp.getIn().getHeaders().entrySet().stream()
                .filter(e -> !e.getKey().toString().startsWith("Camel"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        if (resp.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) > 299) {
            // Error/redirect path. An extension has given a non-200 code. This entire message will be
            // returned directly to the client as a response.
            req.getIn().getHeader(HEADER_SERVICE_ENDPOINTS, Queue.class).clear();
            req.getOut().setHeaders(respHeaders);
            req.getOut().setHeader(Exchange.HTTP_RESPONSE_CODE, resp.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
            req.getOut().setBody(resp.getIn().getBody());
        } else {
            // Success path. Headers will be merged with the current request,
            // and forwarded to the next destination (Fedora, or extension)
            req.getOut().setHeaders(req.getIn().getHeaders());
            req.getOut().getHeaders().putAll(respHeaders);

            try (final PeekInputStream serviceResponse = new PeekInputStream(resp.getIn().getBody(
                    InputStream.class))) {

                if (serviceResponse.hasContent()) {
                    req.getOut().setBody(serviceResponse);
                }
            } catch (final IOException e) {
                throw new RuntimeException("Error retrieving service response", e);
            }
        }

        req.getOut().setHeader(HEADER_INVOKE_STATUS, resp.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));

        return req;
    });

    // Handle the response from an extension invocation for
    final AggregationStrategy OUTGOING_HANDLE_RESPONSE = ((req, resp) -> {

        final Map<String, Object> respHeaders = resp.getIn().getHeaders().entrySet().stream()
                .filter(e -> !e.getKey().toString().startsWith("Camel"))
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        req.getOut().setHeaders(req.getIn().getHeaders());
        req.getOut().setBody(req.getIn().getBody());

        if (resp.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class) < 299) {
            req.getOut().getHeaders().putAll(respHeaders);

            try (final PeekInputStream responseBody = new PeekInputStream(resp.getIn().getBody(InputStream.class))) {

                if (responseBody.hasContent()) {
                    req.getOut().setBody(responseBody);
                }
            } catch (final IOException e) {
                throw new RuntimeException("Error retrieving response body", e);
            }
        } else {
            // I think we can only log this, we can't abort the request at this point.
            LOG.warn("Outgoing intercept:  Response from {} returned {}", resp.getIn().getHeader(Exchange.HTTP_URI),
                    resp.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        }

        return req;
    });

    private static class PeekInputStream extends PushbackInputStream {

        public PeekInputStream(final InputStream in) {
            super(in);
        }

        public boolean hasContent() {
            try {
                final int next = read();
                if (next > -1) {
                    unread(next);
                    return true;
                }

                return false;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
