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

package org.fcrepo.apix.registry.impl;

import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.apache.http.HttpStatus.SC_GONE;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple HTTP-based registry that performs GET for lookups on a given URI.
 *
 * @author apb@jhu.edu
 */
public class HttpRegistry implements Registry {

    private static final Logger LOG = LoggerFactory.getLogger(HttpRegistry.class);

    private CloseableHttpClient client;

    static final String RDF_MEDIA_TYPES = "application/rdf+xml, text/turtle";

    /**
     * Set the underlying httpClient used by this registry.
     *
     * @param client closeable http client.
     */
    public void setHttpClient(final CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public WebResource get(final URI id) {

        final HttpGet get = new HttpGet(id);
        get.setHeader(ACCEPT, RDF_MEDIA_TYPES);

        return new WebResource() {

            final ResponseMgr mgr = new ResponseMgr(get);

            @Override
            public URI uri() {
                return id;
            }

            @Override
            public InputStream representation() {
                return mgr.getStream();
            }

            @Override
            public String name() {
                return null;
            }

            @Override
            public String contentType() {
                return mgr.getResponse().getFirstHeader(CONTENT_TYPE).getValue();
            }

            @Override
            public void close() throws Exception {
                mgr.close();
            }
        };
    }

    private CloseableHttpResponse execute(final HttpUriRequest request) {
        CloseableHttpResponse response = null;

        try {
            response = client.execute(request);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        final int code = response.getStatusLine().getStatusCode();
        if (code != SC_OK) {
            try {
                if (code == SC_NOT_FOUND || code == SC_GONE) {
                    throw new ResourceNotFoundException("HTTP " + code + ": " + request.getURI());
                }

                try {
                    LOG.warn(IOUtils.toString(response.getEntity().getContent(), Charset.forName("UTF_8")));
                } catch (final Exception e) {
                    LOG.warn(Integer.toString(response.getStatusLine().getStatusCode()));
                }
                throw new RuntimeException(String.format("Error performing %s on %s: %s; %s", request.getMethod(),
                        request
                                .getURI(), response.getStatusLine(), body(response)));

            } finally {
                try {
                    response.close();
                } catch (final IOException e) {
                    // nothing
                }
            }
        }

        return response;
    }

    private String body(final HttpResponse response) {
        try {
            return EntityUtils.toString(response.getEntity());
        } catch (final Exception e) {
            return "";
        }
    }

    // Some last-minute refactoring, there might be a better way to do this
    private class ResponseMgr {

        final HttpGet get;

        CloseableHttpResponse response;

        boolean isClosed = true;

        ResponseMgr(final HttpGet get) {
            this.get = get;
            getResponse();
        }

        CloseableHttpResponse getResponse() {
            if (isClosed) {
                try {
                    if (response != null) {
                        response.close();
                    }
                    response = execute(get);
                    isClosed = false;
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return response;
        }

        InputStream getStream() {
            try {
                if (!isClosed && response.getEntity().isRepeatable()) {
                    return response.getEntity().getContent();
                } else {
                    getResponse();
                    return new FilterInputStream(response.getEntity().getContent()) {

                        @Override
                        public void close() throws IOException {
                            isClosed = true;
                            super.close();
                        }
                    };
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        public void close() throws IOException {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    public boolean contains(final URI uri) {
        try (CloseableHttpResponse response = client.execute(new HttpHead(uri))) {
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI put(final WebResource resource, final boolean asBinary) {
        throw new UnsupportedOperationException();
    }

    @Override
    public URI put(final WebResource resource) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public Collection<URI> list() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(final URI uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasInDomain(final URI uri) {
        return uri.getScheme().startsWith("http");
    }
}
