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

package org.fcrepo.apix.routing;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.ServiceInstance;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;

/**
 * URI parsiing and manipulation utilities.
 *
 * @author apb@jhu.edu
 */
public abstract class Util {

    private static final Random random = new Random();

    /**
     * Normalize a URI path segment to remove forward and trailing slashes, if present.
     *
     * @param path path segment
     * @return Normalized segment
     */
    public static String segment(final String path) {
        return path.replaceFirst("^/", "").replaceFirst("/$", "");
    }

    /**
     * Normalize a terminall URI path segment by removing forward slashes, if present.
     *
     * @param path path segment
     * @return Normalized segmen
     */
    public static String terminal(final String path) {
        return path.equals("") ? "/" : path.replaceFirst("^/", "");
    }

    /**
     * Append URI path segments together.
     *
     * @param segments URI path segment
     * @return URI composed of appended segments.
     */
    public static URI append(final Object... segments) {
        return URI.create(
                Arrays.stream(segments)
                        .reduce((a, b) -> String.join("/", segment(a.toString()), terminal(b.toString())))
                        .get().toString());
    }

    /**
     * Find an instance of the consumed service for an exposing extension.
     *
     * @param extension The extension
     * @param serviceRegistry A service registry.
     * @return URI if a service instance endpoint.
     */
    public static URI exposedServiceInstance(final Extension extension, final ServiceRegistry serviceRegistry) {
        final URI consumedServiceURI = exactlyOne(extension.exposed().consumed(),
                "Exposed services must have exactly one consumed service in extension: " +
                        extension.uri());

        return instance(consumedServiceURI, serviceRegistry);
    }

    /**
     * Find instance of the consumed service for an intercepting extension.
     *
     * @param extension The extension
     * @param serviceRegistry The service registry
     * @return URI of a service instance endpoint
     */
    public static URI interceptingServiceInstance(final Extension extension, final ServiceRegistry serviceRegistry) {
        final URI consumedServiceURI = exactlyOne(extension.intercepted().consumed(),
                "Exposed services must have exactly one consumed service in extension: " +
                        extension.uri());
        return instance(consumedServiceURI, serviceRegistry);

    }

    private static URI instance(final URI serviceURI, final ServiceRegistry serviceRegistry) {
        final ServiceInstanceRegistry instanceRegistry = serviceRegistry.instancesOf(serviceRegistry.getService(
                serviceURI));

        if (instanceRegistry == null) {
            throw new ResourceNotFoundException("No instance registry for service " + serviceURI);
        }

        final ServiceInstance instance = oneOf(instanceRegistry.instances(),
                "There must be at least one service instance for " + serviceURI);

        return oneOf(
                instance.endpoints(),
                "There must be at least one endpoint for instances of " + serviceURI);
    }

    private static <T> T exactlyOne(final Collection<T> of, final String errMsg) {
        if (of.size() != 1) {
            throw new ResourceNotFoundException(errMsg);
        }

        return of.iterator().next();
    }

    private static <T> T oneOf(final List<T> of, final String errMsg) {
        if (of.isEmpty()) {
            throw new ResourceNotFoundException(errMsg);
        }

        return of.get(random.nextInt(of.size()));
    }
}
