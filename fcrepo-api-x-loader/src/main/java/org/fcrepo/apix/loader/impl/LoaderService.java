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
import static org.fcrepo.apix.jena.Util.objectResourceOf;
import static org.fcrepo.apix.jena.Util.objectResourcesOf;
import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.jena.Util.subjectOf;
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
import java.util.Collection;
import java.util.HashSet;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service which loads extensions and services based on the contents of the given resource.
 *
 * @author apb@jhu.edu
 */
public class LoaderService {

    private ServiceRegistry serviceRegistry;

    private ExtensionRegistry extensionRegistry;

    private Registry generalRegistry;

    private static final Logger LOG = LoggerFactory.getLogger(LoaderService.class);

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
     * @param resource resource to load
     * @return URI pointing to service instance resource.
     */
    public URI load(final WebResource resource) {

        final byte[] body = toByteArray(resource);

        final Model model = parse(WebResource.of(new ByteArrayInputStream(body), resource.contentType(), resource
                .uri(), null), resource.uri().toString());

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
                            findMatchingExtension(model), extensionName(model)),
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

        final Model persistedModel = parse(generalRegistry.get(uri));

        final Set<URI> defined = new HashSet<>(subjectsOf(RDF_TYPE, CLASS_SERVICE, persistedModel));

        final Set<URI> definedWithExplicitCanonical = new HashSet<>(subjectsOf(PROP_CANONICAL, null, persistedModel));

        final Set<URI> referenced = allReferencedServices(persistedModel);

        final Set<URI> resolvableservices = new HashSet<>();

        final Set<URI> canonicalServices = new HashSet<>();

        LOG.debug("Defined services are: {}", defined);
        LOG.debug("Defined with canonical: {}", definedWithExplicitCanonical);
        LOG.debug("Referenced: {}", referenced);

        // All referenced services that are not otherwise given a canonical URI are canonical.
        canonicalServices.addAll(
                referenced.stream().filter(u -> !definedWithExplicitCanonical.contains(u))
                        .collect(Collectors.toSet()));

        // All explicitly canonical URIs are canonical
        canonicalServices.addAll(
                definedWithExplicitCanonical.stream()
                        .map(URI::toString)
                        .map(s -> objectResourceOf(s, PROP_CANONICAL, persistedModel))
                        .collect(Collectors.toSet()));

        // Defined services without an explicitly canonical URI are canonical
        canonicalServices.addAll(defined.stream()
                .filter(u -> !definedWithExplicitCanonical.contains(u))
                .collect(Collectors.toSet()));

        // Finally, add any canonical services that aren't in our registry.
        for (final URI canonical : canonicalServices) {
            if (serviceRegistry.contains(canonical)) {
                LOG.debug("Service registry contains <{}>, NOT adding", canonical);
                resolvableservices.add(canonical);
            } else {
                LOG.info("Service registry does not contain <{}>, adding!", canonical);
                resolvableservices.add(putService(canonical));
            }
        }

        // Return a set of all resolvable URIs to services mentioned in this document.
        return resolvableservices;

    }

    private URI addInstance(final URI instanceURI, final Service service) {
        ServiceInstanceRegistry instances = null;
        try {
            instances = serviceRegistry.instancesOf(service);
        } catch (final ResourceNotFoundException e) {
            instances = serviceRegistry.createInstanceRegistry(service);
        }

        LOG.info("Adding endpoint <{}> as instance of <{}>", instanceURI, service.canonicalURI());
        return instances.addEndpoint(instanceURI);
    }

    private URI putService(final URI serviceURI) {

        try (InputStream template = getClass().getResourceAsStream("/objects/service.ttl")) {
            final String rdf = IOUtils.toString(template, "utf8")
                    .replace("CANONICAL_SERVICE_URI", serviceURI.toString());

            return serviceRegistry.put(WebResource.of(new ByteArrayInputStream(rdf.getBytes()), "text/turtle", null,
                    toName(serviceURI)));
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

    // Find an extension in the registry that matches (i.e. "is") the given one
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

    // Come up with a plausible name for the extension
    private String extensionName(final Model model) {
        return toName(subjectOf(RDF_TYPE, CLASS_EXTENSION, model));
    }

    // Come up with a plausible text name given a uri
    private String toName(final URI uri) {
        if (uri.getPath() == null || uri.getPath().equals("")) {
            return null;
        }

        if (uri.getFragment() != null && !uri.getFragment().equals("")) {
            return uri.getPath() + "-" + uri.getFragment();
        } else {
            return uri.getPath();
        }
    }

    private byte[] toByteArray(final WebResource resource) {

        try (WebResource r = resource) {
            return IOUtils.toByteArray(resource.representation());
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
