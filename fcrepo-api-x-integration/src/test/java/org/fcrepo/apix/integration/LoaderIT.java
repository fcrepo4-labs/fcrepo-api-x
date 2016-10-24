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
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_INSTANCE_OF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.Routing;

import org.apache.camel.Exchange;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.FormElement;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.util.Filter;

/**
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class LoaderIT extends ServiceBasedTest {

    final URI SERVICE_MINIMAL = URI.create("http://example.org/LoaderIT/minimal");

    @Rule
    public TestName name;

    @Inject
    @Filter("(org.fcrepo.apix.registry.role=default)")
    Registry repository;

    @Override
    public String testClassName() {
        return LoaderIT.class.getSimpleName();
    }

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @Inject
    Routing routing;

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    @Override
    public List<Option> additionalKarafConfig() {
        final List<Option> options = new ArrayList<>(super.additionalKarafConfig());

        // This test dependency is not in any features files, so we have to add it manually.
        final MavenArtifactUrlReference jsoup = maven().groupId("org.jsoup")
                .artifactId("jsoup")
                .versionAsInProject();

        options.add(mavenBundle(jsoup));

        return options;
    }

    @Test
    public void htmlMinimalTest() throws Exception {

        final String SERVICE_RESPONSE_BODY = "BODY";

        final String LOADER_URI = "http://127.0.0.1:" + System.getProperty(
                "loader.dynamic.test.port") + "/load";

        onServiceRequest(ex -> {
            if ("OPTIONS".equals(ex.getIn().getHeader(Exchange.HTTP_METHOD))) {
                ex.getOut().setBody(testResource("objects/options_LoaderIT_minimal.ttl").representation());
            } else {
                ex.getOut().setBody(SERVICE_RESPONSE_BODY);
            }
        });

        final Document html = attempt(10, () -> Jsoup.connect(LOADER_URI).method(Method.GET)
                .execute().parse());
        final FormElement form = ((FormElement) html.getElementById("uriForm"));
        form.getElementById("uri").val(serviceEndpoint);

        // responseFromService.setBody(testResource("objects/options_LoaderIT_minimal.ttl").representation());
        final Response response = form.submit().ignoreHttpErrors(true).followRedirects(false).execute();

        assertEquals("OPTIONS", requestToService.getHeader(Exchange.HTTP_METHOD));
        assertEquals(303, response.statusCode());
        assertNotNull(response.header("Location"));

        // Verify that extension works!
        final URI container = routing.interceptUriFor(objectContainer);
        System.out.println("Fedora container is " + objectContainer);
        System.out.println("Proxied container is " + container);

        final URI deposited = client.post(container).slug("LoaderIT_htmlMinimalTest")
                .body(IOUtils.toInputStream("<> a <test:LoaderIT#minimal> .", "utf8"), "text/turtle")
                .perform().getLocation();

        final URI discoveryDoc = client.options(deposited).perform().getLinkHeaders("service").get(0);

        final String body = attempt(10, () -> IOUtils.toString(
                client.get(serviceEndpoints(discoveryDoc).get(SERVICE_MINIMAL)).perform()
                        .getBody(), "utf8"));
        assertEquals(SERVICE_RESPONSE_BODY, body);

    }

    private <T> T attempt(final int times, final Callable<T> it) {

        Exception caught = null;

        for (int tries = 0; tries < times; tries++) {
            try {
                return it.call();
            } catch (final Exception e) {
                caught = e;
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        throw new RuntimeException("Failed executing task", caught);
    }

    private Map<URI, URI> serviceEndpoints(final URI discoveryDoc) throws Exception {
        try (WebResource resource = repository.get(discoveryDoc)) {
            final Model doc = parse(resource);

            final String sparql = "CONSTRUCT { ?endpoint <test:/endpointFor> ?service . } WHERE { " +
                    String.format("?serviceInstance <%s> <%s> . ", RDF_TYPE, CLASS_SERVICE_INSTANCE) +
                    String.format("?serviceInstance <%s> ?service . ", PROP_IS_SERVICE_INSTANCE_OF) +
                    String.format("?serviceInstance <%s> ?endpoint . ", PROP_HAS_ENDPOINT) +
                    "}";

            return query(sparql, doc).collect(Collectors.toMap(
                    t -> URI.create(t.getObject().getURI()),
                    t -> URI.create(t.getSubject().getURI())));
        }
    }

}
