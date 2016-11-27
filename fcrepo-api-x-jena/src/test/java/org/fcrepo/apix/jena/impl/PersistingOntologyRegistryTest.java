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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.apix.model.components.OntologyService;
import org.fcrepo.apix.model.components.Registry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class PersistingOntologyRegistryTest {

    @Mock
    Registry world;

    @Mock
    OntologyRegistry ontologyRegistry;

    @Mock
    ExtensionRegistry extensionRegistry;

    @Mock
    OntologyService ontologyService;

    @Mock
    WebResource resource;

    final URI ontologyIRI = URI.create("http://example.org/ontologyIRI");

    final URI extensionURI = URI.create("test:extension");

    PersistingOntologyRegistry toTest = new PersistingOntologyRegistry();

    @Before
    public void setUp() {
        toTest.setDoPersist(true);
        toTest.setGeneralRegistry(world);
        toTest.setOntologyRegistry(ontologyRegistry);
        toTest.setOntologyService(ontologyService);
        toTest.setExtensionRegistry(extensionRegistry);

        when(world.get(ontologyIRI)).thenReturn(resource);
        when(resource.uri()).thenReturn(URI.create("http://example.org/resource"));
        when(ontologyRegistry.get(ontologyIRI)).thenReturn(resource);
        when(extensionRegistry.hasInDomain(extensionURI)).thenReturn(true);
        when(extensionRegistry.get(extensionURI)).thenReturn(resource);
    }

    @Test
    public void ontologyNotInRegistryTest() {
        when(ontologyRegistry.contains(ontologyIRI)).thenReturn(false);

        assertEquals(resource, toTest.get(ontologyIRI));
        verify(world).get(ontologyIRI);
        verify(ontologyRegistry).put(any(WebResource.class), eq(ontologyIRI));
    }

    @Test
    public void ontologyNotInRegistryButNoPeristTest() {
        toTest.setDoPersist(false);

        when(ontologyRegistry.contains(ontologyIRI)).thenReturn(false);

        assertEquals(resource, toTest.get(ontologyIRI));
        verifyZeroInteractions(world);
        verify(ontologyRegistry, times(0)).put(any(WebResource.class), any(URI.class));
    }

    @Test
    public void ontologyInRegistryTest() {
        when(ontologyRegistry.contains(ontologyIRI)).thenReturn(true);

        assertEquals(resource, toTest.get(ontologyIRI));
        verifyZeroInteractions(world);
        verify(ontologyRegistry, times(0)).put(any(WebResource.class), any(URI.class));
    }

    @Test
    public void updateExtensionInDomainTest() {
        toTest.update(extensionURI);

        verify(ontologyService).parseOntology(eq(resource));
    }

    @Test
    public void updateExtensionInDomainNotPersistingTest() {
        toTest.setDoPersist(false);

        toTest.update(extensionURI);

        verifyZeroInteractions(extensionRegistry);
        verifyZeroInteractions(ontologyRegistry);
    }

    @Test
    public void updateExtensionNotInDomainTest() {
        when(extensionRegistry.hasInDomain(extensionURI)).thenReturn(false);

        toTest.update(extensionURI);

        verifyZeroInteractions(ontologyRegistry);
    }

    @Test
    public void updateTest() {
        when(extensionRegistry.list()).thenReturn(Arrays.asList(extensionURI, extensionURI));

        toTest.update();

        verify(ontologyService, times(2)).parseOntology(eq(resource));
    }

    @Test
    public void updateNoPersistTest() {
        toTest.setDoPersist(false);
        when(extensionRegistry.list()).thenReturn(Arrays.asList(extensionURI, extensionURI));

        toTest.update();

        verifyZeroInteractions(ontologyRegistry);
    }
}
