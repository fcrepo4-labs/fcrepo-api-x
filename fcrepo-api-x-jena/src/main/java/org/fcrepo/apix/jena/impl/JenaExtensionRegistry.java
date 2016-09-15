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

import static org.fcrepo.apix.jena.Util.objectLiteralOf;
import static org.fcrepo.apix.jena.Util.objectResourceOf;
import static org.fcrepo.apix.jena.Util.objectResourcesOf;
import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_BINDS_TO;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_CONSUMES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE_AT;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Registry;

import org.apache.jena.rdf.model.Model;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Wraps a delegate registry and parses extensions using Jena.
 * <p>
 * Right now, this is mostly a stub; it does nothing more than identify the class an extension is bound to.
 * </p>
 *
 * @author apb@jhu.edu
 */
@Component(service = ExtensionRegistry.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JenaExtensionRegistry extends WrappingRegistry implements ExtensionRegistry {

    /**
     * Underlying registry containing extension resources.
     *
     * @param reg A registry containing extensions.
     */
    @Override
    @Reference // (target = "org.fcrepo.apix.registry.contains=org.fcrepo.apix.model.Extension")
    public void setRegistryDelegate(final Registry reg) {
        super.setRegistryDelegate(reg);
    }

    @Override
    public Extension getExtension(final URI uri) {
        return new JenaExtension(uri);
    }

    @Override
    public Collection<Extension> getExtensions() {
        return delegate.list().stream().map(JenaExtension::new).collect(Collectors.toList());
    }

    private class JenaExtension implements Extension {

        private final URI uri;

        private Model model;

        public JenaExtension(final URI uri) {
            this.uri = uri;
            this.model = getModel();
        }

        @Override
        public URI bindingClass() {
            return objectResourceOf(uri.toString(), PROP_BINDS_TO, model);
        }

        @Override
        public WebResource getResource() {
            return delegate.get(uri);
        }

        @Override
        public URI uri() {
            return uri;
        }

        private Model getModel() {
            if (model == null) {
                try (WebResource wr = getResource()) {
                    model = parse(wr);
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return model;
        }

        @Override
        public boolean isExposing() {
            return model.contains(null, model.getProperty(PROP_EXPOSES_SERVICE));
        }

        @Override
        public boolean isIntercepting() {
            return !isExposing();
        }

        @Override
        public ServiceExposureSpec exposed() {
            return new ServiceExposureSpec() {

                @Override
                public Set<URI> consumed() {
                    return new HashSet<>(objectResourcesOf(uri.toString(), PROP_CONSUMES_SERVICE, model));
                }

                @Override
                public Scope scope() {
                    final String exposedAt = objectLiteralOf(uri.toString(), PROP_EXPOSES_SERVICE_AT, model);

                    if (exposedAt == null) {
                        throw new RuntimeException(String.format(
                                "Can't determine exposure scope; extension <%s> does not expose any services!", uri));
                    }

                    final URI exposeAtURI = URI.create(exposedAt);

                    if (exposeAtURI.isAbsolute() && exposeAtURI.getScheme().startsWith("http")) {
                        return Scope.EXTERNAL;
                    } else if (!exposeAtURI.isAbsolute() && exposeAtURI.getRawPath().startsWith("/")) {
                        return Scope.REPOSITORY;
                    } else {
                        return Scope.RESOURCE;
                    }
                }

                @Override
                public URI exposed() {
                    return objectResourceOf(uri.toString(), PROP_EXPOSES_SERVICE, model);
                }
            };
        }

        @Override
        public Spec intercepted() {
            return new Spec() {

                @Override
                public Set<URI> consumed() {
                    return new HashSet<>(objectResourcesOf(uri.toString(), PROP_CONSUMES_SERVICE, model));
                }
            };
        }
    }
}
