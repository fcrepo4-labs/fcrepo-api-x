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

package org.fcrepo.apix.binding.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyService;
import org.fcrepo.apix.model.components.Registry;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple extension binding based on runtime lookup and reasoning.
 * <p>
 * Polls the extension registry for all known extensions at time of transaction, and performs reasoning at runtime in
 * order to bind a repository resource to the extensions that match it.
 * </p>
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RuntimeExtensionBinding implements ExtensionBinding {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeExtensionBinding.class);

    private ExtensionRegistry extensionRegistry;

    private OntologyService ontologySvc;

    private Registry registry;

    @Reference
    public void setExtensionRegistry(ExtensionRegistry exr) {
        extensionRegistry = exr;
    }

    @Reference
    public void setOntologyService(OntologyService os) {
        ontologySvc = os;
    }

    @Reference(target = "(org.fcrepo.apix.registry.role=default)")
    public void setDelegateRegistry(Registry registry) {
        this.registry = registry;
    }

    // (a) Determine the set of known extensions
    // (b) for each extension, get its union ontology
    // (c) for each union ontology, infer classes of the instance
    // (d) Collect the list of classes
    // (e) For each extension, see if its binding class is in that list. If so, collect it
    // (f) return collected list of extensions.
    @Override
    public Collection<Extension> getExtensionsFor(WebResource resource) {

        final Collection<Extension> extensions = extensionRegistry.getExtensions();
        LOG.debug("(A) Got list of known extensions: {}", extensions);

        final List<Extension> boundExtensions = new ArrayList<>();

        for (final Extension e : extensions) {
            LOG.debug("(B) Getting the ontology closure of extension {}", e.uri());
            final Ontology o = ontologySvc.parseOntology(e.getResource());

            LOG.debug("(C) Inferring classes over resource {} using ontologies from extension {}", resource.uri(), e
                    .uri());
            final Set<URI> inferredClasses = ontologySvc.inferClasses(resource.uri(), resource, o);

            for (final URI inferredClass : inferredClasses) {
                LOG.debug("(D) Found class {}", inferredClass);

                for (final Extension candidate : extensions) {
                    if (candidate.bindingClass().equals(inferredClass)) {
                        boundExtensions.add(candidate);
                        LOG.debug("(E) Class {} matches extension {}", candidate.uri(), candidate.bindingClass());
                    }
                }
            }
        }

        return boundExtensions;

        // This is perhaps a more concise way of performing the above^^, but the logging statements
        // from the above are very useful.
        //
        // final Set<URI> rdfTypes = extensions.stream()
        // .map(Extension::getResource)
        // .map(ontologySvc::parseOntology)
        // .flatMap(o -> ontologySvc.inferClasses(resource.uri(), resource, o).stream())
        // .collect(Collectors.toSet());
        //
        // return extensions.stream().filter(e -> rdfTypes.contains(e.bindingClass())).collect(Collectors.toList());
    }

    /** Just does a dumb dereference and lookup */
    @Override
    public Collection<Extension> getExtensionsFor(URI resourceURI) {
        try (WebResource resource = registry.get(resourceURI)) {
            return getExtensionsFor(resource);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
