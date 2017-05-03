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

package org.fcrepo.apix.jena.impl;

import java.util.Collection;

import org.apache.http.impl.client.CloseableHttpClient;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Fetches HttpClients from the OSGi registry.
 * <p>
 * Aries Blueprint cannot to variable substitution in service reference filtering, so this class achieves the same
 * effect programatically. Given the <code>osgi.jndi.service.name</code> of an HttpClient in the service registry,
 * this class will retrieve the service reference.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class HttpClientFetcher {

    private String serviceName;

    private BundleContext bundleContext;

    /**
     * Set the osgi.jndi.service.name to retrieve an HttpClient.
     *
     * @param name osgi.jndi.service.name
     */
    public void setServiceName(final String name) {
        this.serviceName = name;
    }

    /**
     * Set the bundle context.
     *
     * @param cxt Bundle context
     */
    public void setBundleContext(final BundleContext cxt) {
        this.bundleContext = cxt;
    }

    /**
     * Get an httpClient from the OSGi registry.
     *
     * @return the client
     * @throws Exception thrown when exactly one matching client cannot be retrieved.
     */
    public CloseableHttpClient getClient() throws Exception {

        final Collection<ServiceReference<CloseableHttpClient>> refs = bundleContext.getServiceReferences(
                CloseableHttpClient.class, String.format("(osgi.jndi.service.name=%s)", serviceName));

        if (refs.size() != 1) {
            throw new RuntimeException("Expecting to find exactly one CloseableHttpClient with " +
                    "osgi.jndi.service.name=" + serviceName + "instead, found " + refs.size());
        }

        return bundleContext.getService(refs.iterator().next());
    }

}
