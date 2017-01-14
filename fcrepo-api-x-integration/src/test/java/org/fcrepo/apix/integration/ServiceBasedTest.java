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

import static org.fcrepo.apix.jena.Util.rdfResource;
import static org.fcrepo.apix.model.Ontologies.Apix.CLASS_EXTENSION;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_BINDS_TO;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_CONSUMES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE;
import static org.fcrepo.apix.model.Ontologies.Apix.PROP_EXPOSES_SERVICE_AT;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_CANONICAL;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
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
import org.junit.rules.TestName;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.util.Filter;
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

    static final String SERVICE_ROUTE_ID = "test-service-endpoint";

    @Inject
    @Filter("(role=test)")
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
        if (cxt.getRoute(SERVICE_ROUTE_ID) == null) {
            cxt.addRoutes(createRouteBuilder());
        }
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
                                .routeId(SERVICE_ROUTE_ID)
                                .process(ex -> {
                                    requestToService.copyFrom(ex.getIn());

                                    if (processFromTest != null) {
                                        processFromTest.process(ex);
                                    } else {
                                        ex.getOut().copyFrom(responseFromService);
                                    }

                                });
            }
        };
    }

    protected ExtensionBuilder newExtension(final TestName name) {
        return new ExtensionBuilder(name);
    }

    class ExtensionBuilder {

        String differentiator;

        private final String name;

        private Scope scope;

        private String bindingClass;

        ExtensionBuilder(final TestName name) {
            this.name = name.getMethodName();
            differentiator = name.getMethodName() + "-" + UUID.randomUUID().toString();
        }

        ExtensionBuilder withScope(final Scope scope) {
            this.scope = scope;
            return this;
        }

        ExtensionBuilder bindsTo(final URI bindingClass) {
            this.bindingClass = bindingClass.toString();
            return this;
        }

        ExtensionBuilder withDifferentiator(final String diff) {
            this.differentiator = name + "-" + diff;
            return this;
        }

        Extension create() throws Exception {

            if (bindingClass == null) {
                bindingClass = "http://example.org/test/class/" + differentiator;
            }

            final String serviceURI = "http://example.org/test/service/" + differentiator;

            registerService(rdfResource(null, serviceBody(
                    serviceURI), differentiator));

            return extensionRegistry.getExtension(
                    registerExtension(rdfResource(null,
                            extensionBody(serviceURI), differentiator)));

        }

        private String serviceBody(final String serviceURI) throws Exception {
            final StringBuilder serviceBody = new StringBuilder();
            try (WebResource template = testResource("objects/service.ttl")) {
                serviceBody.append(IOUtils.toString(template.representation(), "utf8"));
            }

            serviceBody.append(String.format("<#service> <%s> <%s> .\n", PROP_CANONICAL, serviceURI));

            return serviceBody.toString();

        }

        private String extensionBody(final String serviceURI) {
            final String differentiator = UUID.randomUUID().toString();

            final String endpointSpec = String.format("%s:%s", name, differentiator);
            final StringBuilder extensionBody = new StringBuilder();

            extensionBody.append(String.format("<> a <%s> .\n", CLASS_EXTENSION));
            extensionBody.append(String.format("<> <%s> <%s> .\n", PROP_BINDS_TO, bindingClass));

            if (Scope.RESOURCE.equals(scope)) {
                extensionBody.append(String.format("<> <%s> \"%s\" .\n", PROP_EXPOSES_SERVICE_AT, endpointSpec));
                extensionBody.append(String.format("<> <%s> <%s> .\n", PROP_EXPOSES_SERVICE, serviceURI));
            } else if (Scope.REPOSITORY.equals(scope)) {
                extensionBody.append(String.format("<> <%s> \"/%s\" .\n", PROP_EXPOSES_SERVICE_AT, endpointSpec));
                extensionBody.append(String.format("<> <%s> <%s> .\n", PROP_EXPOSES_SERVICE, serviceURI));
            } else {
                extensionBody.append(String.format("<> <%s> <%s> .\n", PROP_CONSUMES_SERVICE, serviceURI));
            }

            return extensionBody.toString();
        }
    }
}
