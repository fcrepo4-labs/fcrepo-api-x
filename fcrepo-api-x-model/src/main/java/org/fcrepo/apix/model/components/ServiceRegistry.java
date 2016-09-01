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
     * Retrieve an abstract representation of a service with the
     *
     * @param uri URI of the service
     * @return Service instance, or a runtime exception if not found.
     */
    Service getService(URI uri);
}
