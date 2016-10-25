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

import static org.fcrepo.apix.jena.Util.objectLiteralsOf;
import static org.fcrepo.apix.jena.Util.objectResourcesOf;
import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.jena.Util.subjectsOf;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Apix.CLASS_EXTENSION;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_CONSUMES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE_AT;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * Service which loads extensions and services based on the contents of the given resource.
 *
 * @author apb@jhu.edu
 */
public class LoaderService {

    private ServiceRegistry serviceRegistry;

    private ExtensionRegistry extensionRegistry;

    private Registry generalRegistry;

    /**
     * Set the service registry.
     *
     * @param registry The registry
     */
    public void setServiceRegistry(final ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }

    /**
     * Set the extension registry.
     *
     * @param registry the registry.
     */
    public void setExtensionRegistry(final ExtensionRegistry registry) {
        this.extensionRegistry = registry;
    }

    /**
     * Set the general registry.
     *
     * @param registry the registry.
     */
    public void setGeneralRegistry(final Registry registry) {
        this.generalRegistry = registry;
    }

    /**
     * Load an extension and/or services.
     *
     * @return URI pointing to service instance resource.
     */
    public URI load(final WebResource resource) {

        final byte[] body = toByteArray(resource);

        final Model model = parse(WebResource.of(new ByteArrayInputStream(body), resource.contentType()), "");

        final int extensionCount = subjectsOf(RDF_TYPE, CLASS_EXTENSION, model).size();

        final int definedServiceCount = allDefinedServices(model).size();

        if (extensionCount > 1) {
            throw new RuntimeException("Too many extension URIs in " + resource.uri());
        }

        URI depositedResource = null;

        // If the document defines an extension, load it as such. Otherwise, add to the
        // service registry;
        if (extensionCount == 1) {
            depositedResource = extensionRegistry.put(
                    WebResource.of(
                            new ByteArrayInputStream(body),
                            resource.contentType(),
                            findMatchingExtension(model), null),
                    useBinary(model));
        } else if (definedServiceCount > 0) {
            depositedResource = serviceRegistry.put(
                    WebResource.of(new ByteArrayInputStream(body), resource.contentType()));
        } else {
            throw new RuntimeException(String.format(
                    "Cannot load resource.  Does not describe exactly one extension (%d), " +
                            "or define any services:\n===\n%s\n===",
                    extensionCount, new String(body)));
        }

        // Now, see if we need to register a service instance.
        final Set<URI> allServices = loadServices(depositedResource);

        // If there is unambiguously one service, then register our resource URI as an instance of it, and return a
        // URI to the instance, otherwise, just return the extension or service URI as appropriate.
        if (allServices.size() == 1) {
            final Service service = serviceRegistry.getService(allServices.iterator().next());
            addInstance(resource.uri(), service);
        } else {
            return depositedResource;
        }

        return depositedResource;
    }

    // Load an extension resource, then load any services defined or referenced by it if appropriate.
    private Set<URI> loadServices(final URI uri) {

        System.out.println("Loading services from " + uri);
        final Model persistedModel = parse(generalRegistry.get(uri));

        final Collection<URI> definedServices = subjectsOf(RDF_TYPE, CLASS_SERVICE, persistedModel);

        final Set<URI> referencedServices = allReferencedServices(persistedModel);

        System.out.println("Defined services: " + definedServices);
        System.out.println("Referenced services: " + referencedServices);

        final Map<URI, URI> canonicalToLocal =
                persistedModel.listStatements(null, persistedModel.getProperty(PROP_CANONICAL), (Resource) null)
                        .mapWith(s -> new SimpleEntry<String, String>(
                                s.getSubject().getURI(),
                                s.getObject().asResource().getURI()))
                        .toSet().stream().collect(Collectors.toMap(e -> URI.create(e.getValue()), e -> URI.create(e
                                .getKey())));

        final Map<URI, URI> localToCanonical = canonicalToLocal.entrySet().stream().collect(Collectors.toMap(e -> e
                .getValue(), e -> e.getKey()));

        // Add a reference to each service defined in this resource to the service registry, if the service is not
        // already registered
        definedServices.stream()
                .filter(s -> !serviceRegistry.contains(localToCanonical.getOrDefault(s, s)))
                .forEach(serviceRegistry::register);

        System.out.println(canonicalToLocal);

        // For any other services referenced but not defined, put a skeletal service description in the registry if
        // one doesn't exist.
        referencedServices.stream()
                .filter(s -> !definedServices.contains(canonicalToLocal.getOrDefault(s, s)))
                .filter(s -> !serviceRegistry.contains(s))
                .forEach(s -> canonicalToLocal.putIfAbsent(s, putService(s)));

        final Set<URI> uris = new HashSet<>();
        uris.addAll(definedServices);
        uris.addAll(referencedServices);

        return uris.stream().map(u -> canonicalToLocal.getOrDefault(u, u)).collect(Collectors.toSet());
    }

    private URI addInstance(final URI instanceURI, final Service service) {
        ServiceInstanceRegistry instances = null;
        try {
            instances = serviceRegistry.instancesOf(service);
        } catch (final ResourceNotFoundException e) {
            instances = serviceRegistry.createInstanceRegistry(service);
        }

        return instances.addEndpoint(instanceURI);
    }

    private URI putService(final URI serviceURI) {

        try (InputStream template = getClass().getResourceAsStream("/objects/service.ttl")) {
            final String rdf = IOUtils.toString(template, "utf8")
                    .replace("CANONICAL_SERVICE_URI", serviceURI.toString());
            return serviceRegistry.put(WebResource.of(new ByteArrayInputStream(rdf.getBytes()), "text/turtle"));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static Set<URI> allReferencedServices(final Model model) {
        final Set<URI> referencedServices = new HashSet<>();
        referencedServices.addAll(objectResourcesOf(null, PROP_CONSUMES_SERVICE, model));
        referencedServices.addAll(objectResourcesOf(null, PROP_EXPOSES_SERVICE, model));

        return referencedServices;
    }

    private static Set<URI> allDefinedServices(final Model model) {
        return new HashSet<>(subjectsOf(RDF_TYPE, CLASS_SERVICE, model));
    }

    private URI findMatchingExtension(final Model model) {
        final Collection<URI> exposesServices = objectResourcesOf(null, PROP_EXPOSES_SERVICE, model);
        final Collection<URI> consumesServices = objectResourcesOf(null, PROP_CONSUMES_SERVICE, model);
        final Collection<String> exposesAt = objectLiteralsOf(null, PROP_EXPOSES_SERVICE_AT, model);
        final boolean isExposing = exposesServices.size() > 0;

        for (final Extension e : extensionRegistry.getExtensions()) {
            if (isExposing && e.isExposing() && exposesAt.contains(e.exposed().exposedAt().toString())) {
                return e.uri();
            } else if (!isExposing && consumesServices.equals(e.intercepted().consumed())) {
                return e.uri();
            }
        }

        return null;
    }

    // Return true if any blank nodes are present
    private boolean useBinary(final Model model) {
        return !model.listSubjects().filterKeep(r -> r.isAnon()).toList().isEmpty();
    }

    private byte[] toByteArray(final WebResource resource) {

        try (WebResource r = resource) {
            return IOUtils.toByteArray(resource.representation());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
