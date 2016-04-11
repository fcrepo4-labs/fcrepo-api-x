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
package org.fcrepo.apix.poc.amherst.registry.memory;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fcrepo.apix.poc.amherst.registry.api.RegistryService;
import org.slf4j.Logger;

/**
 * @author acoburn
 * @since 1/25/16
 */
public class RegistryServiceImpl implements RegistryService {

    private static final Logger LOGGER  = getLogger(RegistryServiceImpl.class);

    private Map<String, String> descriptions = new HashMap<>();

    /**
     *  Get the description of a service
     *
     *  @param identifier the identifying key for the service
     *  @return the description of the service
     */
    public String get(final String identifier) {
        return descriptions.get(identifier);
    }

    /**
     *  Register a new service
     *
     *  @param identifier the identifying key for the service
     *  @param description the description
     *  @return whether the operation was successful
     */
    public Boolean put(final String identifier, final String description) {
        descriptions.put(identifier, description);
        return true;
    }

    /**
     *  Deregister a service
     *
     *  @param identifier the identifying key for the service
     *  @return whether the operation was successful
     */
    public Boolean remove(final String identifier) {
        descriptions.remove(identifier);
        return true;
    }

    /**
     *  Determine whether the service has been registered
     *
     *  @param identifier the idyntifying key for the service
     *  @return whether the service has been registered
     */
    public Boolean exists(final String identifier) {
        return descriptions.containsKey(identifier);
    }

    /**
     *  List all services
     *
     *  @return the complete service catalog
     */
    public List<String> list() {
        return descriptions.entrySet().stream().map(x -> x.getKey()).collect(toList());
    }
}
