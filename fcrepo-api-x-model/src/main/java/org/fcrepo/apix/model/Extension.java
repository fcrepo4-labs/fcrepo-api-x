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

package org.fcrepo.apix.model;

import java.net.URI;
import java.util.Set;

/**
 * Abstract notion of an API-X extension.
 * <p>
 * TODO: This is mostly a stub
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface Extension {

    /**
     * RDF type this extension binds to.
     *
     * @return URI of the <code>rdf:type</code> this extension binds to.
     */
    public URI bindingClass();

    /**
     * Determine if this extension exposes services.
     *
     * @return true if the extension exposes services
     */
    public boolean isExposing();

    /**
     * Determine if this extension intercepts requests.
     *
     * @return true if the extension intercepts requests.
     */
    public boolean isIntercepting();

    /**
     * Get specification for exposed services.
     *
     * @return Service exposure specification.
     */
    public ServiceExposureSpec exposed();

    /**
     * Get specification for intercepting modality.
     *
     * @return Specification.
     */
    public Spec intercepted();

    /**
     * The URI (location) of the extension.
     * <p>
     * This is a potentially more lightweight operation than <code>getResource().uri()</code>
     * </p>
     *
     * @return its resolvable URI.
     */
    public URI uri();

    /**
     * Underlying RDF representation of an extension.
     *
     * @return A serialization of the extension resource.
     */
    public WebResource getResource();

    /**
     * Implementation specification for an extension.
     */
    public interface Spec {

        /**
         * Set of consumed services.
         *
         * @return Non-null set.
         */
        public Set<Service> consumed();

    }

    /**
     * Implementation specification for a service-exposing specification.
     */
    public interface ServiceExposureSpec extends Spec {

        /**
         * The exposed service.
         *
         * @return the exposed service
         */
        public Service exposed();

        /**
         * The scope of the exposed service.
         *
         * @return scope (external, repository, resource)
         */
        public Scope scope();
    }

    public enum Scope {
        RESOURCE,
        REPOSITORY,
        EXTERNAL
    }
}
