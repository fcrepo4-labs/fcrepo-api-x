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

import static org.fcrepo.apix.jena.Util.ltriple;
import static org.fcrepo.apix.jena.Util.rdfResource;
import static org.fcrepo.apix.jena.Util.triple;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_BINDS_TO;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_CONSUMES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE_AT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.components.Registry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Test the Jena Extension Registry Impl.
 *
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class JenaExtensionRegistryTest {

    static final String EXTENSION = "http://example.org/extension";

    static final URI EXTENSION_URI = URI.create(EXTENSION);

    static final String SERVICE_1_URI = "test:/service1";

    static final String SERVICE_2_URI = "test:/service2";

    @Mock
    Registry registryDelegate;

    @Mock
    Service service1;

    @Mock
    Service service2;

    JenaExtensionRegistry toTest;

    @Before
    public void setUp() {
        toTest = new JenaExtensionRegistry();
        toTest.setRegistryDelegate(registryDelegate);

        when(service1.uri()).thenReturn(URI.create(SERVICE_1_URI));
        when(service2.uri()).thenReturn(URI.create(SERVICE_2_URI));

    }

    @Test
    public void bindingClassTest() {

        final String BINDING_CLASS = "test:/binding";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION, triple(EXTENSION,
                PROP_BINDS_TO,
                BINDING_CLASS)));

        assertEquals(URI.create(BINDING_CLASS), toTest.getExtension(EXTENSION_URI).bindingClass());
    }

    @Test
    public void isExposingTest() {

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                triple(EXTENSION, PROP_EXPOSES_SERVICE, SERVICE_1_URI)));

        final Extension extension = toTest.getExtension(EXTENSION_URI);

        assertTrue(extension.isExposing());
        assertFalse(extension.isIntercepting());
    }

    @Test
    public void exposedConsumedServiceTest() {

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                triple(EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_1_URI) +
                        triple(EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_2_URI)));

        final Extension extension = toTest.getExtension(EXTENSION_URI);

        assertEquals(2, extension.exposed().consumed().size());

        assertTrue(extension.exposed().consumed().containsAll(
                Arrays.asList(service1.uri(), service2.uri())));
    }

    @Test
    public void exposedServiceTest() {
        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                triple(EXTENSION, PROP_EXPOSES_SERVICE, SERVICE_1_URI)));

        final Extension extension = toTest.getExtension(EXTENSION_URI);

        assertEquals(service1.uri(), extension.exposed().exposedService());
    }

    @Test
    public void exposedResourceScopeTest() {
        final String EXPOSED_AT = "svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(Scope.RESOURCE, toTest.getExtension(EXTENSION_URI).exposed().scope());
    }

    @Test
    public void exposedRepositoryScopeTest() {
        final String EXPOSED_AT = "/svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(Scope.REPOSITORY, toTest.getExtension(EXTENSION_URI).exposed().scope());
    }

    @Test
    public void exposedExternalScopeTest() {
        final String EXPOSED_AT = "http://127.0.0.1/svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(Scope.EXTERNAL, toTest.getExtension(EXTENSION_URI).exposed().scope());
    }

    @Test
    public void isInterceptingTest() {

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                triple(EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_1_URI)));

        final Extension extension = toTest.getExtension(EXTENSION_URI);

        assertFalse(extension.isExposing());
        assertTrue(extension.isIntercepting());
    }

    @Test
    public void exposedAtURITest() {
        final String EXPOSED_AT = "http://127.0.0.1/svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(EXPOSED_AT, toTest.getExtension(EXTENSION_URI).exposed().exposedAt().toString());
    }

    @Test
    public void exposedAtAbsoluteTest() {
        final String EXPOSED_AT = "/svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(EXPOSED_AT, toTest.getExtension(EXTENSION_URI).exposed().exposedAt().toString());
        assertEquals(EXPOSED_AT, toTest.getExtension(EXTENSION_URI).exposed().exposedAt().getPath());
    }

    @Test
    public void exposedAtRelativeTest() {
        final String EXPOSED_AT = "svc:/123()";

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                ltriple(EXTENSION, PROP_EXPOSES_SERVICE_AT, EXPOSED_AT)));

        assertEquals(EXPOSED_AT, toTest.getExtension(EXTENSION_URI).exposed().exposedAt().toString());
        assertEquals(EXPOSED_AT, toTest.getExtension(EXTENSION_URI).exposed().exposedAt().getPath());
    }

    @Test
    public void interceptingConsumedServiceTest() {

        when(registryDelegate.get(EXTENSION_URI)).thenReturn(rdfResource(EXTENSION,
                triple(EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_1_URI) +
                        triple(EXTENSION, PROP_CONSUMES_SERVICE, SERVICE_2_URI)));

        final Extension extension = toTest.getExtension(EXTENSION_URI);

        assertEquals(2, extension.exposed().consumed().size());

        assertTrue(extension.intercepted().consumed()
                .containsAll(
                        Arrays.asList(service1.uri(), service2.uri())));
    }

}
