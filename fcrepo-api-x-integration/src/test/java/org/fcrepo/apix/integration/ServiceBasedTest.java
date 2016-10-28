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

import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.ServiceRegistry;
import org.fcrepo.apix.model.components.Updateable;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.BundleContext;

/**
 * Test base class which exposes an HTTP service; implemented by camel-jetty
 *
 * @author apb@jhu.edu
 */
public abstract class ServiceBasedTest implements KarafIT {

    static final protected String serviceEndpoint = "http://127.0.0.1:" + System.getProperty(
            "services.dynamic.test.port") +
            "/TestService";

    @Inject
    private CamelContext cxt;

    @Inject
    private ExtensionRegistry extensionRegistry;

    @Inject
    private ServiceRegistry serviceRegistry;

    @Inject
    BundleContext bundleContext;

    protected final Message responseFromService = new DefaultMessage();

    protected final Message requestToService = new DefaultMessage();

    private Processor processFromTest;

    @Override
    public List<Option> additionalKarafConfig() {
        final MavenArtifactUrlReference testBundle = maven()
                .groupId("org.fcrepo.apix")
                .artifactId("fcrepo-api-x-test")
                .versionAsInProject();
        return Arrays.asList(mavenBundle(testBundle));
    }

    @Before
    public void start() throws Exception {
        cxt.addRoutes(createRouteBuilder());
    }

    protected void onServiceRequest(final Processor process) {
        processFromTest = process;
    }

    protected URI registerService(final WebResource service) throws Exception {
        // Register the service
        final URI serviceURI = serviceRegistry.put(service);

        // Register the test service instance endpoint (run by the camel route in this IT)
        client.patch(serviceURI).body(
                IOUtils.toInputStream(
                        String.format("INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                PROP_HAS_ENDPOINT, serviceEndpoint, CLASS_SERVICE_INSTANCE), "UTF-8")).perform();

        update();

        return serviceURI;
    }

    protected URI registerExtension(final WebResource extension) throws Exception {

        final URI extensionURI = extensionRegistry.put(extension);

        update();

        return extensionURI;
    }

    protected void update() throws Exception {

        bundleContext.getServiceReferences(Updateable.class, null).stream()
                .map(bundleContext::getService)
                .forEach(Updateable::update);
    }

    private RoutesBuilder createRouteBuilder() throws Exception {

        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jetty:" + serviceEndpoint +
                        "?matchOnUriPrefix=true")
                                .process(ex -> {
                                    ex.getOut().copyFrom(responseFromService);
                                    requestToService.copyFrom(ex.getIn());

                                    if (processFromTest != null) {
                                        processFromTest.process(ex);
                                    }

                                });
            }
        };
    }
}
