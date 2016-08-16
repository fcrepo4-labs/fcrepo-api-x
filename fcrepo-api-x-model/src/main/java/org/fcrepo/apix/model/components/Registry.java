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
import java.util.Collection;

import org.fcrepo.apix.model.WebResource;

/**
 * Registry for retrieving and persisting some sort of resource.
 */
public interface Registry {

    /**
     * Get a resource
     *
     * @param id URI of the resource
     * @return A serialized resource, or an exception if none found.
     */
    public WebResource get(URI id);

    /**
     * Persist a resource in the registry.
     *
     * @param id URI of the resource
     * @param resource serialized resource;
     * @return URI of the resource, as persisted in the registry;
     */
    public URI put(WebResource resource);

    /**
     * Delete a resource from the registry.
     *
     * @param uri URI of the resource to delete.
     */
    public void delete(URI uri);

    /**
     * Determines if resources can be written to this registry.
     *
     * @return true if writable via {@link #put(URI, WebResource)}
     */
    public boolean canWrite();

    /**
     * Lists all resources in the registry.
     *
     * @return possibly unordered list of all resource URIs in the registry.
     */
    public Collection<URI> list();

    /**
     * Determine if the registry contains the given resource.
     *
     * @param id URI of the resource to check.
     * @return
     */
    public boolean contains(URI id);
}
