
package org.fcrepo.apix.integration;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;

public interface KarafIT {

    static final String fcrepoBaseURI = String.format("http://localhost:%s/%s/rest/", System.getProperty(
            "fcrepo.dynamic.test.port"), System.getProperty("fcrepo.cxtPath", "fcrepo"));

    static final File testResources = new File(System.getProperty("project.basedir"), "src/test/resources");

    @Configuration
    public default Option[] config() {
        final MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf").version(karafVersion()).type("zip");

        final MavenUrlReference apixJena =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-jena").versionAsInProject()
                        .classifier("features").type("xml");

        final MavenUrlReference apixRegistry =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-registry").versionAsInProject()
                        .classifier("features").type("xml");

        final MavenUrlReference apixBinding =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-binding").versionAsInProject()
                        .classifier("features").type("xml");

        final MavenUrlReference camelRepo = maven().groupId("org.apache.camel.karaf")
                .artifactId("apache-camel").type("xml").classifier("features")
                .versionAsInProject();

        final ArrayList<Option> options = new ArrayList<>();

        final Option[] defaultOptions = new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl)
                    .unpackDirectory(new File("target", "exam")),
            // configureConsole().ignoreLocalConsole(),
            logLevel(LogLevel.WARN),

            keepRuntimeFolder(),

            features(apixJena, "fcrepo-api-x-jena"),
            features(camelRepo, "camel-http4"),
            features(apixRegistry, "fcrepo-api-x-registry"),
            features(apixBinding, "fcrepo-api-x-binding"),
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
            e.printStackTrace(System.err);
            throw new RuntimeException(e);
        }
    }

}
