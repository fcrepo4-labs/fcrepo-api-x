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

import static org.fcrepo.apix.jena.Util.objectResourceOf;
import static org.fcrepo.apix.jena.Util.objectResourcesOf;
import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.jena.Util.subjectOf;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CONTAINS_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE_REGISTRY;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_INSTANCE_OF;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.apix.jena.JenaResource;
import org.fcrepo.apix.jena.Util;
import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.ServiceInstance;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;
import org.fcrepo.apix.model.components.Updateable;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jena-based service registry implementation,
 *
 * @author apb@jhu.edu
 */
public class JenaServiceRegistry extends WrappingRegistry implements ServiceRegistry, Updateable {

    private URI registryContainer;

    private FcrepoClient client;

    private static final Logger LOG = LoggerFactory.getLogger(JenaServiceRegistry.class);

    /**
     * Set the Fedora client.
     *
     * @param client the client
     */
    public void setFcrepoClient(final FcrepoClient client) {
        this.client = client;
    }

    /**
     * Set the LDP container corresponding to this service registry.
     *
     * @param container the container;
     */
    public void setRegistryContainer(final URI container) {
        registryContainer = container;
    }

    @Override
    public void setRegistryDelegate(final Registry delegate) {
        super.setRegistryDelegate(delegate);
    }

    // Index mapping canonical URI to resource URI
    private final ConcurrentHashMap<URI, URI> canonicalUriMap = new ConcurrentHashMap<>();

    @Override
    public void update() {

        // For all resources in the registry, get the URIs of everything that calls itself a Service, or is explicitly
        // registered as a service

        final Set<URI> serviceURIs = Stream.concat(super.list().stream()
                .map(this::get)
                .map(Util::parse)
                .flatMap(m -> m.listSubjectsWithProperty(
                        m.getProperty(RDF_TYPE),
                        m.getResource(CLASS_SERVICE))
                        .mapWith(Resource::getURI)
                        .toSet().stream()
                        .map(URI::create)),
                objectResourcesOf(null, PROP_CONTAINS_SERVICE, parse(this.get(
                        registryContainer))).stream())
                .collect(
                        Collectors.toSet());

        // Map canonical URI to service resource. If multiple service resources
        // indicate the same canonical URI, pick one arbitrarily.
        final Map<URI, URI> canonical = serviceURIs.stream()
                .flatMap(this::attemptLookupService)
                .collect(Collectors.toMap(s -> s.canonicalURI(), s -> s.uri(), (a, b) -> a));

        canonicalUriMap.putAll(canonical);

        canonicalUriMap.keySet().removeIf(k -> !canonical.containsKey(k));
    }

    @Override
    public void update(final URI uri) {
        if (hasInDomain(uri)) {
            // TODO: This can be optimized more
            update();
        }
    }

    @Override
    public void register(final URI uri) {
        try {
            LOG.debug("Registering service {} ", uri);
            this.client.patch(registryContainer).body(patchAddService(uri)).perform().close();
        } catch (final Exception e) {
            throw new RuntimeException(String.format("Could not add <%s> to service registry <%s>", uri,
                    registryContainer), e);
        }

        update(uri);
    }

