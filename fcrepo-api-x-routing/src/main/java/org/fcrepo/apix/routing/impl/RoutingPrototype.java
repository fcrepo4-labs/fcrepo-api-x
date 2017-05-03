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

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.apix.model.components.RoutingFactory;

import java.net.URI;

import static org.fcrepo.apix.routing.Util.append;

/**
 * A prototype holding common {@code Routing} configuration parameters for concrete implementations.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public abstract class RoutingPrototype implements RoutingFactory, Routing {

    private String discoveryPath;

    private String exposePath;

    private String interceptPath;

    private URI fcrepoBaseURI;

    /**
     * Set the API-X service path.
     * <p>
     * Used for providing access to service document resources, as in
     * <code>http://${apix.host}:${apix.port}/${apix.svcPath}/path/to/object</code>
     * </p>
     *
     * @param path The path segment.
     */
    public void setDiscoveryPath(final String path) {
        this.discoveryPath = path;
    }

    /**
     * Set the intercept path segment
     *
     * @param path intercept path segment
     */
    public void setInterceptPath(final String path) {
        this.interceptPath = path;
    }

    /**
     * Set Fedora's baseURI.
     *
     * @param uri the base URI.
     */
    public void setFcrepoBaseURI(final URI uri) {
        this.fcrepoBaseURI = uri;
    }

    /**
     * Set the API-X Service Exposure path.
     * <p>
     * This establishes a baseURI for exposed services;
     * <code>http://${apix.host}:${apix.port}/${apix.svcPath}/path/to/object/${exposedAt}</code>
     * </p>
     *
     * @param path The path segment.
     */
    public void setExposePath(final String path) {
        this.exposePath = path;
    }

    /**
     * Set the API-X service path.
     * <p>
     * Used for providing access to service document resources, as in
     * <code>http://${apix.host}:${apix.port}/${apix.svcPath}/path/to/object</code>
     * </p>
     *
     * @return The path segment.
     */
    public String getDiscoveryPath() {
        return discoveryPath;
    }

    /**
     * Set the API-X Service Exposure path.
     * <p>
     * This establishes a baseURI for exposed services;
     * <code>http://${apix.host}:${apix.port}/${apix.svcPath}/path/to/object/${exposedAt}</code>
     * </p>
     *
     * @return The path segment.
     */
    public String getExposePath() {
        return exposePath;
    }

    /**
     * Set the intercept path segment
     *
     * @return intercept path segment
     */
    public String getInterceptPath() {
        return interceptPath;
    }

    /**
     * Set Fedora's baseURI.
     *
     * @return the base URI.
     */
    public URI getFcrepoBaseURI() {
        return fcrepoBaseURI;
    }

    @Override
    public URI endpointFor(final Extension.ServiceExposureSpec spec, final String path) {
        switch (spec.scope()) {
            case EXTERNAL:
                return spec.exposedAt();
            case REPOSITORY:
                return append(exposedBaseURI(), exposePath, "", spec.exposedAt().getPath());
            case RESOURCE:
                if (path == null || path.equals("")) {
                    return append(exposedBaseURI(), exposePath, spec.exposedAt());
                }
                return append(exposedBaseURI(), exposePath, path, spec.exposedAt());
            default:
                throw new RuntimeException("Unknown service exposure scope " + spec.scope());
        }
    }

    @Override
    public URI endpointFor(final Extension.ServiceExposureSpec spec, final URI onResource) {
        return endpointFor(spec, resourcePath(onResource));
    }

    @Override
    public URI serviceDocFor(final URI resource) {
        return serviceDocFor(resourcePath(resource));
    }

    @Override
    public URI serviceDocFor(final String resourcePath) {
        return append(exposedBaseURI(), discoveryPath, resourcePath);
    }

    @Override
    public String resourcePath(final URI resourceURI) {

        final String r = resourceURI.toString();

        if (r.startsWith(fcrepoBaseURI.toString())) {
            return "/" + fcrepoBaseURI.relativize(resourceURI).getPath();
        }

        final URI interceptBase = append(exposedBaseURI(), interceptPath);

        if (r.startsWith(interceptBase.toString())) {
            return "/" + interceptBase.relativize(resourceURI).getPath();
        } else {
            throw new ResourceNotFoundException(String.format(
                    "%s is not in the repository or intercept domain (%s, %s)", r, fcrepoBaseURI, interceptBase));
        }

    }

    @Override
    public URI interceptUriFor(final URI resource) {
        if (resource.getPath().startsWith(interceptPath.toString())) {
            return resource;
        }
        return append(exposedBaseURI(), interceptPath, resourcePath(resource));
    }

    @Override
    public URI nonProxyURIFor(final URI resource) {
        if (resource.getPath().startsWith(fcrepoBaseURI.toString())) {
            return resource;
        }
        return append(fcrepoBaseURI, resourcePath(resource));
    }

    /**
     * Implementation note: the returned {@code Routing} implementation is immutable; invoking any setters will result
     * in an {@code UnsupportedOperationException} being thrown.
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param requestUri {@inheritDoc}
     * @return answers an <em>immutable</em> {@code Routing} implementation
     */
    @Override
    public Routing of(final URI requestUri) {
        if (requestUri == null) {
            throw new NullPointerException("Request URI must not be null!");
        }
        return new ImmutableRouter(requestUri.getScheme(),
                requestUri.getHost(),
                (requestUri.getPort() < 0) ? 80 : requestUri.getPort(),
                this);
    }

    /**
     * Implementations are expected to provide the base URI for HTTP resources publicly exposed by API-X.  Typically
     * this will be derived from the HTTP request received by API-X from the user-agent.  This base URI will be used to
     * construct URIs in responses to the client.
     * <p>
     * If a request is received by API-X at http://example.org/fcrepo/rest/foo, then the exposed base URI would be
     * {@code http://example.org/}.  If a request is received by API-X at http://example.org:8080/fcrepo/rest/foo, then
     * the exposed base URI would be {@code http://example.org:8080/}.  Implementations are free to use the HTTP
     * {@code Host} header or any other means to determine the publicly-facing API-X base URI.
     * </p>
     *
     * @return the publicly-facing base URI of resources exposed by API-X.
     */
    abstract URI exposedBaseURI();

}
