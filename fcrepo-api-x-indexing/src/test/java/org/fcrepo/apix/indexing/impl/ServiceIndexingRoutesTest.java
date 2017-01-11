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

package org.fcrepo.apix.indexing.impl;

import static org.fcrepo.apix.indexing.impl.ServiceIndexingRoutes.ROUTE_PROCESS_MESSAGE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_EVENT_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.update.UpdateAction;
import org.junit.Before;
import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
@SuppressWarnings("serial")
public class ServiceIndexingRoutesTest extends CamelBlueprintTestSupport {

    private static final String RESOURCE_CREATION = "http://fedora.info/definitions/v4/event#ResourceCreation";

    private static final String RESOURCE_DELETION = "http://fedora.info/definitions/v4/event#ResourceDeletion";

    private static final String RESOURCE_MODIFICATION =
            "http://fedora.info/definitions/v4/event#ResourceModification";

    @EndpointInject(uri = "mock:triplestore")
    protected MockEndpoint tripleStoreEndpoint;

    @EndpointInject(uri = "mock:reindex")
    protected MockEndpoint reindexEndpoint;

    private static final String REINDEX_STREAM = "direct:test_reindex";

    @Produce(uri = "direct:start")
    protected ProducerTemplate indexer;

    @Produce(uri = REINDEX_STREAM)
    protected ProducerTemplate reindex;

    private Dataset dataset;

    private String serviceDocumentBody = "";

    private static final String SERVICE_DOC_URI = "test:ServiceDoc";

    private final Object linkHeader = Arrays.asList("<" + SERVICE_DOC_URI + ">; rel=\"service\"");

    private static final String EXTENSION_BASEURI = "http://example.org/extensions";

    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String setConfigAdminInitialConfiguration(final Properties props) {
        props.put("service.index.stream", "direct:test_index");
        props.put("service.reindex.stream", REINDEX_STREAM);
        props.put("apix.baseUrl", "direct:apix-head");
        props.put("reindexing.service.uri", "mock:reindex");
        props.put("triplestore.baseUrl", "mock:triplestore");
        props.put("ldp.path.extension.container", EXTENSION_BASEURI);
        return "org.fcrepo.apix.indexing";
    }

