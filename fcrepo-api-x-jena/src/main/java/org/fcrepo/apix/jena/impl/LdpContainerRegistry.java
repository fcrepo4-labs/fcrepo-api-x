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

import static org.fcrepo.apix.model.Ontologies.LDP_CONTAINS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import org.fcrepo.apix.jena.Util;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses a single LDP container as a registry.
 * <p>
 * This uses Jena to parse the LDP membership list, and a delegate (presumably a http-based delegate) to GET
 * individual resources. {@link #put(WebResource)} will issue a PUT or GET to a given container as appropriate to
 * create or update a resource.
 * </p>
 *
 * @author apb@jhu.edu
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LdpContainerRegistry implements Registry {

    private Registry delegate;

    private URI containerId;

    private CloseableHttpClient client;

    private boolean binary = false;

    private boolean create = true;

    private URI containerContent;

    private static final Logger LOG = LoggerFactory.getLogger(LdpContainerRegistry.class);

    /**
     * Underlying registry delegate for GETs.
     *
     * @param registry the registry.
     */
    @Reference
    public void setRegistryDelegate(final Registry registry) {
        this.delegate = registry;
    }

    /**
     * HttpClient for performing LDP requests.
     *
     * @param client the client.
     */
    @Reference
    public void setHttpClient(final CloseableHttpClient client) {

        this.client = client;
    }

    /**
     * Create the container if it doesn't exist.
     * <p>
     * TODO: make this more robust. What if the repository is not accessible at initialization time?
     * </p>
     */
    public void init() {

        boolean found = false;

        while (!found) {
            try {
                found = exists(containerId);

                if (create && !found) {
                    put(WebResource.of(initialContent(), "text/turtle",
                            containerId, null), false);
                    found = true;
                }
            } catch (final Exception e) {
                LOG.info("Failed initializing underlying container, re-trying..", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Set the URI of the LDP container containing objects relevant to this registry.
     *
     * @param containerId the LDP container URI
     */
    public void setContainer(final URI containerId) {
        this.containerId = containerId;
    }

    /**
     * Indicate whether to treat resources in this registry as binaries
     *
     * @param binary true if the resources are to be treated as binaries
     */
    public void setBinary(final boolean binary) {
        this.binary = binary;
    }

    /**
     * Indicate whether to attempt to create the container if not present
     *
     * @param create true if the container is to be created if it is missing
     */
    public void setCreateContainer(final boolean create) {
        this.create = create;
    }

    /**
     * Initial content content to initialize container with .
     * <p>
     * This is only relevant if {@link #setCreateContainer(boolean)} is true, at time of container creation. No
     * attempts to update the content of an existing container will be made.
     * </p>
     *
     * @param loc URI to a resource containing rdf. May be a file URI (for local content), classpath (for classpath
     *        resource), or http.
     */
    public void setContainerContent(final URI loc) {
        this.containerContent = loc;
    }

    @Override
    public WebResource get(final URI id) {
        return delegate.get(id);
    }

    @Override
    public URI put(final WebResource resource) {
        return put(resource, binary);
    }

    @Override
    public URI put(final WebResource resource, final boolean asBinary) {
        HttpEntityEnclosingRequestBase request = null;

        if (resource.uri() == null || !resource.uri().isAbsolute()) {
            request = new HttpPost(containerId);
        } else {
            request = new HttpPut(resource.uri());
            if (!asBinary) {
                request.addHeader("Prefer", "handling=lenient; received=\"minimal\"");
            }
        }

        if (asBinary) {
            request.addHeader("Content-Disposition", String.format("attachment; filename=%s", resource.uri() == null
                    ? "file.bin" : FilenameUtils.getName(resource.uri().getPath())));
        }

        if (resource.uri() != null && !resource.uri().isAbsolute()) {
            request.addHeader("Slug", resource.uri().toString());
        }

        if (resource.representation() != null) {
            request.setEntity(new InputStreamEntity(resource.representation()));
        }
        request.setHeader(HttpHeaders.CONTENT_TYPE, resource.contentType());

        try {
            return client.execute(request, (response -> {
                final int status = response.getStatusLine().getStatusCode();

                if (status == HttpStatus.SC_CREATED) {
                    return URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
                } else if (status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_OK) {
                    return resource.uri();
                } else {
                    throw new RuntimeException(String.format("Resource creation failed: %s; %s",
                            response.getStatusLine().toString(),
                            EntityUtils.toString(response.getEntity())));
                }
            }));
        } catch (final Exception e) {
            throw new RuntimeException(String.format("Error executing %s request to %s", request.getClass()
                    .getSimpleName(), request.getURI().toString()), e);
        }

    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public Collection<URI> list() {
        try {
            final Model model = Util.parse(delegate.get(containerId));

            return model.listObjectsOfProperty(model.getProperty(LDP_CONTAINS)).mapWith(
                    RDFNode::asResource)
                    .mapWith(Resource::getURI).mapWith(URI::create).toSet();
        } catch (final Exception e) {
            throw new RuntimeException("Error reading from " + containerId, e);
        }

    }

    @Override
    public void delete(final URI uri) {
        try (CloseableHttpResponse response = client.execute(new HttpDelete(uri))) {
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HttpStatus.SC_NO_CONTENT && status.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException(String.format("DELETE failed on %s: %s", uri, status));
            }
        } catch (final Exception e) {
            throw new RuntimeException(uri.toString(), e);
        }
    }

    private boolean exists(final URI uri) throws IOException {
        try (CloseableHttpResponse response = client.execute(new HttpHead(uri))) {
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        }
    }

    @Override
    public boolean contains(final URI id) {
        return list().contains(id);
    }

    private InputStream initialContent() {
        if (containerContent == null || containerContent.getScheme() == null) {
            return new ByteArrayInputStream(new byte[0]);
        } else if (containerContent.getScheme().equals("classpath")) {
            return this.getClass().getResourceAsStream(containerContent.getPath());
        }

        try {
            return containerContent.toURL().openStream();
        } catch (final IOException e) {
            throw new RuntimeException("Error retrieving container content from " + containerContent, e);
        }
    }

    @Override
    public boolean hasInDomain(final URI uri) {
        return uri.toString().startsWith(containerId.toString());
    }
}
