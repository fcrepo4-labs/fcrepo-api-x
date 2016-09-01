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

import static org.fcrepo.apix.jena.impl.Util.isA;
import static org.fcrepo.apix.jena.impl.Util.objectResourceOf;
import static org.fcrepo.apix.jena.impl.Util.objectResourcesOf;
import static org.fcrepo.apix.jena.impl.Util.parse;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_LDP_SERVICE_INSTANCE_REGISTRY;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE_REGISTRY;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.ServiceInstance;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;

import org.apache.jena.rdf.model.Model;

/**
 * Jena-based service registry implementation,
 *
 * @author apb@jhu.edu
 */
public class JenaServiceRegistry extends WrappingRegistry implements ServiceRegistry {

    @Override
    public void setRegistryDelegate(final Registry delegate) {
        super.setRegistryDelegate(delegate);
    }

    @Override
    public ServiceInstanceRegistry instancesOf(final Service service) {

        final URI registryURI = objectResourceOf(service.uri().toString(), PROP_HAS_SERVICE_INSTANCE_REGISTRY, parse(
                service));

        if (registryURI == null) {
            return null;
        }

        final Model registry = getRegistry(registryURI, service);

        if (!isA(CLASS_LDP_SERVICE_INSTANCE_REGISTRY, registryURI.toString(), registry)) {
            throw new RuntimeException(String.format("<%s> is not a LdpServiceInstanceRegistry", registryURI));
        }

        return new ServiceInstanceRegistry() {

            @Override
            public List<ServiceInstance> instances() {
                return objectResourcesOf(registryURI.toString(), PROP_HAS_SERVICE_INSTANCE, registry).stream()
                        .map(uri -> new LdpServiceInstanceImpl(uri, service))
                        .collect(Collectors.toList());
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

    class ServiceImpl extends WrappingResource implements Service, JenaResource {

        final Model model;

        ServiceImpl(final URI uri) {
            super(get(uri));
            this.model = parse(this);
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

}