    @Before
    public void init() throws Exception {
        dataset = DatasetFactory.create();

        // Create two named graphs with triples. Verify that they're non-empty from the start.
        final Model a = ModelFactory.createDefaultModel();
        final Model b = ModelFactory.createDefaultModel();

        a.add(a.getResource("test:something"), a.getProperty("test:prop"), a.getResource("test:whatever"));
        b.add(b.getResource("test:somethingElse"), b.getProperty("test:prop"), b.getResource("test:whatever"));

        dataset.addNamedModel("test:unaffected", a);
        dataset.addNamedModel(SERVICE_DOC_URI, b);

        // Skip the event processor parsing, this test uses the final headers and NOT raw repository event json.
        context.getRouteDefinition("index-services").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                replaceFromWith("direct:start");
                interceptSendToEndpoint(ROUTE_PROCESS_MESSAGE).skipSendToOriginalEndpoint().to("log:foo");
            }
        });

        // Do not send an http request to get the content of the service document. Instead, use the
        // content of the 'body' string
        context.getRouteDefinition("perform-index").adviceWith(context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                interceptSendToEndpoint("http://localhost").skipSendToOriginalEndpoint().process(ex -> {
                    assertEquals(URI.create(SERVICE_DOC_URI), ex.getIn().getHeader(Exchange.HTTP_URI));
                    ex.getIn().setBody(serviceDocumentBody);
                    ex.getIn().setHeader("Content-Type", "text/turtle");
                }).to("log:foo");
            }
        });

        // Add our test routes
        new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:apix-head")
                        .process(ex -> ex.getIn().setHeader("Link", linkHeader));

            }
        }.addRoutesToCamelContext(context);
    }

    @Test
    public void testDelete() throws Exception {

        // Verify that all relevant graphs have content
        assertFalse(dataset.getNamedModel("test:unaffected").listStatements().toList().isEmpty());
        assertFalse(dataset.getNamedModel(SERVICE_DOC_URI).listStatements().toList().isEmpty());

        // Sent a DELETE event
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_DELETION));
            }
        });

        // Capture the sparql request from the mock, and manually apply the sparql;
        tripleStoreEndpoint.setExpectedCount(1);
        assertMockEndpointsSatisfied();
        UpdateAction.parseExecute(tripleStoreEndpoint.getExchanges().get(0).getIn().getBody(String.class), dataset);

        // Verify that the desired graph is empty, but the other one is unaffected
        assertFalse(dataset.getNamedModel("test:unaffected").listStatements().toList().isEmpty());
        assertTrue(dataset.getNamedModel(SERVICE_DOC_URI).listStatements().toList().isEmpty());
    }

    @Test
    public void testUpdate() throws Exception {

        final String EXPECTED_SUBJECT = "test:expectedSubject";

        final String EXPECTED_PREDICATE = "test:expectedPredicate";

        final String EXPECTED_OBJECT = "test:expectedObject";

        // This is what we want to update to: a single triple with expected content
        serviceDocumentBody = String.format("<%s> <%s> <%s> .", EXPECTED_SUBJECT, EXPECTED_PREDICATE,
                EXPECTED_OBJECT);

        // Verify that the triple store contains names graph equal to SERVICE_DOC_URI,
        // that it *does* have some content, but that it does *not* contain our expected content yet
        final Model beforeUpdate = dataset.getNamedModel(SERVICE_DOC_URI);
        assertTrue(beforeUpdate.listStatements().hasNext());
        assertFalse(beforeUpdate.contains(
                beforeUpdate.getResource(EXPECTED_SUBJECT),
                beforeUpdate.getProperty(EXPECTED_PREDICATE),
                beforeUpdate.getResource(EXPECTED_OBJECT)));

        // Send the update
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_MODIFICATION));
            }
        });

        // Capture the sparql request from the mock, and manually apply the sparql;
        tripleStoreEndpoint.setExpectedCount(1);
        assertMockEndpointsSatisfied();
        UpdateAction.parseExecute(tripleStoreEndpoint.getExchanges().get(0).getIn().getBody(String.class), dataset);

        // We should have only one triple in the graph named by the service document URI
        final Model afterUpdate = dataset.getNamedModel(SERVICE_DOC_URI);
        assertEquals(1, afterUpdate.listStatements().toList().size());

        // .. and the triple contains expected content
        assertTrue(dataset.getNamedModel(SERVICE_DOC_URI).contains(
                afterUpdate.getResource(EXPECTED_SUBJECT),
                afterUpdate.getProperty(EXPECTED_PREDICATE),
                afterUpdate.getResource(EXPECTED_OBJECT)));
    }

    @Test
    public void testCreate() throws Exception {
        final String EXPECTED_SUBJECT = "test:expectedSubject";

        final String EXPECTED_PREDICATE = "test:expectedPredicate";

        final String EXPECTED_OBJECT = "test:expectedObject";

        // This is what we want to update to: a single triple with expected content
        serviceDocumentBody = String.format("<%s> <%s> <%s> .", EXPECTED_SUBJECT, EXPECTED_PREDICATE,
                EXPECTED_OBJECT);

        // Ensure that the triple store contains does not contain named graph SERVICE_DOC_URI
        dataset.removeNamedModel(SERVICE_DOC_URI);
        assertFalse(dataset.containsNamedModel(SERVICE_DOC_URI));

        // Send the update
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_CREATION));
            }
        });

        // Capture the sparql request from the mock, and manually apply the sparql;
        tripleStoreEndpoint.setExpectedCount(1);
        assertMockEndpointsSatisfied();
        UpdateAction.parseExecute(tripleStoreEndpoint.getExchanges().get(0).getIn().getBody(String.class), dataset);

        // We should have only one triple in the graph named by the service document URI
        final Model afterUpdate = dataset.getNamedModel(SERVICE_DOC_URI);
        assertEquals(1, afterUpdate.listStatements().toList().size());

        // .. and the triple contains expected content
        assertTrue(dataset.getNamedModel(SERVICE_DOC_URI).contains(
                afterUpdate.getResource(EXPECTED_SUBJECT),
                afterUpdate.getProperty(EXPECTED_PREDICATE),
                afterUpdate.getResource(EXPECTED_OBJECT)));
    }

    @Test
    public void testIgnoreHashURIs() throws Exception {
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test#ignore");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_CREATION));
            }
        });

        tripleStoreEndpoint.setExpectedMessageCount(0);
        assertMockEndpointsSatisfied();
    }

    @Test
    public void testTriggerReindex() throws Exception {

        // Ordinary object
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_CREATION));
            }
        });

        // Something under our extensions container. Trigger a reindex
        indexer.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, EXTENSION_BASEURI + "/whatever");
                put(FCREPO_EVENT_TYPE, Arrays.asList(RESOURCE_CREATION));
            }
        });

        reindexEndpoint.setExpectedCount(1);
        tripleStoreEndpoint.setExpectedCount(2);
        assertMockEndpointsSatisfied();

        final Message reindexMessage = reindexEndpoint.getExchanges().get(0).getIn();

        assertTrue(reindexMessage.getBody(String.class).contains(REINDEX_STREAM));
        assertEquals("POST", reindexMessage.getHeader(Exchange.HTTP_METHOD));
        assertEquals("application/json", reindexMessage.getHeader("Content-Type"));
    }

    @Test
    public void testReindexStream() throws Exception {
        final String EXPECTED_SUBJECT = "test:expectedSubject";

        final String EXPECTED_PREDICATE = "test:expectedPredicate";

        final String EXPECTED_OBJECT = "test:expectedObject";

        // This is what we want to update to: a single triple with expected content
        serviceDocumentBody = String.format("<%s> <%s> <%s> .", EXPECTED_SUBJECT, EXPECTED_PREDICATE,
                EXPECTED_OBJECT);

        // Verify that the triple store contains names graph equal to SERVICE_DOC_URI,
        // that it *does* have some content, but that it does *not* contain our expected content yet
        final Model beforeUpdate = dataset.getNamedModel(SERVICE_DOC_URI);
        assertTrue(beforeUpdate.listStatements().hasNext());
        assertFalse(beforeUpdate.contains(
                beforeUpdate.getResource(EXPECTED_SUBJECT),
                beforeUpdate.getProperty(EXPECTED_PREDICATE),
                beforeUpdate.getResource(EXPECTED_OBJECT)));

        // Send the reindex message. All we have is a URI.
        reindex.sendBodyAndHeaders(null, new HashMap<String, Object>() {

            {
                put(FCREPO_URI, "http://example.org/test");
            }
        });

        // Capture the sparql request from the mock, and manually apply the sparql;
        tripleStoreEndpoint.setExpectedCount(1);
        assertMockEndpointsSatisfied();
        UpdateAction.parseExecute(tripleStoreEndpoint.getExchanges().get(0).getIn().getBody(String.class), dataset);

        // We should have only one triple in the graph named by the service document URI
        final Model afterUpdate = dataset.getNamedModel(SERVICE_DOC_URI);
        assertEquals(1, afterUpdate.listStatements().toList().size());

        // .. and the triple contains expected content
        assertTrue(dataset.getNamedModel(SERVICE_DOC_URI).contains(
                afterUpdate.getResource(EXPECTED_SUBJECT),
                afterUpdate.getProperty(EXPECTED_PREDICATE),
                afterUpdate.getResource(EXPECTED_OBJECT)));
    }
}
