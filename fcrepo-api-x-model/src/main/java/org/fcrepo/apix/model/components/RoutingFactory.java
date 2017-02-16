/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * Produces {@link Routing} instances for requested resources.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface RoutingFactory {

    /**
     * Answers a {@link Routing} for the requested resource.
     *
     * @param requestUri the URI of the requested resource
     * @return the {@code Routing}
     */
    Routing of(URI requestUri);

}
