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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests the LDP registry impl
 *
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class LdpContainerRegistryTest {

    @Captor
    ArgumentCaptor<HttpUriRequest> requestCaptor;

    @Mock
    Registry registryDelegate;

    @Mock
    StatusLine entityStatus;

    @Mock
    StatusLine headStatus;

    @Mock
    CloseableHttpClient client;

    @Mock
    CloseableHttpResponse entityResponse;

    @Mock
    CloseableHttpResponse headResponse;

    @Mock
    Header header;

    @Before
    public void setUp() throws Exception {
        when(entityResponse.getStatusLine()).thenReturn(entityStatus);
        when(headResponse.getStatusLine()).thenReturn(headStatus);

        when(client.execute(isA(HttpEntityEnclosingRequestBase.class))).thenReturn(entityResponse);
        when(client.execute(isA(HttpHead.class))).thenReturn(headResponse);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createTest() throws Exception {
        final URI containerURI = URI.create("test:Container");
        final LdpContainerRegistry toTest = new LdpContainerRegistry();
        toTest.setContainer(containerURI);

        when(entityStatus.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);

        when(header.getValue()).thenReturn(containerURI.toString());
        when(entityResponse.getFirstHeader(HttpHeaders.LOCATION)).thenReturn(header);

        when(headStatus.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        toTest.setCreateContainer(true);
        toTest.setHttpClient(client);
        toTest.init();

        verify(client, times(1)).execute(requestCaptor.capture(), isA(ResponseHandler.class));

        final List<HttpUriRequest> requests = requestCaptor.getAllValues();

        assertEquals(HttpPut.class, requests.get(0).getClass());
        assertEquals(containerURI, requests.get(0).getURI());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createWithInitialContentTest() throws Exception {
        final URI containerURI = URI.create("test:Container");
        final LdpContainerRegistry toTest = new LdpContainerRegistry();
        toTest.setContainer(containerURI);

        when(entityStatus.getStatusCode()).thenReturn(HttpStatus.SC_CREATED);

        when(header.getValue()).thenReturn(containerURI.toString());
        when(entityResponse.getFirstHeader(HttpHeaders.LOCATION)).thenReturn(header);

        when(headStatus.getStatusCode()).thenReturn(HttpStatus.SC_NOT_FOUND);

        toTest.setCreateContainer(true);
        toTest.setHttpClient(client);
        toTest.setContainerContent(URI.create("classpath:/objects/service-registry.ttl"));
        toTest.init();

        verify(client, times(1)).execute(requestCaptor.capture(), isA(ResponseHandler.class));

        final HttpUriRequest request = requestCaptor.getValue();

        assertEquals(HttpPut.class, request.getClass());
        assertEquals(containerURI, request.getURI());

        final byte[] content = IOUtils.toByteArray(((HttpPut) request).getEntity().getContent());
        assertTrue(content.length > 0);

    }

    @Test
    public void dontCreateIfExistsTest() throws Exception {
        final URI containerURI = URI.create("test:Container");
        final LdpContainerRegistry toTest = new LdpContainerRegistry();
        toTest.setContainer(containerURI);

        when(headStatus.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        toTest.setCreateContainer(true);
        toTest.setHttpClient(client);
        toTest.init();

        verify(client, times(1)).execute(requestCaptor.capture());

        assertEquals(HttpHead.class, requestCaptor.getValue().getClass());
    }

    @Test
    public void noCreateTest() throws Exception {
        final LdpContainerRegistry toTest = new LdpContainerRegistry();

        toTest.setCreateContainer(false);
        toTest.setHttpClient(client);
        toTest.init();

        verify(client, never()).execute(isA(HttpHead.class));
        verify(client, never()).execute(isA(HttpPut.class));
    }

    @Test
    public void listLdpMembersTest() throws Exception {
        final URI member1 = URI.create("test:member1");
        final URI member2 = URI.create("test:member2");
        final URI containerURI = URI.create("test:Container");
        final LdpContainerRegistry toTest = new LdpContainerRegistry();
        toTest.setContainer(containerURI);
        toTest.setRegistryDelegate(registryDelegate);

        final String rdf = String.format("<%s> <%s> <%s> .\n<%s> <%s> <%s> .", containerURI.toString(), LDP_CONTAINS,
                member1.toString(), containerURI.toString(), LDP_CONTAINS, member2.toString());

        when(registryDelegate.get(containerURI)).thenReturn(WebResource.of(IOUtils.toInputStream(rdf, "UTF-8"),
                "application/n-triples"));

        final Collection<URI> members = toTest.list();
        final Collection<URI> expectedMembers = Arrays.asList(member1, member2);

        assertTrue(members.containsAll(expectedMembers));
        assertTrue(expectedMembers.containsAll(members));
    }

    @Test
    public void domainTest() {
        final LdpContainerRegistry toTest = new LdpContainerRegistry();
        final String CONTAINER = "http://example.org/container";
        toTest.setContainer(URI.create(CONTAINER));

        assertTrue(toTest.hasInDomain(URI.create(CONTAINER)));
        assertTrue(toTest.hasInDomain(URI.create(CONTAINER + "/other/path")));
        assertFalse(toTest.hasInDomain(URI.create("http://bad.example.org/not")));
    }
}
