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
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * Ontology registry tests.
 *
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class LookupOntologyRegistryTest {

    @Mock
    Registry delegate;

    // After PUTting an ontology in the registry, verify that we can retrieve it by
    // its location URI and ontology IRI.
    @Test
    public void putAndLookupByIRITest() {
        final LookupOntologyRegistry toTest = new LookupOntologyRegistry();

        final URI ontologyLocationURI = URI.create("http://example.org/location");
        final URI ontologyIRI = URI.create("http://example.org/test#Ontology");

        final WebResource ontologyToPersist = WebResource.of(this.getClass().getResourceAsStream(
                "/ontologyWithIRI.ttl"), "text/turtle", ontologyLocationURI, null);

        when(delegate.put(ontologyToPersist)).thenReturn(ontologyLocationURI);
        when(delegate.get(ontologyLocationURI)).thenReturn(ontologyToPersist);
        when(delegate.list()).thenReturn(new ArrayList<>());

        toTest.setRegistryDelegate(delegate);
        toTest.init();

        toTest.put(ontologyToPersist);

        assertEquals(ontologyToPersist, toTest.get(ontologyIRI));
        assertEquals(toTest.get(ontologyIRI), toTest.get(ontologyLocationURI));
    }

    // Verify that persisting an ontology whilst explicitly providing an ontology IRI works
    @Test
    public void explicitIRITest() {
        final LookupOntologyRegistry toTest = new LookupOntologyRegistry();

        final URI ontologyLocationURI = URI.create("http://example.org/location");
        final URI ontologyIRI = URI.create("http://example.org/test#OntologyNoIRI");

        final WebResource ontologyToPersist = new ReadableResource(this.getClass().getResourceAsStream(
                "/ontologyWithoutIRI.ttl"), "text/turtle", ontologyLocationURI, null);

        final ArrayList<URI> entries = new ArrayList<>();

        when(delegate.put(any(WebResource.class))).then(new Answer<URI>() {

            @Override
            public URI answer(final InvocationOnMock invocation) throws Throwable {
                final WebResource submitted = ((WebResource) invocation.getArguments()[0]);
                when(delegate.get(ontologyLocationURI)).thenReturn(submitted);
                entries.add(ontologyLocationURI);
                return ontologyLocationURI;
            }
        });
        when(delegate.list()).thenReturn(entries);

        toTest.setRegistryDelegate(delegate);
        toTest.init();

        toTest.put(ontologyToPersist, ontologyIRI);

        assertEquals(toTest.get(ontologyIRI), toTest.get(ontologyLocationURI));
    }

    // Verifies that init() populates the registry, and we can lookup by IRI
    @Test
    public void initializePopulateTest() {
        final LookupOntologyRegistry toTest = new LookupOntologyRegistry();

        final URI ontologyLocationURI = URI.create("http://example.org/location");
        final URI ontologyIRI = URI.create("http://example.org/test#Ontology");

        final WebResource ontologyToPersist = WebResource.of(this.getClass().getResourceAsStream(
                "/ontologyWithIRI.ttl"), "text/turtle", ontologyLocationURI, null);

        when(delegate.put(any(WebResource.class))).thenReturn(ontologyLocationURI);
        when(delegate.get(ontologyLocationURI)).thenReturn(ontologyToPersist);
        when(delegate.list()).thenReturn(Arrays.asList(ontologyLocationURI));

        toTest.setRegistryDelegate(delegate);
        toTest.init();

        assertEquals(ontologyToPersist, toTest.get(ontologyIRI));
        assertEquals(toTest.get(ontologyIRI), toTest.get(ontologyLocationURI));
    }

    // Verifies that init() retries until it succeeds.
    @Test
    public void initRetryTest() throws Exception {

        final LookupOntologyRegistry toTest = new LookupOntologyRegistry();

        // init() will fail will a NullPointerException until we populate it with a registry;
        final Future<?> initializing = Executors.newSingleThreadExecutor().submit(new Runnable() {

            @Override
            public void run() {
                toTest.init();
            }
        });

        assertFalse(initializing.isDone());

        when(delegate.list()).thenReturn(new ArrayList<>());

        toTest.setRegistryDelegate(delegate);

        initializing.get(2, TimeUnit.SECONDS);
    }

    private class ReadableResource implements WebResource {

        private final byte[] content;

        private final String contentType;

        private final URI uri;

        private final Long length;

        public ReadableResource(final InputStream stream, final String contentType, final URI uri,
                final Long length) {
            try {
                content = IOUtils.toByteArray(stream);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

            this.contentType = contentType;
            this.uri = uri;
            this.length = length;

        }

        @Override
        public void close() throws Exception {
            // nothing
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public URI uri() {
            return uri;
        }

        @Override
        public Long length() {
            return length;
        }

        @Override
        public InputStream representation() {
            return new ByteArrayInputStream(content);
        }
    }
}
