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

package org.fcrepo.apix.loader.impl;

import static org.fcrepo.apix.jena.Util.ltriple;
import static org.fcrepo.apix.jena.Util.rdfResource;
import static org.fcrepo.apix.jena.Util.triple;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Apix.CLASS_EXTENSION;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_CONSUMES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE_AT;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.ServiceExposureSpec;
import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ResourceNotFoundException;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class LoaderServiceTest {

    final String SERVICE_ENDPOINT = "http://example.org/service/endpoint";

    final URI SERVICE_ENDPOINT_URI = URI.create(SERVICE_ENDPOINT);

    final String SERVICE_CANONICAL = "http://example.org/service/canonical";

    final URI SERVICE_CANONICAL_URI = URI.create(SERVICE_CANONICAL);

    final String PERSISTED_EXTENSION = "test:persisted_extension";

    final URI PERSISTED_EXTENSION_URI = URI.create(PERSISTED_EXTENSION);

    final URI PERSISTED_SERVICE_URI = URI.create("test:persisted_service");

    final URI PERSISTED_SERVICE_INSTANCE_URI = URI.create("test:persistedServiceInstance");

    @Mock
    ServiceRegistry serviceRegistry;

    @Mock
    ExtensionRegistry extensionRegistry;

    @Mock
    Registry generalRegistry;

    @Mock
    Service service;

    @Mock
    ServiceInstanceRegistry serviceInstanceRegistry;

    LoaderService toTest = new LoaderService();

    @Before
    public void setUp() {
        toTest.setExtensionRegistry(extensionRegistry);
        toTest.setServiceRegistry(serviceRegistry);
        toTest.setGeneralRegistry(generalRegistry);

        when(extensionRegistry.put(any(WebResource.class), any(Boolean.class))).thenReturn(PERSISTED_EXTENSION_URI);

        when(serviceRegistry.getService(eq(PERSISTED_SERVICE_URI))).thenReturn(service);
        when(serviceRegistry.createInstanceRegistry(eq(service))).thenReturn(serviceInstanceRegistry);
        when(serviceInstanceRegistry.addEndpoint(SERVICE_ENDPOINT_URI)).thenReturn(PERSISTED_SERVICE_INSTANCE_URI);

        // When a service is deposited, make it available via the general registry
        final List<WebResource> depositedServices = new ArrayList<>();
        when(serviceRegistry.put(any(WebResource.class))).thenAnswer(i -> {
            depositedServices.add(i.getArgumentAt(0, WebResource.class));

            return PERSISTED_SERVICE_URI;
        });
        when(generalRegistry.get(eq(PERSISTED_SERVICE_URI))).thenAnswer(i -> depositedServices.get(0));

    }

    // Deposit an extension with a referenced service that is not defined in the extension doc, nor exists in the
    // registry.
    @Test
    public void referencedServiceNotExistsTest() {

        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                triple(PERSISTED_EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // No services in registry
        when(serviceRegistry.contains(any(URI.class))).thenReturn(false);

        // Our service doesn't pre-exist in the service registry, so we want it "not found"
        when(serviceRegistry.instancesOf(any(Service.class))).thenAnswer(i -> {
            throw new ResourceNotFoundException("not found");
        });

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        triple(SERVICE_ENDPOINT, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);
        verify(serviceRegistry).createInstanceRegistry(eq(service));
        verify(serviceRegistry).put(any(WebResource.class));
        verify(extensionRegistry).put(any(WebResource.class), eq(false));
        assertEquals(PERSISTED_EXTENSION_URI, loadResult);
    }

    // Service exists in registry, but instance registry is not
    @Test
    public void referencedServiceExistsTest() {
        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                triple(PERSISTED_EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // Yes, services in registry
        when(serviceRegistry.contains(any(URI.class))).thenReturn(true);
        when(serviceRegistry.getService(eq(SERVICE_CANONICAL_URI))).thenReturn(service);

        // No, service instance registry does not exist
        when(serviceRegistry.instancesOf(any(Service.class))).thenAnswer(i -> {
            throw new ResourceNotFoundException("not found");
        });

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        triple(SERVICE_ENDPOINT, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // Make sure NO services added to registry
        verify(serviceRegistry, times(0)).put(any(WebResource.class));

        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);
        verify(serviceRegistry).createInstanceRegistry(eq(service));
        verify(extensionRegistry).put(any(WebResource.class), eq(false));
        assertEquals(PERSISTED_EXTENSION_URI, loadResult);
    }

    // Service instance and instance registry exists
    @Test
    public void referencedServiceAndInstanceRegistryExistsTest() {
        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                triple(PERSISTED_EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // Yes, services in registry
        when(serviceRegistry.contains(any(URI.class))).thenReturn(true);
        when(serviceRegistry.getService(eq(SERVICE_CANONICAL_URI))).thenReturn(service);

        // Yes, service instance registry exists
        when(serviceRegistry.instancesOf(eq(service))).thenAnswer(i -> serviceInstanceRegistry);

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        triple(SERVICE_ENDPOINT, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // Make sure NO services added to registry
        verify(serviceRegistry, times(0)).put(any(WebResource.class));

        // Make sure NO instance registries created;
        verify(serviceRegistry, times(0)).createInstanceRegistry(any(Service.class));

        verify(serviceRegistry).instancesOf(eq(service));
        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);
        verify(extensionRegistry).put(any(WebResource.class), eq(false));
        assertEquals(PERSISTED_EXTENSION_URI, loadResult);
    }

    @Test
    public void newDefinedServiceTest() {

        final String LOCALLY_DEFINED_SERVICE = "http://example.org/localService";
        final URI LOCALLY_DEFINED_SERVICE_URI = URI.create(LOCALLY_DEFINED_SERVICE);

        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                triple(PERSISTED_EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL) +
                                triple(LOCALLY_DEFINED_SERVICE, RDF_TYPE, CLASS_SERVICE) +
                                triple(LOCALLY_DEFINED_SERVICE, PROP_CANONICAL, SERVICE_CANONICAL)));

        // No services in registry
        when(serviceRegistry.contains(any(URI.class))).thenReturn(false);

        // Our service doesn't pre-exist in the service registry, so we want it "not found"
        when(serviceRegistry.instancesOf(any(Service.class))).thenAnswer(i -> {
            throw new ResourceNotFoundException("not found");
        });

        // Our service should be registered by its local (in extension document) URI
        when(serviceRegistry.getService(LOCALLY_DEFINED_SERVICE_URI)).thenReturn(service);

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        triple(SERVICE_ENDPOINT, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        verify(serviceRegistry, times(1)).put(any(WebResource.class));
        verify(serviceRegistry).createInstanceRegistry(eq(service));
        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);
        verify(extensionRegistry).put(any(WebResource.class), eq(false));

        assertEquals(PERSISTED_EXTENSION_URI, loadResult);
    }

    @Test
    public void existingDefinedServiceTest() {
        final String LOCALLY_DEFINED_SERVICE = "http://example.org/localService";
        final URI LOCALLY_DEFINED_SERVICE_URI = URI.create(LOCALLY_DEFINED_SERVICE);

        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                triple(PERSISTED_EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL) +
                                triple(LOCALLY_DEFINED_SERVICE, RDF_TYPE, CLASS_SERVICE) +
                                triple(LOCALLY_DEFINED_SERVICE, PROP_CANONICAL, SERVICE_CANONICAL)));

        // Yes, service in registry
        when(serviceRegistry.contains(eq(SERVICE_CANONICAL_URI))).thenReturn(true);
        when(serviceRegistry.getService(eq(SERVICE_CANONICAL_URI))).thenReturn(service);

        // Our service doesn't pre-exist in the service registry, so we want it "not found"
        when(serviceRegistry.instancesOf(any(Service.class))).thenAnswer(i -> {
            throw new ResourceNotFoundException("not found");
        });

        // Our service should be registered by its local (in extension document) URI
        when(serviceRegistry.getService(LOCALLY_DEFINED_SERVICE_URI)).thenReturn(service);

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        triple(SERVICE_ENDPOINT, PROP_CONSUMES_SERVICE, SERVICE_CANONICAL)));

        // Service already exists in registry, so do not add or link
        verify(serviceRegistry, times(0)).put(any(WebResource.class));
        verify(serviceRegistry, times(0)).register(any(URI.class));

        verify(serviceRegistry).createInstanceRegistry(eq(service));
        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);

        verify(extensionRegistry).put(any(WebResource.class), eq(false));

        assertEquals(PERSISTED_EXTENSION_URI, loadResult);
    }

    @Test
    public void duplicateExtensionTest() {

        final URI EXPOSED_AT = URI.create("exposed()");

        final ArgumentCaptor<WebResource> arg = ArgumentCaptor.forClass(WebResource.class);

        when(generalRegistry.get(eq(PERSISTED_EXTENSION_URI))).thenReturn(
                rdfResource(PERSISTED_EXTENSION,
                        triple(PERSISTED_EXTENSION, RDF_TYPE, CLASS_EXTENSION) +
                                ltriple(SERVICE_ENDPOINT, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT.toString()) +
                                triple(PERSISTED_EXTENSION, PROP_EXPOSES_SERVICE, SERVICE_CANONICAL)));

        // No services in registry
        when(serviceRegistry.contains(any(URI.class))).thenReturn(false);

        // Our service doesn't pre-exist in the service registry, so we want it "not found"
        when(serviceRegistry.instancesOf(any(Service.class))).thenAnswer(i -> {
            throw new ResourceNotFoundException("not found");
        });

        final Extension existing = mock(Extension.class);
        final ServiceExposureSpec spec = mock(ServiceExposureSpec.class);
        when(spec.exposedAt()).thenReturn(EXPOSED_AT);
        when(existing.uri()).thenReturn(PERSISTED_EXTENSION_URI);
        when(existing.isExposing()).thenReturn(true);
        when(existing.exposed()).thenReturn(spec);

        when(extensionRegistry.getExtensions()).thenReturn(Arrays.asList(existing));

        final URI loadResult = toTest.load(rdfResource(SERVICE_ENDPOINT,
                triple(SERVICE_ENDPOINT, RDF_TYPE, CLASS_EXTENSION) +
                        ltriple(SERVICE_ENDPOINT, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT.toString()) +
                        triple(SERVICE_ENDPOINT, PROP_EXPOSES_SERVICE, SERVICE_CANONICAL)));

        verify(serviceInstanceRegistry).addEndpoint(SERVICE_ENDPOINT_URI);
        verify(serviceRegistry).createInstanceRegistry(eq(service));
        verify(serviceRegistry).put(any(WebResource.class));
        assertEquals(PERSISTED_EXTENSION_URI, loadResult);

        verify(extensionRegistry).put(arg.capture(), eq(false));
        assertEquals(PERSISTED_EXTENSION_URI, arg.getValue().uri());
    }
}
