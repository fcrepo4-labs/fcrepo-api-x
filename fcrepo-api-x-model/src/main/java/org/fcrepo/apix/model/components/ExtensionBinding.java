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

import java.util.Collection;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;

/**
 * Binds extensions to resources.
 * <p>
 * An extension describes the class of resources that it may be bound to. Implementations of this service determine
 * (eg via OWL reasoning) whether a given resource meets the binding criteria of a given extension.
 * </p>
 */
public interface ExtensionBinding {

    /**
     * Determne all known extensions that bind to the given resource.
     * <p>
     * Implementations will consult an underlying registry of extensions
     * </p>
     *
     * @param resource Candidate resource.
     * @return All extensions that bind to the given resource, or an empty collection if none.
     */
    public Collection<Extension> getExtensionsFor(WebResource resource);
}
