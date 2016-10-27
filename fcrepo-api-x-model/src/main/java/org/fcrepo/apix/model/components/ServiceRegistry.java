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

package org.fcrepo.apix.model.components;

import java.net.URI;

import org.fcrepo.apix.model.Service;

/**
 * @author apb@jhu.edu
 */
public interface ServiceRegistry extends Registry {

    /**
     * Get a registry of all instances of a given service.
     *
     * @param service the service.
     * @return registry of service instances.
     */
    ServiceInstanceRegistry instancesOf(Service service);

    /**
     * Create a new service instance registry.
     *
     * @param service The service for which to register instances.
     * @return new service instance registry
     */
    ServiceInstanceRegistry createInstanceRegistry(Service service);

    /**
     * Register the given uri as a service.
     * <p>
     * This does not deposit content in the registry, only references a web resource as a service.
     * </p>
     *
     * @param uri URI to the service resource.
     */
    public void register(URI uri);

    /**
     * Retrieve an abstract representation of a service.
     *
     * @param uri URI of the service
     * @return Service instance
     * @throws ResourceNotFoundException if the service is not in the registry.
     */
    Service getService(URI uri) throws ResourceNotFoundException;
}
