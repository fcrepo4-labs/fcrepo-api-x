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

package org.fcrepo.apix.registry;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.fcrepo.apix.registry.HttpClientFactory.AuthSpec;

import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class HttpClentFactoryTest {

    @Test
    public void goodAuthSpecsTest() {

        final HttpClientFactory toTest = new HttpClientFactory();

        final String scheme1 = "http";
        final int port1 = 8080;
        final String host1 = "example.org";
        final String username1 = "user";
        final String passwd1 = "pass";

        final String scheme2 = "https";
        final int port2 = 80;
        final String host2 = "this.username.is.password.auth.https.80.whatever.password";
        final String username2 = "whatever";
        final String passwd2 = "passwd2";

        final Map<String, String> props = new TreeMap<>();
        props.put(String.format("auth.%s.%s.%s.username", scheme1, port1, host1), username1);
        props.put(String.format("auth.%s.%s.%s.password", scheme1, port1, host1), passwd1);
        props.put(String.format("auth.%s.%s.%s.username", scheme2, port2, host2), username2);
        props.put(String.format("auth.%s.%s.%s.password", scheme2, port2, host2), passwd2);
        props.put("not.a.prop.we.care.about", "1234");

        toTest.setProperties(props);

        final List<AuthSpec> specs = toTest.getAuthSpecs();

        assertEquals(2, specs.size());

        assertEquals(host1, specs.get(0).host);
        assertEquals(scheme1, specs.get(0).scheme);
        assertEquals(port1, specs.get(0).port);
        assertEquals(username1, specs.get(0).username());
        assertEquals(passwd1, specs.get(0).passwd());

        assertEquals(host2, specs.get(1).host);
        assertEquals(scheme2, specs.get(1).scheme);
        assertEquals(port2, specs.get(1).port);
        assertEquals(username2, specs.get(1).username());
        assertEquals(passwd2, specs.get(1).passwd());
    }

    @Test
    public void noAuthSpecsTest() {
        final HttpClientFactory toTest = new HttpClientFactory();
        assertEquals(0, toTest.getAuthSpecs().size());
    }
}
