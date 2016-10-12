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

package org.fcrepo.apix.integration;

import static org.junit.Assert.assertEquals;

import java.net.URI;

import javax.inject.Inject;

import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import org.apache.camel.Exchange;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class InterceptingModalityIT extends ServiceBasedTest implements KarafIT {

    @Inject
    Routing routing;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Rule
    public TestName name = new TestName();

    @Override
    public String testClassName() {
        return InterceptingModalityIT.class.getSimpleName();
    }

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    // Verify that a 'link' header pointing to the service document is present
    @Test
    public void serviceLinkHeaderTest() throws Exception {

        final URI object = client.post(objectContainer).slug(testMethodName()).perform().getLocation();

        final FcrepoResponse response = client.get(routing.interceptUriFor(object)).perform();

        assertEquals(1, response.getLinkHeaders("service").size());
        assertEquals(routing.serviceDocFor(object), response.getLinkHeaders("service").get(0));
    }

    // Verify that if an intercepting extension throws a 4xx code, it aborts the request and returns service response.
    @Test
    public void incomingInterceptRespomseCodeTest() throws Exception {
        registerExtension(testResource("objects/extension_InterceptingModalityIT.ttl"));

        registerService(testResource("objects/service_InterceptingServiceIT.ttl"));

        final String LOCATION = "http://example.org/Location";
        final int RESPONSE_CODE = 418;

        responseFromService.setHeader(Exchange.HTTP_RESPONSE_CODE, RESPONSE_CODE);
        responseFromService.setHeader("Location", LOCATION);

        final URI objectContainer_intercept = routing.interceptUriFor(objectContainer);

        final URI object = postFromTestResource("objects/object_InterceptingServiceIT.ttl",
                objectContainer_intercept);

        final FcrepoResponse response = FcrepoClient.client().build().get(object).perform();

        assertEquals(RESPONSE_CODE, response.getStatusCode());
        assertEquals(LOCATION, response.getHeaderValue("Location"));
    }

}
