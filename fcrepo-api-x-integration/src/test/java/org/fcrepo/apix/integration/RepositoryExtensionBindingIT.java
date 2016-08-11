
package org.fcrepo.apix.integration;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.fcrepo.apix.model.ExtensionBinding;
import org.fcrepo.apix.model.ExtensionRegistry;
import org.fcrepo.apix.model.OntologyService;
import org.fcrepo.apix.model.WebResource;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
public class RepositoryExtensionBindingIT implements KarafIT {

    @Inject
    BundleContext cxt;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Inject
    OntologyService ontologyServivce;

    @Inject
    ExtensionBinding extensionBinding;

    URI pcdmOntology;

    URI oreOntology;

    URI remWithOrderedAggregationOntology;

    URI remWithOrderedAggregationExtension;

    static final File testResources = new File(System.getProperty("project.basedir"), "src/test/resources");

    @Override
    public List<Option> additionalKarafConfig() {
        return Arrays.asList(
                editConfigurationFilePut("etc/system.properties", "fcrepo.dynamic.test.port", System.getProperty(
                        "fcrepo.dynamic.test.port")),
                editConfigurationFilePut("etc/system.properties", "project.basedir", System.getProperty(
                        "project.basedir")),
                deployFile("cfg/org.fcrepo.apix.jena.cfg"),
                deployFile("cfg/org.fcrepo.apix.registry.http.cfg"));
    }

    @Before
    public void loadOntologies() {

        System.err.println("LOADING ONTOLOGIES");
        if (pcdmOntology == null) {
            pcdmOntology = ontologyServivce.put(classPathResource("ontologies/pcdm.ttl"));
        }

        if (oreOntology == null) {
            oreOntology = ontologyServivce.put(classPathResource("ontologies/ore.ttl"));
        }

        if (remWithOrderedAggregationOntology == null) {
            remWithOrderedAggregationOntology = ontologyServivce.put(classPathResource(
                    "ontologies/RemWithOrderedAggregation.ttl"));
        }

        if (remWithOrderedAggregationExtension == null) {
            remWithOrderedAggregationExtension = extensionRegistry.put(classPathResource(
                    "objects/extension_rem_ordered_collection.ttl"));
        }

    }

    // If no ontology is declared in the extension, and the rdf:type of an object does not match
    // the binding class of an extension, then the extension should not be bound
    @Test
    public void noOntologyTest() throws Exception {

    }

    private WebResource classPathResource(String path) {

        final File file = new File(testResources, path);
        try {
            return WebResource.of(new FileInputStream(file), "text/turtle", URI.create(FilenameUtils.getBaseName(
                    path)), null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
