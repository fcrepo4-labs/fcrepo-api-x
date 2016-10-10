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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * Test which exposes an HTTP service; implemented by camel-jetty
 *
 * @author apb@jhu.edu
 */
public class ServiceExposingTest {

    static final protected String serviceEndpoint = "http://127.0.0.1:" + System.getProperty(
            "services.dynamic.test.port") +
            "/TestService";

    @Inject
    CamelContext cxt;

    protected final Message responseFromService = new DefaultMessage();

    protected final Message requestToService = new DefaultMessage();

    public List<Option> additionalKarafConfig() {
        final MavenArtifactUrlReference testBundle = maven()
                .groupId("org.fcrepo.apix")
                .artifactId("fcrepo-api-x-test")
                .versionAsInProject();
        return Arrays.asList(mavenBundle(testBundle));
        // return Arrays.asList(features(camelRepo, "camel-test"));
    }

    @Before
    public void start() throws Exception {
        cxt.addRoutes(createRouteBuilder());
    }

    protected RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jetty:" + serviceEndpoint +
                        "?matchOnUriPrefix=true").process(ex -> {
                            ex.getOut().copyFrom(responseFromService);
                            requestToService.copyFrom(ex.getIn());
                        });
            }
        };
    }

}
