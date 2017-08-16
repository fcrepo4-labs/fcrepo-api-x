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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@SuppressWarnings("deprecation")
public class HttpClientFetcher {

    private CloseableHttpClient defaultClient;

    private BundleContext bundleContext;

    private final AtomicReference<ServiceReference<HttpClient>> serviceClient = new AtomicReference<>();

    private final AtomicReference<CloseableHttpClient> client = new AtomicReference<>();

    Logger LOG = LoggerFactory.getLogger(HttpClientFetcher.class);

    /**
     * Set the default http client, if none specified.
     */
    public void setDefaultClient(final CloseableHttpClient client) {
        this.defaultClient = client;
        if (serviceClient.get() == null) {
            this.client.set(defaultClient);
            LOG.info("Set default client {}", this.client.get());
        }
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
     * Get an httpClient
     * <p>
     * Returns a proxy that matches either a default, or highest-priority HttpClient service from the OSGi service
     * registry.
     * </p>
     *
     * @return the client
     */
    public CloseableHttpClient getClient() {

        return new CloseableHttpClientProxy();
    }

    private class CloseableHttpClientProxy extends CloseableHttpClient {

        @Override
        public ClientConnectionManager getConnectionManager() {
            return client.get().getConnectionManager();
        }

        @Override
        public HttpParams getParams() {
            return client.get().getParams();
        }

        @Override
        public void close() throws IOException {
            client.get().close();
        }

        @Override
        protected CloseableHttpResponse doExecute(final HttpHost host, final HttpRequest req, final HttpContext cxt)
                throws IOException, ClientProtocolException {
            LOG.debug("Executing with client {}", client.get());
            return client.get().execute(host, req, cxt);
        }

    }

    /**
     * React to a newly bound service reference.
     *
     * @param clientRef
     */
    public synchronized void bind(final ServiceReference<HttpClient> clientRef) {
        LOG.info("Got new HttpClient service.");
        if (serviceClient.get() == null || clientRef.compareTo(serviceClient.get()) > 0) {
            serviceClient.set(clientRef);
            client.set((CloseableHttpClient) bundleContext.getService(clientRef));
            LOG.info("Using new HttpClient service " + client.get());
        } else {
            LOG.info("HttpClient service is lower priority.  Ignoring.");
        }
    }

    /**
     * Unbind a service reference.
     *
     * @param clientRef to be unbound.
     * @throws Exception
     */
    public synchronized void unbind(final ServiceReference<HttpClient> clientRef) throws Exception {
        if (clientRef != null && clientRef.equals(serviceClient.get())) {
            client.set(defaultClient);
            serviceClient.set(null);

            for (final ServiceReference<HttpClient> ref : bundleContext.getServiceReferences(HttpClient.class,
                    null)) {
                if (!ref.equals(clientRef)) {
                    bind(ref);
                }
            }
        }
    }

}
