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

import static org.fcrepo.apix.jena.impl.Util.parse;
import static org.fcrepo.apix.model.Ontology.EXTENSION_BINDING_CLASS;

import java.net.URI;
import java.util.Collection;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Registry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Wraps a delegate registry and parses extensions using Jena.
 * <p>
 * Right now, this is mostly a stub; it does nothing more than identify the class an extension is bound to.
 * </p>
 */
@Component(service = ExtensionRegistry.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JenaExtensionRegistry implements ExtensionRegistry {

    Registry delegate;

    @Reference
    public void setRegistryDelegate(Registry reg) {
        delegate = reg;
    }

    @Override
    public WebResource get(URI id) {
        return delegate.get(id);
    }

    @Override
    public URI put(WebResource ontologyResource) {
        return delegate.put(ontologyResource);
    }

    @Override
    public boolean canWrite() {
        return delegate.canWrite();
    }

    @Override
    public Collection<URI> list() {
        return delegate.list();
    }

    @Override
    public Extension getExtension(URI uri) {
        return new JenaExtension(uri);
    }

    @Override
    public Collection<Extension> getExtensions() {
        return delegate.list().stream().map(JenaExtension::new).collect(Collectors.toList());
    }

    private class JenaExtension implements Extension {

        private final URI uri;

        private Model model;

        public JenaExtension(URI uri) {
            this.uri = uri;
        }

        @Override
        public URI bindingClass() {
            return getModel()
                    .listObjectsOfProperty(getModel().getProperty(EXTENSION_BINDING_CLASS))
                    .mapWith(RDFNode::asResource)
                    .mapWith(Resource::getURI)
                    .mapWith(URI::create)
                    .next();
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
    }

    @Override
    public void delete(URI uri) {
        delegate.delete(uri);
    }

    @Override
    public boolean contains(URI id) {
        return delegate.contains(id);
    }
}
