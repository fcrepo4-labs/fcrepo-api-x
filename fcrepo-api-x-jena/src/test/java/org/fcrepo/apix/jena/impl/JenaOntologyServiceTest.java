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

package org.fcrepo.apix.jena.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.OntologyRegistry;

import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests Jena ontology service.
 *
 * @author apb@jhu.edu
 */
public class JenaOntologyServiceTest {

    JenaOntologyService toTest;

    static final String ONT1 = "http://example.org/ont1";

    static final String ONT2 = "http://example.org/ont2";

    static final String ONT3 = "http://example.org/ont3";

    static final String ONT4 = "http://example.org/ont4";

    static final String ONT5 = "http://example.org/ont5";

    static final String CLASS_A = "http://example.org/classes#A";

    static final String CLASS_B = "http://example.org/classes#B";

    static final String CLASS_C = "http://example.org/classes#C";

    static final String CLASS_D = "http://example.org/classes#D";

    static final String CLASS_E = "http://example.org/classes#E";

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    static final String SUBCLASS_OF = "http://www.w3.org/2000/01/rdf-schema#subClassOf";

    static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";

    static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";

    static OntologyRegistry ONTOLOGY_REGISTRY = mock(OntologyRegistry.class);

    @BeforeClass
    public static void populateRegistry() {

        final WebResource ONT1_RESOURCE = ontResource(ONT1,
                triple(ONT1, RDF_TYPE, OWL_ONTOLOGY) +
                        triple(CLASS_B, SUBCLASS_OF, CLASS_A));
        when(ONTOLOGY_REGISTRY.get(eq(URI.create(ONT1)))).thenReturn(ONT1_RESOURCE);

        final WebResource ONT2_RESOURCE = ontResource(ONT2,
                triple(ONT2, RDF_TYPE, OWL_ONTOLOGY) +
                        triple(CLASS_C, SUBCLASS_OF, CLASS_A));
        when(ONTOLOGY_REGISTRY.get(eq(URI.create(ONT2)))).thenReturn(ONT2_RESOURCE);

        final WebResource ONT3_RESOURCE =
                ontResource(ONT3,
                        triple(ONT3, RDF_TYPE, OWL_ONTOLOGY) +
                                triple(ONT3, OWL_IMPORTS, ONT1) +
                                triple(ONT3, OWL_IMPORTS, ONT2));
        when(ONTOLOGY_REGISTRY.get(eq(URI.create(ONT3)))).thenReturn(ONT3_RESOURCE);

        final WebResource ONT4_RESOURCE =
                ontResource(ONT4,
                        triple(ONT4, RDF_TYPE, OWL_ONTOLOGY) +
                                triple(ONT4, OWL_IMPORTS, ONT1) +
                                triple(CLASS_D, SUBCLASS_OF, CLASS_B));
        when(ONTOLOGY_REGISTRY.get(eq(URI.create(ONT4)))).thenReturn(ONT4_RESOURCE);

        final WebResource ONT5_RESOURCE =
                ontResource(ONT5,
                        triple(ONT5, RDF_TYPE, OWL_ONTOLOGY) +
                                triple(ONT5, OWL_IMPORTS, ONT4) +
                                triple(CLASS_E, SUBCLASS_OF, CLASS_D));
        when(ONTOLOGY_REGISTRY.get(eq(URI.create(ONT5)))).thenReturn(ONT5_RESOURCE);

    }

    @Before
    public void setUp() {
        toTest = new JenaOntologyService();
        toTest.setRegistryDelegate(ONTOLOGY_REGISTRY);
    }

