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

import org.apache.commons.io.FilenameUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;

public interface KarafIT {

    static final String fcrepoBaseURI = String.format("http://localhost:%s/%s/rest/", System.getProperty(
            "fcrepo.dynamic.test.port", "8080"), System.getProperty("fcrepo.cxtPath", "fcrepo"));

    static final File testResources = new File(System.getProperty("project.basedir"), "src/test/resources");

    static final FcrepoClient client = FcrepoClient.client().throwExceptionOnFailure().build();

    @Configuration
    public default Option[] config() {
        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf").version(karafVersion()).type("zip");

        final MavenUrlReference apixRepo =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-karaf").versionAsInProject()
                        .classifier("features").type("xml");

        // final MavenUrlReference camelRepo = maven().groupId("org.apache.camel.karaf")
        // .artifactId("apache-camel").type("xml").classifier("features")
        // .versionAsInProject();

        final ArrayList<Option> options = new ArrayList<>();

        final Option[] defaultOptions = new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl)
                    .unpackDirectory(new File("target", "exam")),
            // configureConsole().ignoreLocalConsole(),
            logLevel(LogLevel.WARN),

            keepRuntimeFolder(),

            features(apixRepo, "fcrepo-api-x")
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

    /** Get a test resource from test-classes */
    public default WebResource testResource(String path) {

        final File file = new File(testResources, path);
        try {
            return WebResource.of(new FileInputStream(file), "text/turtle", URI.create(FilenameUtils.getBaseName(
                    path)), null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
