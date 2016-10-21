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
import java.util.List;

import org.fcrepo.apix.model.ServiceInstance;

/**
 * Registry of service instances.
 * <p>
 * Implementations may use a variety of technologies, ranging from LDP to ZooKeeper.
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface ServiceInstanceRegistry {

    /**
     * List all service instances in this registry
     *
     * @return list of all instances.
     */
    public List<ServiceInstance> instances();

    /**
     * Add an endpoint to the registry.
     *
     * @param endpoint the endpoint.
     * @return URI of the corresponding service instance.
     */
    public URI addEndpoint(URI endpoint);
}
