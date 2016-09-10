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

package org.fcrepo.apix.discovery.impl;

import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.jena.Util.query;
import static org.fcrepo.apix.jena.Util.subjectsOf;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_FUNCTION_OF;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_DOCUMENT_FOR;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_INSTANCE_OF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.Extension.ServiceExposureSpec;
import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.apix.model.components.ServiceRegistry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests service document generation.
 *
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class ServiceDocumentGeneratorTest {

    @Mock
    Routing routing;

    @Mock
    ExtensionBinding binding;

    @Mock
    WebResource theResource;

    @Mock
    Extension EXPOSING_EXTENSION_RESOURCE_SCOPED;

    @Mock
    Extension EXPOSING_EXTENSION_REPOSITORY_SCOPED;

    @Mock
    Extension EXPOSING_EXTENSION_UNREGISTERED;

    @Mock
    ServiceExposureSpec RESOURCE_SCOPE_SPEC;

    @Mock
    ServiceExposureSpec REPOSITORY_SCOPE_SPEC;

    @Mock
    ServiceExposureSpec UNREGISTERED_SERVICE_SPEC;

    @Mock
    Service RESOURCE_SCOPE_SERVICE;

    @Mock
    Service REPOSITORY_SCOPE_SERVICE;

    @Mock
    Extension INTERCEPTING_EXTENSION;

    @Mock
    ServiceRegistry SERVICE_REGISTRY;

    final URI RESOURCE_URI = URI.create("http://example.org/resource");

    final URI RESOURCE_SCOPE_SERVICE_URI = URI.create("http://example.org/services/resourceScoped");

    final URI REPOSITORY_SCOPE_SERVICE_URI = URI.create("http://example.org/services/repositoryScoped");

    final URI UNREGISTERED_SERVICE_URI = URI.create("http://example.org/services/unregistered");

    final URI RESOURCE_SCOPE_ENDPOINT_URI = URI.create("http://example.org/endpoint/resourceScoped");

    final URI REPOSITORY_SCOPE_ENDPOINT_URI = URI.create("http://example.org/endpoint/repositoryScoped");

    final URI UNREGISTERED_ENDPOINT_URI = URI.create("http://example.org/endpoont/unregistered");

    ServiceDocumentGenerator toTest = new ServiceDocumentGenerator();

    @Before
    public void setUp() {
        when(theResource.uri()).thenReturn(RESOURCE_URI);

        when(EXPOSING_EXTENSION_REPOSITORY_SCOPED.isExposing()).thenReturn(true);
        when(EXPOSING_EXTENSION_RESOURCE_SCOPED.isExposing()).thenReturn(true);
        when(EXPOSING_EXTENSION_UNREGISTERED.isExposing()).thenReturn(true);
        when(INTERCEPTING_EXTENSION.isExposing()).thenReturn(false);

        when(EXPOSING_EXTENSION_REPOSITORY_SCOPED.exposed()).thenReturn(REPOSITORY_SCOPE_SPEC);
        when(EXPOSING_EXTENSION_RESOURCE_SCOPED.exposed()).thenReturn(RESOURCE_SCOPE_SPEC);
        when(EXPOSING_EXTENSION_UNREGISTERED.exposed()).thenReturn(UNREGISTERED_SERVICE_SPEC);

        when(REPOSITORY_SCOPE_SPEC.exposed()).thenReturn(REPOSITORY_SCOPE_SERVICE_URI);
        when(REPOSITORY_SCOPE_SPEC.scope()).thenReturn(Scope.REPOSITORY);
        when(RESOURCE_SCOPE_SPEC.exposed()).thenReturn(RESOURCE_SCOPE_SERVICE_URI);
        when(RESOURCE_SCOPE_SPEC.scope()).thenReturn(Scope.RESOURCE);
        when(UNREGISTERED_SERVICE_SPEC.scope()).thenReturn(Scope.RESOURCE);
        when(UNREGISTERED_SERVICE_SPEC.exposed()).thenReturn(UNREGISTERED_SERVICE_URI);

        when(REPOSITORY_SCOPE_SERVICE.canonicalURI()).thenReturn(REPOSITORY_SCOPE_SERVICE_URI);
        when(RESOURCE_SCOPE_SERVICE.canonicalURI()).thenReturn(RESOURCE_SCOPE_SERVICE_URI);

        when(SERVICE_REGISTRY.getService(REPOSITORY_SCOPE_SERVICE_URI)).thenReturn(REPOSITORY_SCOPE_SERVICE);
        when(SERVICE_REGISTRY.getService(RESOURCE_SCOPE_SERVICE_URI)).thenReturn(RESOURCE_SCOPE_SERVICE);
        when(SERVICE_REGISTRY.getService(UNREGISTERED_SERVICE_URI)).thenThrow(new ResourceNotFoundException("NO"));

        when(routing.endpointFor(REPOSITORY_SCOPE_SPEC, RESOURCE_URI)).thenReturn(REPOSITORY_SCOPE_ENDPOINT_URI);
        when(routing.endpointFor(RESOURCE_SCOPE_SPEC, RESOURCE_URI)).thenReturn(RESOURCE_SCOPE_ENDPOINT_URI);
        when(routing.endpointFor(UNREGISTERED_SERVICE_SPEC, RESOURCE_URI)).thenReturn(UNREGISTERED_ENDPOINT_URI);
        when(routing.serviceDocFor(RESOURCE_URI)).thenReturn(URI.create(""));

        toTest.setRouting(routing);
        toTest.setExtensionBinding(binding);
        toTest.setServiceRegistry(SERVICE_REGISTRY);
    }

    // Verifies that resource-scoped services are a function of the resource, and those that aren't don't have it.
    @Test
    public void functionOfTest() throws Exception {
        when(binding.getExtensionsFor(theResource)).thenReturn(
                Arrays.asList(EXPOSING_EXTENSION_RESOURCE_SCOPED, EXPOSING_EXTENSION_UNREGISTERED,
                        EXPOSING_EXTENSION_REPOSITORY_SCOPED));

        final Model doc = parse(toTest.getServiceDocumentFor(theResource, "text/turtle"));

        final String sparql = "CONSTRUCT { ?service <test:/rel> ?serviceInstance . } WHERE { " +
                String.format("?serviceInstance <%s> <%s> . ", PROP_IS_FUNCTION_OF, RESOURCE_URI) +
                String.format("?serviceInstance <%s> ?service . ", PROP_IS_SERVICE_INSTANCE_OF) +
                "}";

        final Set<URI> matchingServices = subjectsOf(query(sparql, doc));

        assertEquals(2, matchingServices.size());
        assertTrue(matchingServices.containsAll(Arrays.asList(RESOURCE_SCOPE_SERVICE_URI, UNREGISTERED_SERVICE_URI)));

    }

    // Verify that the service document points back to the resource.
    @Test
    public void serviceDocumentForTest() throws Exception {
        final String SERVICE_DOC_URI = "test:/doc";
        when(routing.serviceDocFor(RESOURCE_URI)).thenReturn(URI.create(SERVICE_DOC_URI));

        final Model doc = parse(toTest.getServiceDocumentFor(theResource, "text/turtle"));

        assertTrue(doc.contains(
                doc.getResource(SERVICE_DOC_URI),
                doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR),
                doc.getResource(theResource.uri().toString())));
    }

    @Test
    public void serviceDocumentRelativeURITest() throws Exception {
        final String SERVICE_DOC_URI = "test:/doc";
        when(routing.serviceDocFor(RESOURCE_URI)).thenReturn(URI.create(SERVICE_DOC_URI));
        toTest.setRelativeURIs(true);

        final Model doc = parse(toTest.getServiceDocumentFor(theResource, "text/turtle"));

        assertTrue(doc.contains(
                null,
                doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR),
                doc.getResource(theResource.uri().toString())));

        final List<String> subjects = doc.listResourcesWithProperty(doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR))
                .mapWith(
                        Resource::getURI).toList();
        assertEquals(1, subjects.size());
        assertNotEquals(SERVICE_DOC_URI, subjects.get(0));
    }

    // Verify that each exposing extension results a service instance,
    // and that it's 'isServiceInstanceOf' the right service.
    @Test
    public void serviceInstanceOfTest() {
        when(binding.getExtensionsFor(theResource)).thenReturn(
                Arrays.asList(EXPOSING_EXTENSION_RESOURCE_SCOPED, EXPOSING_EXTENSION_UNREGISTERED,
                        EXPOSING_EXTENSION_REPOSITORY_SCOPED, INTERCEPTING_EXTENSION));

        final Model doc = parse(toTest.getServiceDocumentFor(theResource, "text/turtle"));

        final String sparql = "CONSTRUCT { ?service <test:/isPresentFromInstance> ?serviceInstance . } WHERE { " +
                String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                String.format("?serviceInstance <%s> ?service . ", PROP_IS_SERVICE_INSTANCE_OF) +
                "}";

        final Set<URI> servicesFromInstances = subjectsOf(query(sparql, doc));

        assertEquals(3, servicesFromInstances.size());

        assertTrue(servicesFromInstances.containsAll(Arrays.asList(REPOSITORY_SCOPE_SERVICE_URI,
                RESOURCE_SCOPE_SERVICE_URI, UNREGISTERED_SERVICE_URI)));

    }

    // Verify that service instances point to their URI.
    @Test
    public void endpointURITest() {
        when(binding.getExtensionsFor(theResource)).thenReturn(
                Arrays.asList(EXPOSING_EXTENSION_RESOURCE_SCOPED, EXPOSING_EXTENSION_UNREGISTERED,
                        EXPOSING_EXTENSION_REPOSITORY_SCOPED, INTERCEPTING_EXTENSION));

        final Model doc = parse(toTest.getServiceDocumentFor(theResource, "text/turtle"));

        final String sparql = "CONSTRUCT { ?endpoint <test:/endpointFor> ?serviceInstance . } WHERE { " +
                String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                String.format("?serviceInstance <%s> ?endpoint . ", PROP_HAS_ENDPOINT) +
                "}";

        final Set<URI> canonicalEndpoints = subjectsOf(query(sparql, doc));

        assertEquals(3, canonicalEndpoints.size());

        assertTrue(canonicalEndpoints.containsAll(Arrays.asList(REPOSITORY_SCOPE_ENDPOINT_URI,
                RESOURCE_SCOPE_ENDPOINT_URI, UNREGISTERED_ENDPOINT_URI)));
    }

    // Verify that a specific serialization can be produced by specifying content type
    @Test
    public void contentTypeTest() throws Exception {
        final WebResource serviceDoc = toTest.getServiceDocumentFor(theResource, "application/rdf+xml");

        final Model doc = ModelFactory.createDefaultModel().read(serviceDoc.representation(), Lang.RDFXML.getName());

        assertTrue(doc.contains(
                null,
                doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR),
                doc.getResource(theResource.uri().toString())));

    }

    // Verify that nothing fails if all registries are empty.
    @Test
    public void emptyRegistriesTest() {

        when(binding.getExtensionsFor(theResource)).thenReturn(new ArrayList<>());

        toTest.getServiceDocumentFor(theResource, "text/turtle");
    }
}
