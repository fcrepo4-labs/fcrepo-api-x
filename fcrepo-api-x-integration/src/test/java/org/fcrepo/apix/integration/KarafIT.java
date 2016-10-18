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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Karaf + Pax Exam boilerplace.
 * <p>
 * This is an interface, in case the test needs to inherit some base test class.
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface KarafIT {

    Logger _log = LoggerFactory.getLogger(KarafIT.class);

    String fcrepoBaseURI = String.format("http://localhost:%s/%s/rest/", System.getProperty(
            "fcrepo.dynamic.test.port", "8080"), System.getProperty("fcrepo.cxtPath", "fcrepo"));

    File testResources = new File(System.getProperty("project.basedir"), "src/test/resources");

    FcrepoClient client = FcrepoClient.client().throwExceptionOnFailure().build();

    URI testContainer = URI.create(System.getProperty("test.container", ""));

    URI objectContainer = URI.create(testContainer + "/objects");

    URI extensionContainer = URI.create(System.getProperty("registry.extension.container", ""));

    URI serviceContainer = URI.create(System.getProperty("registry.service.container", ""));

    URI ontologyContainer = URI.create(System.getProperty("registry.ontology.container", ""));

    // What really sucks about pax exam is that this is called *ONLY ONCE, EVER*,
    // yet Karaf is entirely re-deployed from scratch *EVERY TEST*. There is no straightforward
    // way to configure separate service, ontology, or extension registry containers on a per-test basis.
    // Instead, the containers persist (and are shared) between tests. Had this been invoked every time
    // Karaf is configured and deployed, life would be much, much easier.
    @Configuration
    public default Option[] config() {
        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf").version(karafVersion()).type("zip");

        final MavenUrlReference apixRepo =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-karaf").versionAsInProject()
                        .classifier("features").type("xml");

        // This dependency is not in any features files, so we have to add it manually.
        final MavenArtifactUrlReference fcrepoClient = maven().groupId("org.fcrepo.client")
                .artifactId("fcrepo-java-client")
                .versionAsInProject();

        final String container = String.format("%s%s", fcrepoBaseURI, testClassName());

        final ArrayList<Option> options = new ArrayList<>();

        final Option[] defaultOptions = new Option[] {

            // Fcrepo client is not a dependency of anything else, but tests need it.
            // As this test runs as a maven bundle in Karaf, the test's reactor dependencies are not
            // available a priori.
            mavenBundle(fcrepoClient),

            karafDistributionConfiguration().frameworkUrl(karafUrl)
                    .unpackDirectory(new File("target", "exam")).useDeployFolder(false),
            // KarafDistributionOption.configureConsole().ignoreLocalConsole(),
            logLevel(LogLevel.WARN),

            // KarafDistributionOption.debugConfiguration("5005", true),

            keepRuntimeFolder(),

            features(apixRepo, "fcrepo-api-x"),

            // We need to tell Karaf to set any system properties we need.
            // This code is run prior to executing Karaf, the tests themselves are run in Karaf, in a separate
            // VM.
            editConfigurationFilePut("etc/system.properties", "apix.dynamic.test.port", System.getProperty(
                    "apix.dynamic.test.port")),
            editConfigurationFilePut("etc/system.properties", "fcrepo.dynamic.test.port", System.getProperty(
                    "fcrepo.dynamic.test.port")),
            editConfigurationFilePut("etc/system.properties", "services.dynamic.test.port", System.getProperty(
                    "services.dynamic.test.port")),
            editConfigurationFilePut("etc/system.properties", "project.basedir", System.getProperty(
                    "project.basedir")),
            editConfigurationFilePut("/etc/system.properties", "fcrepo.cxtPath", System.getProperty(
                    "fcrepo.cxtPath")),
            editConfigurationFilePut("/etc/system.properties", "test.container", container),
            editConfigurationFilePut("/etc/system.properties", "registry.extension.container", container +
                    "/extensions"),
            editConfigurationFilePut("/etc/system.properties", "registry.service.container", container +
                    "/services"),
            editConfigurationFilePut("/etc/system.properties", "registry.ontology.container", container +
                    "/ontologies"),

            deployFile("cfg/org.fcrepo.apix.jena.cfg"),
            deployFile("cfg/org.fcrepo.apix.registry.http.cfg"),
            deployFile("cfg/org.fcrepo.apix.routing.cfg"),
            deployFile("cfg/org.fcrepo.apix.listener.cfg")
        };

        options.addAll(Arrays.asList(defaultOptions));
        options.addAll(additionalKarafConfig());

        return options.toArray(defaultOptions);

    }

    /*
     * Use this to add additional karaf config options, or return an empty array for none
     */
    public default List<Option> additionalKarafConfig() {
        return new ArrayList<>();
    }

    public String testClassName();

    public String testMethodName();

    public static String karafVersion() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String karafVersion = cm.getProperty("pax.exam.karaf.version", "4.0.0");
        return karafVersion;
    }

    public default Option deployFile(String path) {
        try {
            return replaceConfigurationFile("deploy/" + new File(path).getName(),
                    new File("target/test-classes", path));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a test resource from test-classes
     *
     * @param path the resource path relative to {@link #testResources}
     * @return the resulting WebResource
     */
    public default WebResource testResource(String path) {

        final File file = new File(testResources, path);
        try {
            return WebResource.of(new FileInputStream(file), "text/turtle", URI.create(FilenameUtils.getBaseName(
                    path)), null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public default URI postFromTestResource(final String filePath, final URI intoContainer) throws Exception {

        try (final WebResource object = testResource(filePath);
                final FcrepoResponse response = client.post(intoContainer)
                        .body(object.representation(), object.contentType())
                        .slug(testMethodName())
                        .perform()) {
            return response.getLocation();
        }
    }

    /**
     * Create all necessary containers for registries, etc.
     * <p>
     * Tests that need functional registries need to do this <code>@BeforeClass</code>, or deploy alternate
     * configuration files for jena registry impls.
     * </p>
     *
     * @throws Exception when something goes wrong
     */
    public static void createContainers() throws Exception {

        for (final URI container : Arrays.asList(testContainer, objectContainer, extensionContainer,
                serviceContainer,
                ontologyContainer)) {
            // Add the container, if it doesn't exist.
            boolean initialized = false;

            while (!initialized) {
                try (FcrepoResponse head = client.head(container).perform()) {
                    initialized = true;
                } catch (final FcrepoOperationFailedException e) {
                    if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        try (FcrepoResponse response = client.put(container)
                                .perform()) {
                            if (response.getStatusCode() != HttpStatus.SC_CREATED && response
                                    .getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                                _log.info("Could not create container {}, retrying...", container);
                                try {
                                    Thread.sleep(1000);
                                } catch (final InterruptedException i) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
