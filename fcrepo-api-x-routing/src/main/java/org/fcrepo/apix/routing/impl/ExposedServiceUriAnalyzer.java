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

import static org.fcrepo.apix.routing.Util.append;
import static org.fcrepo.apix.routing.Util.segment;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.Updateable;

/**
 * Analyzes service URIs.
 * <p>
 * Determines if a given URI corresponds to an exposed service URI implied by an extension.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class ExposedServiceUriAnalyzer implements Updateable {

    private ExtensionRegistry extensions;

    private String exposeBaseURI;

    private String fcrepoBaseURI;

    private final Map<String, Extension> endpoints = new ConcurrentHashMap<>();

    /**
     * Set the extension registry.
     *
     * @param registry The extension registry.
     */
    public void setExtensionRegistry(final ExtensionRegistry registry) {
        this.extensions = registry;
    }

    /**
     * Set the fedora baseURI.
     *
     * @param baseURI Fedora baseURI.
     */
    public void setFcrepoBaseURI(final URI baseURI) {
        this.fcrepoBaseURI = baseURI.toString();
    }

    /**
     * Set the API-X expose base URI.
     *
     * @param baseuri the base URI for service exposure.
     */
    public void setExposeBaseURI(final URI baseuri) {
        this.exposeBaseURI = baseuri.toString();
    }

    @Override
    public void update() {
        final Map<String, Extension> exts = extensions.list().stream()
                .map(extensions::getExtension)
                .filter(Extension::isExposing)
                .filter(e -> e.exposed().scope() != Scope.EXTERNAL)
                .collect(Collectors.toMap((e -> e.exposed().exposedAt().getPath()), e -> e));

        endpoints.putAll(exts);

        endpoints.keySet().removeIf(k -> !exts.containsKey(k));
    }

    @Override
    public void update(final URI inResponseTo) {
        if (extensions.hasInDomain(inResponseTo)) {
            // TODO: This can be optimized more
            update();
        }
    }

    /**
     * Match a request URI to a concrete extension binding.
     *
     * @param requestURI a request URI
     * @return Service exposing extension binding
     */
    public ServiceExposingBinding match(final URI requestURI) {

        if (requestURI.toString().startsWith(exposeBaseURI)) {
            final String rawPath = requestURI.toString().replaceFirst("^" + exposeBaseURI, "");

            final List<String> matches = endpoints.keySet().stream().filter(rawPath::contains).collect(Collectors
                    .toList());

            if (matches.isEmpty()) {
                return null;
            } else if (matches.size() > 1) {
                throw new RuntimeException(String.format("Resource URI %s matches multiple extensions: %s",
                        requestURI, matches));
            }

            final String exposeSegment = matches.get(0);
            final Extension extension = endpoints.get(exposeSegment);

            final String resourcePath = Scope.RESOURCE.equals(extension.exposed().scope())
                    ? rawPath.substring(0, rawPath.indexOf(exposeSegment) - 1)
                    : null;

            final URI exposedServiceURI = Scope.RESOURCE.equals(extension.exposed().scope())
                    ? append(exposeBaseURI, resourcePath, exposeSegment)
                    : URI.create(String.format("%s/%s", segment(exposeBaseURI), exposeSegment));

            return new ServiceExposingBinding(
                    extension,
                    resourcePath != null ? append(fcrepoBaseURI, resourcePath) : null,
                    exposedServiceURI,
                    requestURI.toString().replace(exposedServiceURI.toString(), ""));

        } else {
            throw new ResourceNotFoundException(String.format(
                    "Request URI '%s' does not route to any exposed services", requestURI));
        }
    }

    public class ServiceExposingBinding {

        public Extension extension;

        public URI repositoryResourceURI;

        public URI baseURI;

        public String additionalPath;

        /**
         * Create a concrete binding
         *
         * @param extension The extension
         * @param resource The relevant resource, or null if none
         * @param exposed URI the extension exposes the service at
         * @param additionalPath Additional path segments from request.
         */
        public ServiceExposingBinding(final Extension extension, final URI resource, final URI exposed,
                final String additionalPath) {
            this.extension = extension;
            this.repositoryResourceURI = resource;
            this.baseURI = exposed;
            this.additionalPath = additionalPath;
        }

        /**
         * Get the exposed URI of this binding
         *
         * @return Exposed URI.
         */
        public URI getExposedURI() {
            return baseURI;
        }

        /**
         * Get additional path segments.
         *
         * @return Additional segments
         */
        public String getAdditionalPath() {
            return additionalPath;
        }
    }
}
