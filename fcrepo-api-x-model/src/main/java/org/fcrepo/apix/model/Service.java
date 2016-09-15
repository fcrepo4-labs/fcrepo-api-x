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

package org.fcrepo.apix.model;

import java.net.URI;

/**
 * Abstract notion of an API-X service.
 *
 * @author apb@jhu.edu
 */
public interface Service extends WebResource {

    /**
     * The canonical URI for a service.
     *
     * @return Canonical URI. If none defined, then this is implicitly the URI of the service resource itself.
     */
    URI canonicalURI();
}
