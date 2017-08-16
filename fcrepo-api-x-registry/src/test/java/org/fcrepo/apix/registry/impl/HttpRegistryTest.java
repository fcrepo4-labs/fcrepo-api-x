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

package org.fcrepo.apix.registry.impl;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;

/**
 * Tests for HttpRegistry implementation
 *
 * @author apb@jhu.edu
 */
public class HttpRegistryTest {

    HttpEntity entity;

    StatusLine status;

    CloseableHttpResponse response;

    // Verify that correct InputStream is produced
    @Test
    public void representationTest() throws Exception {
        final HttpRegistry toTest = new HttpRegistry();

        final URI uri = URI.create("http://test");
        final String content = "CONTENT";
        try (InputStream stream = IOUtils.toInputStream(content, "UTF-8")) {

            toTest.setHttpClient(mockClient(uri, stream, "text/turte", SC_OK));

            assertEquals(content, IOUtils.toString(toTest.get(uri).representation(), "UTF-8"));
        }
    }

    // Verify that Content-Type is conveyed
    @Test
    public void mimeTypeTest() throws Exception {
        final HttpRegistry toTest = new HttpRegistry();

        final URI uri = URI.create("http://test");
        final String CONTENT_TYPE = "test/type";

        toTest.setHttpClient(mockClient(uri, null, CONTENT_TYPE, SC_OK));

        assertEquals(CONTENT_TYPE, toTest.get(uri).contentType());
    }

    // Verify that the document URI is conveyed
    @Test
    public void uriTest() throws Exception {
        final HttpRegistry toTest = new HttpRegistry();

        final URI uri = URI.create("http://test");

        toTest.setHttpClient(mockClient(uri, null, null, SC_OK));

        assertEquals(uri, toTest.get(uri).uri());
    }

    // Verify that closing the WebResource closes the underlying http response.
    @Test
    public void closeResponseTest() throws Exception {
        final HttpRegistry toTest = new HttpRegistry();
        final URI uri = URI.create("http://test");

        toTest.setHttpClient(mockClient(uri, null, null, SC_OK));

        toTest.get(uri).close();

        verify(response, times(1)).close();
    }

    // Verify that a non-200 response code results in an exception,
    // and the underlying response is closed
    @Test
    public void badResponseCodeTest() throws Exception {
        final HttpRegistry toTest = new HttpRegistry();
        final URI uri = URI.create("http://test");

        toTest.setHttpClient(mockClient(uri, null, null, HttpStatus.SC_NOT_FOUND));

        try {
            toTest.get(uri);
            fail("should have thrown an exception");
        } catch (final Exception e) {
            verify(response, times(1)).close();
        }

    }

    private CloseableHttpClient mockClient(final URI uri, final InputStream content, final String contentType,
            final int statusCode)
            throws Exception {
        entity = mock(HttpEntity.class);
        when(entity.getContent()).thenReturn(content);

        status = mock(StatusLine.class);
        when(status.getStatusCode()).thenReturn(statusCode);

        final Header contentTypeHeader = mock(Header.class);
        when(contentTypeHeader.getValue()).thenReturn(contentType);

        response = mock(CloseableHttpResponse.class);
        when(response.getEntity()).thenReturn(entity);
        when(response.getStatusLine()).thenReturn(status);
        when(response.getFirstHeader(HttpHeaders.CONTENT_TYPE)).thenReturn(contentTypeHeader);

        final CloseableHttpClient client = mock(CloseableHttpClient.class);
        when(client.execute(argThat(isGetRequestTo(uri)))).thenReturn(response);

        return client;
    }

    Matcher<HttpUriRequest> isGetRequestTo(final URI uri) {
        return new BaseMatcher<HttpUriRequest>() {

            @Override
            public boolean matches(final Object item) {
                return item instanceof HttpGet && ((HttpGet) item).getURI().equals(uri);
            }

            @Override
            public void describeTo(final Description description) {
                /* nothing */
            }
        };
    }
}
