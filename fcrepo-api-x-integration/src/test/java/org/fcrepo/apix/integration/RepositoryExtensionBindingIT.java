
package org.fcrepo.apix.integration;

import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.fcrepo.apix.model.ExtensionBinding;
import org.fcrepo.apix.model.ExtensionRegistry;
import org.fcrepo.apix.model.OntologyService;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class RepositoryExtensionBindingIT implements KarafIT {

    @Rule
    public TestName name = new TestName();

    @Inject
    BundleContext cxt;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Inject
    OntologyService ontologyServivce;

    @Inject
    ExtensionBinding extensionBinding;

    FcrepoClient client = FcrepoClient.client().throwExceptionOnFailure().build();

    URI pcdmOntology;

    URI oreOntology;

    URI testContainer;

    URI remWithOrderedAggregationOntology;

    URI remWithOrderedAggregationExtension;

    @Override
    public List<Option> additionalKarafConfig() {

        final MavenArtifactUrlReference fcrepoClient = maven().groupId("org.fcrepo.client")
                .artifactId("fcrepo-java-client")
                .versionAsInProject();

        return Arrays.asList(
                mavenBundle(fcrepoClient),
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
    public void loadOntologies() throws Exception {

        System.err.println("LOADING ONTOLOGIES");
        if (pcdmOntology == null) {
            pcdmOntology = ontologyServivce.put(testResource("ontologies/pcdm.ttl"));
        }

        if (oreOntology == null) {
            oreOntology = ontologyServivce.put(testResource("ontologies/ore.ttl"));
        }

        //
        if (remWithOrderedAggregationOntology == null) {
            remWithOrderedAggregationOntology = ontologyServivce.put(testResource(
                    "ontologies/RemWithOrderedAggregation.ttl"));
        }

        if (remWithOrderedAggregationExtension == null) {

            remWithOrderedAggregationExtension = extensionRegistry.put(testResource(
                    "objects/extension_rem_ordered_collection.ttl"));
        }

        if (testContainer == null) {
            try (FcrepoResponse response = client.post(URI.create(fcrepoBaseURI)).slug(this.getClass()
                    .getSimpleName())
                    .perform()) {
                testContainer = response.getLocation();
            }
        }

    }

    // If no ontology is declared in the extension, and the rdf:type of an object does not match
    // the binding class of an extension, then the extension should not be bound
    @Test
    public void noOntologyTest() throws Exception {

        final URI objectURI = putObjectFromResource("objects/object_with_ordered_collection.ttl");
        assertTrue(extensionBinding.getExtensionsFor(get(objectURI)).isEmpty());

    }

    // Extension should be bound to an object if it can be inferred that the object is a member of
    // the binding class. Here, we have an object with a PCDM collection with ordered members.
    // Our ontology defines a class for ORE aggregation ordered with proxies, and our extension binds
    // to objects of that class. So we should see that our object has our extensiion bound to it.
    @Test
    public void inferredBindingTest() {

    }

    private URI putObjectFromResource(String filePath) throws Exception {
        try (final WebResource object = testResource(filePath);
                final FcrepoResponse response = client.post(testContainer)
                        .body(object.representation(), object.contentType())
                        .slug(name.getMethodName())
                        .perform()) {
            return response.getLocation();
        }
    }

    private WebResource get(URI uri) {
        try {
            return new WebResource() {

                FcrepoResponse response = client.get(uri).perform();

                @Override
                public void close() throws Exception {
                    response.close();
                }

                @Override
                public URI uri() {
                    return uri;
                }

                @Override
                public InputStream representation() {
                    return response.getBody();
                }

                @Override
                public long length() {
                    return Long.valueOf(response.getHeaderValue(HttpHeaders.CONTENT_LENGTH));
                }

                @Override
                public String contentType() {
                    return response.getContentType();
                }
            };
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WebResource testResource(String path) {

        final File file = new File(testResources, path);
        try {
            return WebResource.of(new FileInputStream(file), "text/turtle", URI.create(FilenameUtils.getBaseName(
                    path)), null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
