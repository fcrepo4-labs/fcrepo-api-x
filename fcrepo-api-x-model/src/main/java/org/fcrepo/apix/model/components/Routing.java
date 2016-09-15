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

import org.fcrepo.apix.model.Extension.ServiceExposureSpec;

/**
 * Routing component.
 *
 * @author apb@jhu.edu
 */
public interface Routing {

    /**
     * Get the endpoint for the service exposed by the given extension on the given resource.
     *
     * @param spec specification for exposing a service
     * @param onResource the resource on which the service is exposed.
     * @return URI of exposed service, null if not applicable.
     */
    public URI endpointFor(ServiceExposureSpec spec, URI onResource);

    /**
     * Get the endpoint for the service doc of a given resource.
     *
     * @param resource the resource
     * @return URI that resolves to the service document.
     */
    public URI serviceDocFor(URI resource);

}
