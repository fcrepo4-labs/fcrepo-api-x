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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyService;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.client.FcrepoLink;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
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
 *
 * @author apb@jhu.edu
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class RuntimeExtensionBinding implements ExtensionBinding {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeExtensionBinding.class);

    private CloseableHttpClient httpClient;

    private ExtensionRegistry extensionRegistry;

    private OntologyService ontologySvc;

    private Registry registry;

    /**
     * Set the http client
     *
     * @param client client;
     */
    public void setHttpClient(final CloseableHttpClient client) {
        this.httpClient = client;
    }

    /**
     * Set the underlying registry containing extensions that may be bound.
     *
     * @param exr Extension registry instance.
     */
    @Reference
    public void setExtensionRegistry(final ExtensionRegistry exr) {
        extensionRegistry = exr;
    }

    /**
     * Set the underlying ontology service for performing reasoning for binding.
     *
     * @param os Ontology service instance.
     */
    @Reference
    public void setOntologyService(final OntologyService os) {
        ontologySvc = os;
    }

    /**
     * Set the underlying delegate registry for retrieving arbitrary web resources from the repository.
     * <p>
     * This registry is consulted in support of {@link #getExtensionsFor(URI)}
     * </p>
     *
     * @param registry Registry impl.
     */
    @Reference(target = "(org.fcrepo.apix.registry.role=default)")
    public void setDelegateRegistry(final Registry registry) {
        this.registry = registry;
    }

    /**
     * Simple/naive binding algorithm, there may be opportunities for optimization when the time is right
     * <ol>
     * <li>Determine the set of known extensions</li>
     * <li>for each extension, get its ontology closure</li>
     * <li>for each ontology closure, infer classes of the instance</li>
     * <li>For each extension, see if its binding class is in that list of inferred classes.</li>
     * <li>Return all extensions that match</li>
     * </ol>
     */
    @Override
    public Collection<Extension> getExtensionsFor(final WebResource resource) {

        return getExtensionsFor(resource, extensionRegistry.getExtensions());
    }

    @Override
    public Collection<Extension> getExtensionsFor(final WebResource resource,
            final Collection<Extension> extensions) {

        try (final InputStream resourceContent = resource.representation()) {

            final byte[] content = IOUtils.toByteArray(resourceContent);

            final Set<URI> rdfTypes = extensions.stream()
                    .flatMap(RuntimeExtensionBinding::getExtensionResource)
                    .peek(r -> LOG.debug("Examinining the ontology closure of extension {}", r.uri()))
                    .map(ontologySvc::parseOntology)
                    .flatMap(o -> ontologySvc.inferClasses(resource.uri(), cached(resource, content), o).stream())
                    .peek(rdfType -> LOG.debug("Instance {} is of class {}", resource.uri(), rdfType))
                    .collect(Collectors.toSet());

            return extensions.stream()
                    .peek(e -> LOG.debug("Extension {} binds to instances of {}", e.uri(), e.bindingClass()))
                    .filter(e -> rdfTypes.contains(e.bindingClass()))
                    .peek(e -> LOG.debug("Extension {} bound to instance {} via {}", e.uri(), resource.uri(), e
                            .bindingClass()))
                    .collect(Collectors.toList());
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<WebResource> getExtensionResource(final Extension e) {

        try {
            return Stream.of(e.getResource());
        } catch (final ResourceNotFoundException x) {
            return Stream.empty();
        }
    }

    private WebResource cached(final WebResource initial, final byte[] content) {
        return WebResource.of(new ByteArrayInputStream(content), initial.contentType(), initial.uri(), initial
                .name());
    }

    /** Just does a dumb dereference and lookup */
    @Override
    public Collection<Extension> getExtensionsFor(final URI resourceURI, final Collection<Extension> from) {

        if (from.isEmpty()) {
            return Collections.emptyList();
        }

        // Use object contents for reasoning, or if binary the binary's description
        try (final CloseableHttpResponse response = httpClient.execute(new HttpHead(resourceURI))) {

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException(String.format("Got unexpected status code %s in HEAD to <%s>",
                        response.getStatusLine().getStatusCode(),
                        resourceURI));
            }

            final List<FcrepoLink> describedByLinks =
                    Arrays.asList(response.getHeaders("Link")).stream().map(Header::getValue)
                            .map(FcrepoLink::new)
                            .filter(l -> "describedby".equals(l.getRel()))
                            .collect(Collectors.toList());

            if (!describedByLinks.isEmpty()) {
                if (describedByLinks.size() > 1) {
                    throw new RuntimeException(
                            String.format("Ambiguous; more than one describes header for <%s>", resourceURI));
                }

                LOG.debug("Using <{}> for inference about binary <{}>", describedByLinks.get(0).getUri(),
                        resourceURI);

                try (WebResource resource = registry.get(describedByLinks.get(0).getUri())) {
                    return getExtensionsFor(WebResource.of(
                            resource.representation(),
                            resource.contentType(),
                            resourceURI, null), from);
                }

            } else {
                try (WebResource resource = registry.get(resourceURI)) {
                    return getExtensionsFor(resource, from);
                }
            }

        } catch (final Exception e) {
            throw new RuntimeException("Could not get triples for reasoning over " + resourceURI, e);
        }
    }

    /** Just does a dumb dereference and lookup */
    @Override
    public Collection<Extension> getExtensionsFor(final URI resourceURI) {
        return getExtensionsFor(resourceURI, extensionRegistry.getExtensions());
    }
}
