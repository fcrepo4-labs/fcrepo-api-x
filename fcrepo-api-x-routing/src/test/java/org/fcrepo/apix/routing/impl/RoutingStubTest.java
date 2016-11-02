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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 *
 * @author emetsger@jhu.edu
 */
public class RoutingStubTest {

    /**
     * Don't include the port when it is the default port for the scheme. (Right now http is assumed, no https support)
     */
    @Test
    public void testBaseUriPortPresence() {
        final RoutingStub underTest = new RoutingStub();
        underTest.setHost("www.example.org");

        underTest.setPort(80);
        assertEquals("http://www.example.org/", underTest.baseURI().toString());

        underTest.setPort(8080);
        assertEquals("http://www.example.org:8080/", underTest.baseURI().toString());
    }

}