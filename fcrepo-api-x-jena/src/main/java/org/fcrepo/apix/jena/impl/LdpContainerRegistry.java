
package org.fcrepo.apix.jena.impl;

import java.net.URI;
import java.util.Collection;

import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.Registry;
import org.fcrepo.apix.model.WebResource;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Uses a single LDP container as a registry. This uses Jena to parse the LDP membership list, and a delegate
 * (presumably a http-based delegate) to GET individual resources. {@link #put(WebResource)} will issue a PUT or GET
 * to a given container as appropriate to create or update a resource.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class LdpContainerRegistry implements Registry {

    private Registry delegate;

    private URI containerId;

    private CloseableHttpClient client;

    private boolean binary = false;

    private boolean create = true;

    @Reference
    public void setRegistryDelegate(Registry registry) {
        this.delegate = registry;
    }

    @Reference
    public void setHttpClient(CloseableHttpClient client) {

        this.client = client;
    }

    public void init() {
        if (create) {
            put(WebResource.of(null, "text/turtle",
                    containerId, null), false);
        }
    }

    public void setContainer(URI containerId) {
        this.containerId = containerId;
    }

    public void setBinary(boolean binary) {
        this.binary = binary;
    }

    public void setCreateContainer(boolean create) {
        this.create = create;
    }

    @Override
    public WebResource get(URI id) {
        return delegate.get(id);
    }

    @Override
    public URI put(WebResource resource) {
        return put(resource, binary);
    }

    public URI put(WebResource resource, boolean asBinary) {
        HttpEntityEnclosingRequestBase request = null;

        if (resource.uri() == null || !resource.uri().isAbsolute()) {
            request = new HttpPost(containerId);
        } else {
            request = new HttpPut(resource.uri());
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

        try (CloseableHttpResponse response = client.execute(request)) {
            final int status = response.getStatusLine().getStatusCode();

            if (status == HttpStatus.SC_CREATED) {
                return URI.create(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
            } else if (status == HttpStatus.SC_NO_CONTENT || status == HttpStatus.SC_OK) {
                return resource.uri();
            } else {
                throw new RuntimeException("Resource creation failed " + response.getStatusLine().toString());
            }
        } catch (final Exception e) {
            throw new RuntimeException(request.getURI().toString(), e);
        }
    }

    @Override
    public boolean canWrite() {
        return true;
    }

    @Override
    public Collection<URI> list() {
        final Model model = Util.parse(delegate.get(containerId));

        return model.listObjectsOfProperty(model.getProperty(Ontology.LDP_NS + "contains")).mapWith(
                RDFNode::asResource)
                .mapWith(Resource::getURI).mapWith(URI::create).toSet();
    }

    @Override
    public void delete(URI uri) {
        try (CloseableHttpResponse response = client.execute(new HttpDelete(uri))) {
            final StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != HttpStatus.SC_NO_CONTENT && status.getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException(String.format("DELETE failed on %s: %s", uri, status));
            }
        } catch (final Exception e) {
            throw new RuntimeException(uri.toString(), e);
        }

    }
}