    // Verify that an ontology can be retrieved (from the underlying registry) in the first place
    @Test
    public void ontologyRetrievalTest() {
        final OntModel model = toTest.getOntology(URI.create(ONT1)).model;

        final Model expectedTriples = ModelFactory.createDefaultModel();
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT1)).representation(), null, "N-TRIPLES");

        assertTrue(model.containsAll(expectedTriples));
    }

    // Verify that ontologies are merged.
    @Test
    public void ontologyMergeTest() {
        final Ontology model1 = toTest.getOntology(URI.create(ONT1));
        final Ontology model2 = toTest.getOntology(URI.create(ONT2));

        final OntModel merged = toTest.merge(model1, model2).model;

        final Model expectedTriples = ModelFactory.createDefaultModel();
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT1)).representation(), null, "N-TRIPLES");
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT2)).representation(), null, "N-TRIPLES");

        assertTrue(merged.containsAll(expectedTriples));
    }

    // Verify that imports are resolved (using the underlying registry),
    // and incorporated into the result ontology
    @Test
    public void followOwlImportsTest() {
        final OntModel model_includes_1_and_2 = toTest.getOntology(URI.create(ONT3)).model;

        final Model expectedTriples = ModelFactory.createDefaultModel();
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT1)).representation(), null, "N-TRIPLES");
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT2)).representation(), null, "N-TRIPLES");
        expectedTriples.read(ONTOLOGY_REGISTRY.get(URI.create(ONT3)).representation(), null, "N-TRIPLES");

        expectedTriples.removeAll(null, expectedTriples.getProperty(OWL_IMPORTS), null);

        assertTrue(model_includes_1_and_2.containsAll(expectedTriples));
    }

    // Verify that imports are resolved transitively
    @Test
    public void transitiveImportsTest() throws Exception {
        final OntModel transitive = toTest.getOntology(URI.create(ONT5)).model;

        // 5 includes 4, 4 includes 1
        final Model expectedTriples = ModelFactory.createDefaultModel();
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT1))
                .representation(), null, "N-TRIPLES"));
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT4))
                .representation(), null, "N-TRIPLES"));
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT5))
                .representation(), null, "N-TRIPLES"));

        expectedTriples.removeAll(null, expectedTriples.getProperty(OWL_IMPORTS), null);

        assertTrue(transitive.containsAll(expectedTriples));
    }

    // verifies that an ontology can be loaded manually, and transitive imports resolved with the registry
    @Test
    public void loadOntologyTest() {
        final WebResource resource = ONTOLOGY_REGISTRY.get(URI.create(ONT5));

        final OntModel transitive = toTest.parseOntology(resource).model;

        // 5 includes 4, 4 includes 1
        final Model expectedTriples = ModelFactory.createDefaultModel();
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT1))
                .representation(), null, "N-TRIPLES"));
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT4))
                .representation(), null, "N-TRIPLES"));
        expectedTriples.add(ModelFactory.createDefaultModel().read(ONTOLOGY_REGISTRY.get(URI.create(ONT5))
                .representation(), null, "N-TRIPLES"));

        expectedTriples.removeAll(null, expectedTriples.getProperty(OWL_IMPORTS), null);

        assertTrue(transitive.containsAll(expectedTriples));

    }

    // Verify that classes of an instance are inferred.
    @Test
    public void inferClassesTest() {
        final Ontology ontology = toTest.getOntology(URI.create(ONT1));

        final String individualURI = "test:/individual";

        final WebResource individual = mock(WebResource.class);
        when(individual.contentType()).thenReturn("application/n-triples");
        when(individual.representation()).thenReturn(IOUtils.toInputStream(triple(individualURI, RDF_TYPE, CLASS_B),
                Charset.defaultCharset()));

        assertTrue(toTest.inferClasses(URI.create(individualURI), individual, ontology).contains(URI.create(
                CLASS_A)));

    }

    // Verify that inference is over the closure of owl:imports
    @Test
    public void inferClassesUsingImportsTest() {
        final Ontology ontology = toTest.getOntology(URI.create(ONT5));

        final String individualURI = "test:/individual";

        final WebResource individual = mock(WebResource.class);
        when(individual.contentType()).thenReturn("application/n-triples");
        when(individual.representation()).thenReturn(IOUtils.toInputStream(triple(individualURI, RDF_TYPE, CLASS_E),
                Charset.defaultCharset()));

        assertTrue(toTest.inferClasses(URI.create(individualURI), individual, ontology).contains(URI.create(
                CLASS_A)));
    }

    private static String triple(final String s, final String p, final String o) {
        return String.format("<%s> <%s> <%s> .\n", s, p, o);
    }

    private static WebResource ontResource(final String uri, final String rdf) {

        return new WebResource() {

            @Override
            public void close() throws Exception {
                // nothing
            }

            @Override
            public URI uri() {
                return URI.create(uri);
            }

            @Override
            public InputStream representation() {
                return IOUtils.toInputStream(rdf, Charset.forName("UTF-8"));
            }

            @Override
            public Long length() {
                return 0l;
            }

            @Override
            public String contentType() {
                return "application/n-triples";
            }
        };
    }

}
