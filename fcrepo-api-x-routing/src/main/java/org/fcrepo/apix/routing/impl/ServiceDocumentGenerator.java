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

package org.fcrepo.apix.routing.impl;

import static org.fcrepo.apix.model.Extension.Scope.RESOURCE;
import static org.fcrepo.apix.model.Ontologies.ORE_AGGREGATES;
import static org.fcrepo.apix.model.Ontologies.ORE_DESCRIBES;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_DOCUMENT;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_FUNCTION_OF;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_DOCUMENT_FOR;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_INSTANCE_OF;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_SERVICE_INSTANCE_EXPOSED_BY;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.ServiceExposureSpec;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.apix.model.components.ServiceDiscovery;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.util.ResourceUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

/**
 * Generates service documents for resources.
 *
 * @author apb@jhu.edu
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ServiceDocumentGenerator implements ServiceDiscovery {

    private ExtensionBinding extensionBinding;

    private Routing routing;

    private boolean useRelativeURIs = false;

    private boolean useInterceptedURIs = false;

    /**
     * Set extension binding impl
     *
     * @param binding extension binding impl
     */
    @Reference
    public void setExtensionBinding(final ExtensionBinding binding) {
        this.extensionBinding = binding;
    }

    /**
     * Set the routing component.
     *
     * @param routing The routing component.
     */
    @Reference
    public void setRouting(final Routing routing) {
        this.routing = routing;
    }

    /**
     * Specify whether the service document should use relative URIs.
     *
     * @param relative true if URIs should be relative.
     */
    public void setRelativeURIs(final boolean relative) {
        useRelativeURIs = relative;
    }

    /**
     * Specify whether intercept/proxy URI for the resource will be linked.
     *
     * @param use whether to use intercept URIs.
     */
    public void setInterceptURIs(final boolean use) {
        this.useInterceptedURIs = use;
    }

    @Override
    public WebResource getServiceDocumentFor(final URI resource, final String... contentType) {

        try {
            final ServiceDocumentImpl doc = new ServiceDocumentImpl(resource, pickMediaType(contentType));

            extensionBinding.getExtensionsFor(resource).stream()
                    .filter(Extension::isExposing)
                    .map(Extension::exposed).forEach(doc::expose);
            return doc;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class ServiceDocumentImpl implements WebResource {

        final String resourceURI;

        final String proxyURI;

        final Resource self;

        final Resource services;

        final Lang lang;

        final Model doc = ModelFactory.createDefaultModel();

        final String base;

        ServiceDocumentImpl(final URI uri, final Lang rdfLang) {
            this.resourceURI = uri.toString();
            this.proxyURI = useInterceptedURIs ? routing.interceptUriFor(uri).toString() : uri.toString();
            this.lang = rdfLang;
            this.base = useRelativeURIs ? "" : routing.serviceDocFor(uri).toString();
            self = doc.getResource(base);

            services = doc.createResource(base + "#services");

            self.addProperty(doc.getProperty(RDF_TYPE), doc.getResource(CLASS_SERVICE_DOCUMENT));
            self.addProperty(doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR), doc.getResource(proxyURI));
            self.addProperty(doc.getProperty(ORE_DESCRIBES), services);
        }

        private void expose(final ServiceExposureSpec spec) {
            final Resource serviceInstance = doc.createResource(base + "#" + UUID.randomUUID().toString());
            services.addProperty(doc.getProperty(ORE_AGGREGATES), serviceInstance);

            serviceInstance.addProperty(doc.getProperty(RDF_TYPE), doc.getResource(CLASS_SERVICE_INSTANCE));
            serviceInstance.addProperty(doc.getProperty(PROP_SERVICE_INSTANCE_EXPOSED_BY),
                    doc.getResource(proxyURI));

            serviceInstance.addProperty(doc.getProperty(PROP_IS_SERVICE_INSTANCE_OF),
                    doc.getResource(spec.exposedService().toString()));

            serviceInstance.addProperty(doc.getProperty(PROP_HAS_ENDPOINT),
                    doc.getResource(routing.endpointFor(spec, URI.create(resourceURI)).toString()));

            if (RESOURCE.equals(spec.scope())) {
                serviceInstance.addProperty(doc.getProperty(PROP_IS_FUNCTION_OF),
                        doc.getProperty(proxyURI));
            }

        }

        @Override
        public void close() throws Exception {
            // Nothing
        }

        @Override
        public String contentType() {
            return lang.getContentType().getContentType();
        }

        @Override
        public URI uri() {
            return URI.create(base);
        }

        @Override
        public String name() {
            return null;
        }

        @Override
        public InputStream representation() {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            final RDFWriter writer = doc.getWriter(lang.getName());

            // To allow relative URIs in XML, if desired
            writer.setProperty("allowBadURIs", "true");

            if (useRelativeURIs && Lang.NTRIPLES.equals(lang)) {

                final String serviceDocURI = routing.serviceDocFor(URI.create(resourceURI)).toString();

                doc.listSubjects()
                        .andThen(
                                doc.listObjects()
                                        .filterKeep(RDFNode::isURIResource)
                                        .mapWith(RDFNode::asResource))
                        .filterKeep(Resource::isURIResource)
                        .filterKeep(u -> u.getURI().startsWith("#") || u.getURI().equals("")).toSet()
                        .forEach(r -> ResourceUtils.renameResource(r,
                                serviceDocURI + r.getURI()));
            }

            writer.write(doc, out, base);

            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private static Lang pickMediaType(final String... type) {

        final List<Lang> langs = Arrays.stream(type)
                .map(t -> "text/*".equals(t) ? "text/turtle" : t)
                .map(t -> "*/*".equals(t) ? "text/turtle" : t)
                .map(RDFLanguages::contentTypeToLang)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return langs.isEmpty() ? Lang.TURTLE : langs.get(0);
    }
}
