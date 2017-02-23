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

import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.jena.Util.query;
import static org.fcrepo.apix.jena.Util.subjectsOf;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_EXPOSED_SERVICE_URI;
import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_REPOSITORY_RESOURCE_URI;
import static org.fcrepo.apix.model.components.Routing.HTTP_HEADER_REPOSITORY_ROOT_URI;
import static org.fcrepo.apix.routing.Util.append;
import static org.fcrepo.apix.routing.Util.segment;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.RoutingFactory;
import org.fcrepo.apix.model.components.ServiceDiscovery;
import org.fcrepo.apix.model.components.ServiceRegistry;
import org.fcrepo.apix.model.components.Updateable;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultMessage;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Model;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

/**
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class ExposedServiceIT implements KarafIT {

    final String serviceEndpoint = "http://127.0.0.1:" + System.getProperty("services.dynamic.test.port") +
            "/ExposedServiceIT";

    URI requestURI = URI.create(apixBaseURI);

    URI exposedServiceEndpoint;

    private final Message responseFromService = new DefaultMessage();

    private final Message requestToService = new DefaultMessage();

    @Inject
    ServiceDiscovery discovery;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Inject
    ServiceRegistry serviceRegistry;

    @Inject
    @Filter("(role=test)")
    CamelContext cxt;

    @Inject
    BundleContext bundleContext;

    @Inject
    RoutingFactory routingFactory;

    @Override
    public String testClassName() {
        return ExposedServiceIT.class.getSimpleName();
    }

    @Rule
    public TestName name = new TestName();

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    @Before
    public void setUp() throws Exception {
        cxt.addRoutes(createRouteBuilder());
    }

    // Make camel-test feature available to the pax exam test probe
    @Override
    public List<Option> additionalKarafConfig() {
        final MavenArtifactUrlReference testBundle = maven()
                .groupId("org.fcrepo.apix")
                .artifactId("fcrepo-api-x-test")
                .versionAsInProject();
        return Arrays.asList(mavenBundle(testBundle));
        // return Arrays.asList(features(camelRepo, "camel-test"));
    }

    // @Override
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

    @Test
    public void resourceScopeTest() throws Exception {

        // Register the extension
        extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT.ttl"));

        // Register the service
        final URI serviceURI = serviceRegistry.put(testResource("objects/service_ExposedServiceIT.ttl"));

        // Register the test service instance endpoint (run by the camel route in this IT)
        client.patch(serviceURI).body(
                IOUtils.toInputStream(
                        String.format("INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                PROP_HAS_ENDPOINT, serviceEndpoint, CLASS_SERVICE_INSTANCE), "UTF-8")).perform();

        // Create the object
        final URI object = postFromTestResource("objects/object_ExposedServiceIT.ttl", objectContainer);

        commonTests(object);

        // Make sure the repository resource URI is relayed to our service, in the expected header.
        assertEquals(object.toString(), requestToService.getHeader(HTTP_HEADER_REPOSITORY_RESOURCE_URI));
    }

    @Test
    public void repositoryScopeTest() throws Exception {

        // Register the extension
        extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT_repository.ttl"));

        // Register the service
        final URI serviceURI = serviceRegistry.put(testResource("objects/service_ExposedServiceIT_repository.ttl"));

        // Register the test service instance endpoint (run by the camel route in this IT)
        client.patch(serviceURI).body(
                IOUtils.toInputStream(
                        String.format("INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                PROP_HAS_ENDPOINT, serviceEndpoint, CLASS_SERVICE_INSTANCE), "UTF-8")).perform();

        // Create the object
        final URI object = postFromTestResource("objects/object_ExposedServiceIT_repository.ttl", objectContainer);

        commonTests(object);

        // Make sure the repository resource URI is null for repository-scoped services.
        assertNull(requestToService.getHeader(HTTP_HEADER_REPOSITORY_RESOURCE_URI));

        // Make sure repository root URI is set
        assertEquals(segment(fcrepoBaseURI), segment(requestToService.getHeader(HTTP_HEADER_REPOSITORY_ROOT_URI,
                String.class)));

    }

    @Test
    public void duplicateExtensionTest() throws Exception {

        // Register the extension TWICE
        final URI first = extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT_duplicate.ttl"));

        final URI second = extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT_duplicate.ttl"));

        assertNotEquals(first, second);

        // Register the service
        final URI serviceURI = serviceRegistry.put(testResource("objects/service_ExposedServiceIT_duplicate.ttl"));

        // Register the test service instance endpoint (run by the camel route in this IT)
        client.patch(serviceURI).body(
                IOUtils.toInputStream(
                        String.format("INSERT {?instance <%s> <%s> .} WHERE {?instance a <%s> .}",
                                PROP_HAS_ENDPOINT, serviceEndpoint, CLASS_SERVICE_INSTANCE), "UTF-8")).perform();

        // Create the object
        final URI object = postFromTestResource("objects/object_ExposedServiceIT_duplicate.ttl", objectContainer);

        // Common tests should not fail, as the impl should pick one extension definition arbitrarily.
        commonTests(object);
    }

    @Test
    public void noInstanceTest() throws Exception {
        // Register the extension

        extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT_noInstance.ttl"));

        // Register the service
        serviceRegistry.put(testResource("objects/service_ExposedServiceIT_noInstance.ttl"));

        // Do NOT register a service instance.

        // Create the object
        final URI object = postFromTestResource("objects/object_ExposedServiceIT_noInstance.ttl", objectContainer);

        // Update all services
        bundleContext.getServiceReferences(Updateable.class, null).stream()
                .map(bundleContext::getService)
                .forEach(Updateable::update);

        // Look at the service document to discover the exposed URI
        try (WebResource resource = discovery
                .getServiceDocumentFor(object, routingFactory.of(requestURI), "text/turtle")) {
            final Model doc = parse(resource);

            final String sparql = "CONSTRUCT { ?endpoint <test:/endpointFor> ?serviceInstance . } WHERE { " +
                    String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                    String.format("?serviceInstance <%s> ?endpoint . ", PROP_HAS_ENDPOINT) +
                    "}";
            exposedServiceEndpoint = subjectsOf(query(sparql, doc)).iterator().next();
        }

        try (final FcrepoResponse response = FcrepoClient.client().build().post(exposedServiceEndpoint).perform()) {
            // We want a 404
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    public void noServiceTest() throws Exception {
        // Register the extension
        extensionRegistry.put(testResource(
                "objects/extension_ExposedServiceIT_noService.ttl"));

        // Do NOT register a service or instance

        final URI object = postFromTestResource("objects/object_ExposedServiceIT_noService.ttl", objectContainer);

        // Update all services
        bundleContext.getServiceReferences(Updateable.class, null).stream()
                .map(bundleContext::getService)
                .forEach(Updateable::update);

        // Look at the service document to discover the exposed URI
        try (WebResource resource = discovery
                .getServiceDocumentFor(object, routingFactory.of(requestURI), "text/turtle")) {
            final Model doc = parse(resource);

            final String sparql = "CONSTRUCT { ?endpoint <test:/endpointFor> ?serviceInstance . } WHERE { " +
                    String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                    String.format("?serviceInstance <%s> ?endpoint . ", PROP_HAS_ENDPOINT) +
                    "}";
            exposedServiceEndpoint = subjectsOf(query(sparql, doc)).iterator().next();
        }

        try (final FcrepoResponse response = FcrepoClient.client().build().post(exposedServiceEndpoint).perform()) {
            // We want a 404
            assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatusCode());
        }
    }

    private void commonTests(final URI object) throws Exception {

        // Update all services
        bundleContext.getServiceReferences(Updateable.class, null).stream()
                .map(bundleContext::getService)
                .forEach(Updateable::update);

        // Look at the service document to discover the exposed URI
        try (WebResource resource = discovery
                .getServiceDocumentFor(object, routingFactory.of(requestURI), "text/turtle")) {
            final Model doc = parse(resource);

            final String sparql = "CONSTRUCT { ?endpoint <test:/endpointFor> ?serviceInstance . } WHERE { " +
                    String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                    String.format("?serviceInstance <%s> ?endpoint . ", PROP_HAS_ENDPOINT) +
                    "}";
            exposedServiceEndpoint = subjectsOf(query(sparql, doc)).iterator().next();
        }

        // Specify the behaviour of the test service
        final String BODY = "Success!";
        final String additionalPath = "/additional/path/";
        final String query = "test=1&amp;other=2";
        final String slug = "testSlug";
        final String customHeader = "X-CustotmHeader";
        final String customHeaderValue = "123";
        responseFromService.setBody(BODY);
        responseFromService.setHeader(Exchange.HTTP_RESPONSE_CODE, HttpStatus.SC_ACCEPTED);
        responseFromService.setHeader(customHeader, customHeaderValue);

        // Perform the request!
        final FcrepoResponse response = client.post(
                append(exposedServiceEndpoint, String.join("?", additionalPath, query))).slug(slug).perform();

        // Make sure the host header in the response to client matches request host from client
        assertEquals(exposedServiceEndpoint.getAuthority(), URI.create(requestToService.getHeader(Exchange.HTTP_URL,
                String.class)).getAuthority());

        // Make sure the body returned by the service is received
        assertEquals(BODY, IOUtils.toString(response.getBody(), "UTF-8"));

        // Make sure the status code is what we want
        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusCode());

        // Make sure the query components are passed to the service
        assertEquals(query, requestToService.getHeader(Exchange.HTTP_QUERY));

        // Make sure the path components are passed on
        assertEquals(additionalPath, requestToService.getHeader(Exchange.HTTP_PATH));

        // Make sure a client-provided header is passed along
        assertEquals(slug, requestToService.getHeader("Slug"));

        // Make sure that a server-provided header is passed back to client
        assertEquals(customHeaderValue, response.getHeaderValue(customHeader));

        // Make sure the exposed service URI relayed to our service, in the expected header.
        assertEquals(exposedServiceEndpoint.toString(), requestToService.getHeader(HTTP_HEADER_EXPOSED_SERVICE_URI));
    }
}
