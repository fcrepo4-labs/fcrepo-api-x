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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionBinding;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.apix.model.components.OntologyService;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
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
 * Performs Loads ontologies, extensions, and instance objects in repo to verify extension binding.
 *
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class RepositoryExtensionBindingIT implements KarafIT {

    private static final URI ORE_ONTOLOGY_IRI = URI.create("http://www.openarchives.org/ore/terms/");

    private static final URI PCDM_ONTOLOGY_IRI = URI.create("http://pcdm.org/models");

    private static final URI TEST_ONTOLOGY_IRI = URI.create("http://example.org/test#Ontology");

    @Rule
    public TestName name = new TestName();

    @Inject
    BundleContext cxt;

    @Inject
    @Filter("(org.fcrepo.apix.registry.role=default)")
    Registry repository;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Inject
    OntologyService ontologyServivce;

    @Inject
    OntologyRegistry ontologyRegistry;

    @Inject
    ExtensionBinding extensionBinding;

    static final URI testContainer = URI.create(fcrepoBaseURI.toString() + RepositoryExtensionBindingIT.class
            .getSimpleName());

    @Override
    public List<Option> additionalKarafConfig() {

        // This dependency is not in any features files, so we have to add it manually.
        final MavenArtifactUrlReference fcrepoClient = maven().groupId("org.fcrepo.client")
                .artifactId("fcrepo-java-client")
                .versionAsInProject();

        return Arrays.asList(
                // Fcrepo client is not a dependency of anything else, but tests need it.
                // As this test runs as a maven bundle in Karaf, the test's reactor dependencies are not
                // available a priori.
                mavenBundle(fcrepoClient),

                // Pax exam runs in a separate VM, so we need to tell Karaf to set any system properties we need.
                editConfigurationFilePut("etc/system.properties", "fcrepo.dynamic.test.port", System.getProperty(
                        "fcrepo.dynamic.test.port")),
                editConfigurationFilePut("etc/system.properties", "project.basedir", System.getProperty(
                        "project.basedir")),
                editConfigurationFilePut("/etc/system.properties", "fcrepo.cxtPath", System.getProperty(
                        "fcrepo.cxtPath")),

                deployFile("cfg/org.fcrepo.apix.jena.cfg"),
                deployFile("cfg/org.fcrepo.apix.registry.http.cfg"));
    }

    @Before
    public void load() throws Exception {

        if (!ontologyRegistry.contains(PCDM_ONTOLOGY_IRI)) {
            ontologyRegistry.put(testResource("ontologies/pcdm.ttl"), PCDM_ONTOLOGY_IRI);
        }

        if (!ontologyRegistry.contains(ORE_ONTOLOGY_IRI)) {
            ontologyRegistry.put(testResource("ontologies/ore.ttl"), ORE_ONTOLOGY_IRI);
        }

        if (!ontologyRegistry.contains(TEST_ONTOLOGY_IRI)) {
            ontologyRegistry.put(testResource(
                    "ontologies/ReMWithOrderedAggregation.ttl"));
        }

        if (extensionRegistry.list().isEmpty()) {
            extensionRegistry.put(testResource(
                    "objects/extension_rem_ordered_collection.ttl"));
        }

        // Add the container for all repository objects created in this test suite, if it doesn't exist.
        try (FcrepoResponse head = client.head(testContainer).perform()) {
            /* Do nothing */
        } catch (final FcrepoOperationFailedException e) {
            if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                try (FcrepoResponse response = client.put(testContainer)
                        .perform()) {
                    if (response.getStatusCode() != HttpStatus.SC_CREATED && response
                            .getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                        throw new RuntimeException("Could not create base test container " + testContainer);
                    }
                }
            } else {
                throw (e);
            }
        }
    }

    // Extension should be bound to an object if it can be inferred that the object is a member of
    // the binding class. Here, we have an object with a PCDM collection with ordered members.
    // Our ontology defines a class for ORE aggregation ordered with proxies, and our extension binds
    // to objects of that class. So we should see that our object has our extensiion bound to it.
    @Test
    public void inferredBindingTest() throws Exception {
        final URI objectURI = putObjectFromTestResource("objects/object_with_ordered_collection.ttl");

        try {
            final Collection<Extension> extensions = extensionBinding.getExtensionsFor(repository.get(objectURI));
            assertFalse(extensions.isEmpty());
        } catch (final Exception e) {
            throw (e);
        }
    }

    // here, we never import ORE or PCDM in the extension, so API-X do the inferences needed in order to bind to the
    // extension. Modifying the object with an explicitly matching rdf:type should allow binding to occur.
    @Test
    public void noInferredBindingTest() throws Exception {

        // Put in an ontology that doesn't import ORE or PCDM
        ontologyRegistry.put(testResource(
                "ontologies/ReMWithOrderedAggregation_noImport.ttl"));

        // Now put in an extension that binds to a class from that ontology
        final URI extensionURI = extensionRegistry.put(testResource(
                "objects/extension_rem_ordered_collection_noimport.ttl"));
        final URI BINDING_CLASS = extensionRegistry.getExtension(extensionURI).bindingClass();

        // Now put in our object
        final URI objectURI = putObjectFromTestResource("objects/object_with_ordered_collection.ttl");

        // We shouldn't bind to the no-import extension, because that would require inferences
        // based on PCDM and ORE (which the ontology neglects to import)
        assertFalse(extensionBinding.getExtensionsFor(objectURI).stream()
                .map(Extension::uri)
                .collect(Collectors.toSet())
                .contains(extensionURI));

        // let's modify the object so it directly asserts a matching rdf:type
        client.patch(objectURI).body(IOUtils.toInputStream(String.format("INSERT {<> a <%s>} WHERE {}",
                BINDING_CLASS), "UTF-8")).perform().close();

        // Our (modified) object should bind now
        assertTrue(extensionBinding.getExtensionsFor(objectURI).stream()
                .map(Extension::uri)
                .collect(Collectors.toSet())
                .contains(extensionURI));
    }

    private URI putObjectFromTestResource(final String filePath) throws Exception {
        try (final WebResource object = testResource(filePath);
                final FcrepoResponse response = client.post(testContainer)
                        .body(object.representation(), object.contentType())
                        .slug(name.getMethodName())
                        .perform()) {
            return response.getLocation();
        }
    }

}
