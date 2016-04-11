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
package org.fcrepo.apix.poc.amherst.registry.api;

import java.util.List;

/**
 * @author acoburn
 * @since 1/23/16
 */
public interface RegistryService {

    /**
     *  Get the description of a service
     *
     *  @param identifier the identifying key for the service
     *  @return the description of the service
     */
    String get(final String identifier);

    /**
     *  Register a new service
     *
     *  @param identifier the identifying key for the service
     *  @param description the description
     *  @return whether the operation was successful
     */
    Boolean put(final String identifier, final String description);

    /**
     *  Deregister a service
     *
     *  @param identifier the identifying key for the service
     *  @return whether the operation was successful
     */
    Boolean remove(final String identifier);

    /**
     *  Determine whether a service has been registered
     *
     *  @param identifier the identifying key for the service
     *  @return whether the service has been registered
     */
    Boolean exists(final String identifier);

    /**
     *  List all services
     *
     *  @return the complete service catalog
     */
    List<String> list();
}
