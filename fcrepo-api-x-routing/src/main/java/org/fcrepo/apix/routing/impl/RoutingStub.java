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

import static org.fcrepo.apix.routing.Util.segment;
import static org.fcrepo.apix.routing.Util.terminal;

import java.net.URI;

import org.fcrepo.apix.model.Extension.ServiceExposureSpec;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.Routing;

/**
 * @author apb@jhu.edu
 */
public class RoutingStub implements Routing {

    private String host;

    private int port;

    private String discoveryPath;

    private String exposePath;

    private URI fcrepoBaseURI;

    /**
     * Set the API-X host.
     *
     * @param host IP or dns name
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Set the API-X port.
     *
     * @param port port number.
     */
    public void setPort(final int port) {
        this.port = port;
    }

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

    @Override
    public URI endpointFor(final ServiceExposureSpec spec, final URI onResource) {
        switch (spec.scope()) {
        case EXTERNAL:
            return spec.exposedAt();
        case REPOSITORY:
            return URI.create(String.format("http://%s:%s/%s//%s", host, port,
                    segment(exposePath),
                    terminal(spec.exposedAt().getPath())));
        case RESOURCE:
            return URI.create(String.format("http://%s:%s/%s/%s/%s", host, port, segment(exposePath),
                    segment(resourcePath(onResource)),
                    terminal(spec.exposedAt().getPath())));
        default:
            throw new RuntimeException("Unknown service exposure scope " + spec.scope());
        }
    }

    @Override
    public URI serviceDocFor(final URI resource) {
        return URI.create(String.format("http://%s:%d/%s/%s", host, port, segment(discoveryPath), terminal(
                resourcePath(resource))));
    }

    @Override
    public String resourcePath(final URI resourceURI) {

        if (!resourceURI.toString().startsWith(fcrepoBaseURI.toString())) {
            throw new ResourceNotFoundException(String.format("Resource URI %s is not prefixed by base URI %s",
                    resourceURI,
                    fcrepoBaseURI));
        }
        return "/" + fcrepoBaseURI.relativize(resourceURI).getPath();
    }

}
