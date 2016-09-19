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

/**
 * Component that can update its internal state in response to external changes.
 * <p>
 * External changes include updates to repository objects.
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface Updateable {

    /**
     * Update internal state, in whatever manner is appropriate.
     */
    void update();

    /**
     * Update internal state in response to changes to the given resource.
     * <p>
     * Given the identity of a resource that mat have changed, an implementation may decide if and how to update
     * itself.
     * </p>
     *
     * @param inResponseTo URI of some web resource.
     */
    void update(URI inResponseTo);

}
