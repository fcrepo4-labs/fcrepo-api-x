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
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * Verifies that imported ontologies in extensions are persisted, where desired.
 *
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class OntologyIT extends ServiceBasedTest {

    @Rule
    public TestName name = new TestName();

    @Override
    public String testClassName() {
        return OntologyIT.class.getSimpleName();
    }

    @Inject
    OntologyRegistry ontologyRegistry;

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    @Test
    public void depositTest() throws Exception {

        final URI ontologyIRI = URI.create(serviceEndpoint);
        final String ontologyContent = String.format(
                "<%s> a <http://www.w3.org/2002/07/owl#Ontology> .", ontologyIRI);

        final AtomicBoolean ontologyFetched = new AtomicBoolean(false);

        onServiceRequest(e -> {
            e.getOut().setBody(ontologyContent);
            e.getOut().setHeader("Content-Type", "text/turtle");
            ontologyFetched.set(true);
        });

        try (FcrepoResponse response = client.post(extensionContainer)
                .body(IOUtils.toInputStream(
                        String.format("<> <http://www.w3.org/2002/07/owl#imports> <%s> .", ontologyIRI), "utf8"),
                        "text/turtle").perform()) {

            update();

            assertTrue(ontologyFetched.get());

            try (WebResource ontology = ontologyRegistry.get(ontologyIRI)) {
                assertNotEquals(ontology.uri(), ontologyIRI);
                final Model parsed = parse(ontology);
                assertTrue(parsed.contains(parsed.getResource(ontologyIRI.toString()), parsed.getProperty(RDF_TYPE),
                        parsed.getResource("http://www.w3.org/2002/07/owl#Ontology")));
            }
        }
    }
}
