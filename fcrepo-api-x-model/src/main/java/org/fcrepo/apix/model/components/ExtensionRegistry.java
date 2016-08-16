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

/**
 * Registry containing extensions.
 *
 * @author apb@jhu.edu
 */
public interface ExtensionRegistry extends Registry {

    /**
     * Retrieve the abstract representation of a specific extension by URI.
     * <p>
     * Transforms the resource retrievable by {@link #get(URI)} into an abstract notion of an extension.
     * </p>
     *
     * @param uri URI of the desired extension.
     * @return Abstract representation of an extension.
     */
    public Extension getExtension(URI uri);

    /**
     * Retrieve all known extension. Contains a transformation of all resources accessible via
     * {@link ExtensionRegistry#list()}.
     *
     * @return A collection of all known extensions, or null if none.
     */
    public Collection<Extension> getExtensions();
}
