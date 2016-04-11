/**
 * Copyright 2015 Amherst College
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
package org.fcrepo.apix.poc.amherst.binding.api;

import java.util.List;

/**
 * @author acoburn
 * @since 1/23/16
 */
public interface BindingService {

    /**
     *  Bind a service instance to a registry
     *
     *  @param identifier the identifying key for the service
     *  @param endpoint the endpoint
     *  @return whether the operation was successful
     */
    Boolean bind(final String identifier, final String endpoint);

    /**
     *  Remove a service endpoint from the binding registry
     *
     *  @param identifier the identifying key for the service
     *  @param endpoint the endpoint
     *  @return whether the operation was successful
     */
    Boolean unbind(final String identifier, final String endpoint);

    /**
     *  List all endpoints
     *
     *  @return the complete service catalog
     */
    List<String> list(final String identifier);

    /**
     *  Pick a random endpoint
     *
     *  @return an endpoint
     */
    String findAny(final String identifier);
}