    private InputStream patchAddService(final URI service) {
        try {
            return IOUtils.toInputStream(String.format(
                    "INSERT {<> <%s> <%s> .} WHERE {}", PROP_CONTAINS_SERVICE, service), "utf8");
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServiceInstanceRegistry createInstanceRegistry(final Service service) {
        System.out.println("POST: Creating service instance registry");
        try {
            final URI uri = client.post(service.uri()).body(this.getClass().getResourceAsStream(
                    "objects/service-instance-registry.ttl")).perform().getLocation();

            client.patch(uri).body(
                    new ByteArrayInputStream(
                            String.format(
                                    "INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                    PROP_IS_SERVICE_INSTANCE_OF, service.uri(), CLASS_SERVICE_INSTANCE).getBytes()))
                    .perform();
            return instancesOf(getService(service.uri()));
        } catch (final FcrepoOperationFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ServiceInstanceRegistry instancesOf(final Service service) {

        final URI registryURI = objectResourceOf(service.uri().toString(), PROP_HAS_SERVICE_INSTANCE_REGISTRY, parse(
                service));

        if (registryURI == null) {
            throw new ResourceNotFoundException("No service instance registry found for service " + service.uri());
        }

        // TODO: Allow this to be pluggable to different service instance registry implementations

        final Model registry = getRegistry(registryURI, service);

        return new ServiceInstanceRegistry() {

            @Override
            public List<ServiceInstance> instances() {
                return objectResourcesOf(registryURI.toString(), PROP_HAS_SERVICE_INSTANCE, registry).stream()
                        .map(uri -> new LdpServiceInstanceImpl(uri, service))
                        .collect(Collectors.toList());
            }

            @Override
            public URI addEndpoint(final URI endpoint) {
                System.out.println("PATCH: Adding endpoint");
                try {
                    client.patch(registryURI).body(
                            new ByteArrayInputStream(
                                    String.format(
                                            "INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                            PROP_HAS_ENDPOINT, endpoint, CLASS_SERVICE_INSTANCE).getBytes()))
                            .perform();
                    return registryURI;
                } catch (final FcrepoOperationFailedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    class LdpServiceInstanceImpl implements ServiceInstance {

        final Model model;

        final String uri;

        final Service service;

        public LdpServiceInstanceImpl(final URI uri, final Service service) {
            model = parse(get(uri));
            this.uri = uri.toString();
            this.service = service;
        }

        @Override
        public List<URI> endpoints() {
            return objectResourcesOf(uri, PROP_HAS_ENDPOINT, model);
        }

        @Override
        public Service instanceOf() {
            return service;
        }
    }

    @Override
    public Service getService(final URI uri) {
        return new ServiceImpl(uri);
    }

    @Override
    public Collection<URI> list() {
        return new HashSet<>(canonicalUriMap.values());
    }

    @Override
    public boolean contains(final URI uri) {
        return canonicalUriMap.containsKey(uri) || canonicalUriMap.containsValue(uri);
    }

    class ServiceImpl extends WrappingResource implements Service, JenaResource {

        final Model model;

        final URI uri;

        ServiceImpl(final URI uri) {
            super(get(resourceURI(uri)));
            this.model = parse(this);

            // Sanity check - verify that the uri is a service. If not, and if there is exactly one service, use that.
            if (!model.contains(model.getResource(uri.toString()), model.getProperty(RDF_TYPE), model.getResource(
                    CLASS_SERVICE))) {
                try {
                    this.uri = subjectOf(RDF_TYPE, CLASS_SERVICE, model);
                } catch (final ResourceNotFoundException e) {
                    throw new RuntimeException(
                            String.format("Error performing sanity check on service impl <%s>", uri), e);
                }
            } else {
                this.uri = uri;
            }
        }

        @Override
        public URI canonicalURI() {

            final List<URI> canonical = objectResourcesOf(uri().toString(), PROP_CANONICAL, model);

            return canonical.isEmpty() ? uri() : canonical.get(0);
        }

        @Override
        public Model model() {
            return model;
        }

        @Override
        public URI uri() {
            return uri;
        }

    }

    private Model getRegistry(final URI registryURI, final Service svc) {
        if (hasSameRepresentation(registryURI, svc.uri())) {
            return parse(svc);
        } else {
            return parse(get(registryURI));
        }
    }

    private static boolean hasSameRepresentation(final URI a, final URI b) {
        try {
            return new URI(a.getScheme(), a.getAuthority(), a.getPath(), null, null).equals(new URI(b.getScheme(), b
                    .getAuthority(), b.getPath(), null, null));
        } catch (final URISyntaxException e) {
            throw new RuntimeException("Shoud never happen", e);
        }
    }

    // Try looking in canonical map first
    private URI resourceURI(final URI uri) {
        return Optional.ofNullable(canonicalUriMap.get(uri)).orElse(uri);
    }

    @Override
    public boolean hasInDomain(final URI uri) {
        return delegate.hasInDomain(uri) || canonicalUriMap.values().contains(uri);
    }

    private Stream<Service> attemptLookupService(final URI uri) {
        try {
            return Stream.of(this.getService(uri));
        } catch (final Exception e) {
            LOG.warn("Could not resolve service for " + uri, e);
            return Stream.of();
        }
    }

}
