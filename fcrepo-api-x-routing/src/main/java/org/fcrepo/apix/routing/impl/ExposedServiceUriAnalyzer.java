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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.components.ExtensionRegistry;
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
                .filter(e -> e.exposed().scope().equals(Scope.RESOURCE))
                .collect(Collectors.toMap((e -> e.exposed().exposedAt().getPath()), e -> e));

        endpoints.putAll(exts);

        endpoints.keySet().stream()
                .filter(k -> !exts.containsKey(k))
                .forEach(endpoints::remove);
    }

    @Override
    public void update(final URI inResponseTo) {
        // TODO: optimize later
        update();
    }

    /**
     * Match a request URI to a concrete extension binding.
     *
     * @param requestURI
     * @return
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

            final String resourcePath = rawPath.substring(0, rawPath.indexOf(matches.get(0)) - 1);
            final URI exposedServiceURI = append(exposeBaseURI, resourcePath, matches.get(0));

            return new ServiceExposingBinding(
                    endpoints.get(matches.get(0)),
                    append(fcrepoBaseURI, resourcePath),
                    exposedServiceURI,
                    requestURI.toString().replace(exposedServiceURI.toString(), ""));

        } else {
            // TODO: Perhaps this should be an exception?
            return null;
        }
    }

    public class ServiceExposingBinding {

        public Extension extension;

        public URI repositoryResourceURI;

        public URI baseURI;

        public String additionalPath;

        /** Create a concrete binding */
        public ServiceExposingBinding(final Extension extension, final URI resource, final URI exposed,
                final String additionalPath) {
            this.extension = extension;
            this.repositoryResourceURI = resource;
            this.baseURI = exposed;
            this.additionalPath = additionalPath;
        }

        /** Get the exposed URI of this binding */
        public URI getExposedURI() {
            return baseURI;
        }

        /** Get additional path segments */
        public String getAdditionalPath() {
            return additionalPath;
        }
    }
}
