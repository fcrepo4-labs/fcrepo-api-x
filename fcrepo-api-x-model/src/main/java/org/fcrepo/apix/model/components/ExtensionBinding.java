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

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;

/**
 * Binds extensions to resources.
 * <p>
 * An extension describes the class of resources that it may be bound to. Implementations of this service determine
 * (eg via OWL reasoning) whether a given resource meets the binding criteria of a given extension.
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface ExtensionBinding {

    /**
     * Determine all known extensions that bind to the given resource.
     * <p>
     * This is intended for calculating the set of extensions that bind to given, physical resource. There is no
     * presumption that this resource exists on the repository or the web (i.e. it can be from a request).
     * </p>
     * <p>
     * Implementations will consult an underlying registry of extensions
     * </p>
     *
     * @param resource Candidate resource.
     * @return All extensions that bind to the given resource, or an empty collection if none.
     */
    public Collection<Extension> getExtensionsFor(WebResource resource);

    /**
     * Determine which of the given extensions bind to the given resource.
     *
     * @param resource Candidate resource
     * @param from Candidate extensions
     * @return All extensions from the list that bind to the resource, or empty if none.
     */
    public Collection<Extension> getExtensionsFor(WebResource resource, Collection<Extension> from);

    /**
     * Determine all known extensions that bind to the given resource, given its URI.
     * <p>
     * The given URI may be a resource in the repository, on the web, or otherwise within a registry specified by the
     * implementation of this component. Depending on implementation, results may be based on lookup of pre-computed
     * bindings.
     * </p>
     * <p>
     * Implementations will consult an underlying registry of extensions
     * </p>
     *
     * @param resourceURI URI of the candidate resource.
     * @return All extensions that bind to the given resource, or an empty collection if none.
     */
    public Collection<Extension> getExtensionsFor(URI resourceURI);

    /**
     * Determine which of the given extensions bind to the given resource.
     *
     * @param resourceURI URI of resource, will be dereferenced.
     * @param from from Candidate extensions
     * @return All extensions from the list that bind to the resource, or empty if none.
     */
    public Collection<Extension> getExtensionsFor(URI resourceURI, Collection<Extension> from);

}
