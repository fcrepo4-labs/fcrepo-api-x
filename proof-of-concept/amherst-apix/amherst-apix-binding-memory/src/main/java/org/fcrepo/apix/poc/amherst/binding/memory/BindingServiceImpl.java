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
package org.fcrepo.apix.poc.amherst.binding.memory;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.List;
import java.util.Map;

import org.fcrepo.apix.poc.amherst.binding.api.BindingService;
import org.slf4j.Logger;

/**
 * @author acoburn
 * @since 1/25/16
 */
public class BindingServiceImpl implements BindingService {

    private static final Logger LOGGER  = getLogger(BindingServiceImpl.class);

    private Map<String, Set<String>> instances = new HashMap<>();

    /**
     *  Bind a service instance to a registry
     *
     *  @param identifier the identifying key for the service
     *  @param endpoint the endpoint
     *  @return whether the operation was successful
     */
    public Boolean bind(final String identifier, final String endpoint) {
        if (!instances.containsKey(identifier)) {
            instances.put(identifier, new HashSet<>());
        }
        return instances.get(identifier).add(endpoint);
    }

    /**
     *  Remove a service endpoint from the binding registry
     *
     *  @param identifier the identifying key for the service
     *  @param endpoint the endpoint
     *  @return whether the operation was successful
     */
    public Boolean unbind(final String identifier, final String endpoint) {
        if (instances.containsKey(identifier)) {
            return instances.get(identifier).remove(endpoint);
        }
        return false;
    }

    /**
     *  List all endpoints
     *
     *  @return the complete service catalog
     */
    public List<String> list(final String identifier) {
        if (instances.containsKey(identifier)) {
            return instances.get(identifier).stream().collect(toList());
        }
        return emptyList();
    }

    /**
     *  Pick a random endpoint
     *
     *  @return an endpoint
     */
    public String findAny(final String identifier) {
        final List<String> endpoints = list(identifier);
        if (!endpoints.isEmpty()) {
            final Random rand = new Random();
            return endpoints.get(rand.nextInt(endpoints.size()));
        }
        return null;
    }
}
