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

import static org.junit.Assert.assertEquals;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;

/**
 * @author emetsger@jhu.edu
 */
public class RoutingStubTest {

    final RoutingStub toTest = new RoutingStub();

    @Before
    public void setUp() {
        toTest.setScheme("http");
        toTest.setHost("www.example.org");
        toTest.setPort(8080);
        toTest.setInterceptPath("/intercept/path");
        toTest.setFcrepoBaseURI(URI.create("http://www.example.org/fcrepo"));
    }

    /**
     * Don't include the port when it is the default port for the scheme. (Right now http is assumed, no https
     * support)
     */
    @Test
    public void testBaseUriPortPresence() {

        toTest.setPort(80);
        assertEquals("http://www.example.org/", toTest.exposedBaseURI().toString());

        toTest.setPort(8080);
        assertEquals("http://www.example.org:8080/", toTest.exposedBaseURI().toString());
    }

    @Test
    public void interceptPathTest() {

        final URI resourceURI = URI.create("http://www.example.org:8080/intercept/path/a/b/c");

        assertEquals("/a/b/c", toTest.resourcePath(resourceURI));
    }

    @Test
    public void fcrepoPathTest() {

        final URI resourceURI = URI.create("http://www.example.org/fcrepo/a/b/c");

        assertEquals("/a/b/c", toTest.resourcePath(resourceURI));
    }

    @Test
    public void interceptUriForTest() {

        final URI resourceURI = URI.create("http://www.example.org/fcrepo/a/b/c");
        final URI interceptURI = URI.create("http://www.example.org:8080/intercept/path/a/b/c");

        assertEquals(interceptURI, toTest.interceptUriFor(resourceURI));
    }

    @Test
    public void passThroughInterceptUriTest() {

        final URI interceptURI = URI.create("http://www.example.org:8080/intercept/path/a/b/c");

        assertEquals(interceptURI, toTest.interceptUriFor(interceptURI));
    }

    @Test
    public void nonInterceptUriForTest() {

        final URI resourceUri = URI.create("http://www.example.org:8080/intercept/path/a/b/c");
        final URI nonInterceptURI = URI.create("http://www.example.org/fcrepo/a/b/c");

        assertEquals(nonInterceptURI, toTest.nonProxyURIFor(resourceUri));
    }

    @Test
    public void passThroughNonInterceptUriTest() {

        final URI nonInterceptURI = URI.create("http://www.example.org/fcrepo/a/b/c");

        assertEquals(nonInterceptURI, toTest.nonProxyURIFor(nonInterceptURI));
    }

}