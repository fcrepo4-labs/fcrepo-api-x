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

package org.fcrepo.apix.registry;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Helps get properties from OSGi ConfigurationAdmin.
 *
 * @author apb@jhu.edu
 */
public class ConfigHelper {

    private ConfigurationAdmin configAdmin;

    /**
     * Set the OSGi configurationAdmin.
     *
     * @param config configuration.
     */
    public void setConfigurationAdmin(final ConfigurationAdmin config) {
        this.configAdmin = config;
    }

    /**
     * Get properties as a map.
     * <p>
     * Returns an empty map if configAdmin is null, or the configuration is null, etc.
     * </p>
     *
     * @param pid OSGI configAdmin pid.
     * @return map of properties
     */
    public Map<String, String> getProperties(final String pid) throws Exception {
        final Map<String, String> props = new HashMap<>();
        if (configAdmin != null) {
            final Configuration c = configAdmin.getConfiguration(pid);
            if (c != null) {
                final Dictionary<String, Object> dict = c.getProperties();
                if (dict != null) {
                    final Enumeration<String> keys = dict.keys();
                    while (keys.hasMoreElements()) {
                        final String key = keys.nextElement();
                        props.put(key, dict.get(key).toString());
                    }
                }
            }
        }

        return props;
    }
}
