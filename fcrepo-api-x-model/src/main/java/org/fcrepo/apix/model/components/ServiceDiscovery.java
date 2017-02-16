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

import org.fcrepo.apix.model.WebResource;

/**
 * Service discovery component
 *
 * @author apb@jhu.edu
 */
public interface ServiceDiscovery {

    /**
     * Produce a service document for the given resource
     *
     * @param resource A repository resource URI
     * @param routing the {@code Routing} for the repository {@code resource}
     * @param contentType Desired media types, or null if any serialization is acceptable.
     * @return Serialized service document
     */
    WebResource getServiceDocumentFor(URI resource, Routing routing, String... contentType);

}
