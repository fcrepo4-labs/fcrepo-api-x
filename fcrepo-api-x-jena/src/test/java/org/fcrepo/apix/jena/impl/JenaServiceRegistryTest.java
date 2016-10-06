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

import static org.fcrepo.apix.jena.Util.rdfResource;
import static org.fcrepo.apix.jena.Util.triple;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_LDP_SERVICE_INSTANCE_REGISTRY;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_SERVICE_INSTANCE_REGISTRY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.Service;
import org.fcrepo.apix.model.ServiceInstance;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.ServiceInstanceRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Service registry tests.
 *
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class JenaServiceRegistryTest {

    JenaServiceRegistry toTest;

    @Mock
    public Registry delegate;

    @Before
    public void setUp() {
        toTest = new JenaServiceRegistry();
        toTest.setRegistryDelegate(delegate);
    }

    // Verifies that if a canonical URI is specified, it's returned.
    @Test
    public void canonicalPresenceTest() {
        final String CANONICAL = "http://example.org/canonical/service";
        final String SERVICE = "http://example.org/service#uri";
        final URI SERVICE_URI = URI.create(SERVICE);
        when(delegate.get(SERVICE_URI)).thenReturn(
                rdfResource(SERVICE, triple(SERVICE, PROP_CANONICAL, CANONICAL)));

        assertEquals(CANONICAL, toTest.getService(SERVICE_URI).canonicalURI().toString());
    }

    // If there's no canonical, then the service URI is implicitly canonical
    @Test
    public void noCanonicalPresentTest() {
        final String CANONICAL = "http://example.org/canonical/service";
        final String SERVICE = "http://example.org/service#uri";
        final URI SERVICE_URI = URI.create(SERVICE);
        when(delegate.get(SERVICE_URI)).thenReturn(
                rdfResource(SERVICE, triple(SERVICE, "http://example.org/NOT_CANONICAL", CANONICAL)));

        assertEquals(SERVICE, toTest.getService(SERVICE_URI).canonicalURI().toString());
    }

    @Test
    public void canonicalLookupTest() {
        final String CANONICAL = "http://example.org/canonical/service";
        final String SERVICE = "http://repository.local/service#uri";
        final URI SERVICE_URI = URI.create(SERVICE);
        when(delegate.get(SERVICE_URI)).thenReturn(
                rdfResource(SERVICE, triple(SERVICE, RDF_TYPE, CLASS_SERVICE) +
                        triple(SERVICE, PROP_CANONICAL, CANONICAL)));
        when(delegate.list()).thenReturn(Arrays.asList(SERVICE_URI));

        toTest.update();

        final Service svc = toTest.getService(URI.create(CANONICAL));

        assertNotNull(svc);
        assertEquals(svc.canonicalURI().toString(), CANONICAL);
        assertEquals(svc.uri(), SERVICE_URI);
    }

    // Verifies that 'instancesOf' returns services instances for LdpServiceInstanceRegistries
    @Test
    public void instancesOfTest() {
        final String SERVICE_INSTANCE_1 = "http://example.org/service#instance1";
        final String SERVICE_ENDPOINT_1 = "http://example.org/endpoints/1";
        final String SERVICE_INSTANCE_2 = "http://example.org/service#instance2";
        final String SERVICE_ENDPOINT_2 = "http://example.org/endpoints/2";
        final String SERVICE = "http://example.org/service#uri";
        final URI SERVICE_URI = URI.create(SERVICE);

        final WebResource resource = rdfResource(SERVICE,
                triple(SERVICE, RDF_TYPE, CLASS_LDP_SERVICE_INSTANCE_REGISTRY) +
                        triple(SERVICE, PROP_HAS_SERVICE_INSTANCE_REGISTRY, SERVICE) +
                        triple(SERVICE, PROP_HAS_SERVICE_INSTANCE, SERVICE_INSTANCE_1) +
                        triple(SERVICE, PROP_HAS_SERVICE_INSTANCE, SERVICE_INSTANCE_2) +
                        triple(SERVICE_INSTANCE_1, PROP_HAS_ENDPOINT, SERVICE_ENDPOINT_1) +
                        triple(SERVICE_INSTANCE_2, PROP_HAS_ENDPOINT, SERVICE_ENDPOINT_2));

        // Emulate what happens if we do GETS with fragments
        when(delegate.get(SERVICE_URI)).thenReturn(resource);
        when(delegate.get(URI.create(SERVICE_INSTANCE_1))).thenReturn(resource);
        when(delegate.get(URI.create(SERVICE_INSTANCE_2))).thenReturn(resource);

        final ServiceInstanceRegistry instanceRegistry = toTest.instancesOf(toTest.getService(SERVICE_URI));
        assertEquals(2, instanceRegistry.instances().size());
        instanceRegistry.instances().stream().forEach(si -> assertEquals(1, si.endpoints().size()));
        final List<URI> instanceURIs = instanceRegistry.instances().stream().map(ServiceInstance::endpoints).flatMap(
                Collection::stream).collect(
                        Collectors.toList());
        assertTrue(instanceURIs.containsAll(Arrays.asList(URI.create(SERVICE_ENDPOINT_1), URI.create(
                SERVICE_ENDPOINT_2))));
    }
}
