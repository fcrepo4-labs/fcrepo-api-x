
# API-X Integration tests

To run integration tests, simply do 

    mvn clean install
    
The integration tests use `cargo` to deploy an instance of Fedora.  After tests have completed, the Fedora logs can be found in

    fcrepo-api-x-integration/target/cargo/configurations/jetty9x/logs/
    
You can also run the same Fedora instance with all test data intact by

    cd fcrepo-api-x-integration
    mvn cargo:run -Pstandard
    
This will run Fedora at port 8080

## Relevant directories:
* `target/fcrepo/` - Fedora's data store
* `target/cargo/configurations/jetty9x/` - Jetty installation which runs fedora.  Contains logs in Jetty's standard location
* `target/exam/{uuid}/` - Karaf installation directory

## Pax Exam
Pax exam runs integration tests are run in a Karaf container.  This has a few consequences and nuances:
- Specific dependency jars (libraries, etc) are specified by _Karaf feature_ and retrieved from the local maven repository rather than the build reactor.  Karaf will load all dependencies listed in features files, and normal OSGi mechanisms will verify that
all dependencies are satisfied.
    - The set of OSGi dependencies may be different from non-osgi, which is why it's necessary to specifically test OSGi
    - To add, remove, or update dependencies in a manner that is visible to Karaf, you have to track down the correct features file and edit it appropriately
    - Karaf feature files are produced by artifacts in the project, it is necessary to include them in the pom like <pre>&lt;dependency&gt;
     - All dependencies from other modules must have been installed in the local maven repo by `mvn install`.  So running `mvn verify` or `mvn package` from the toplevel directtory is likely to fail, as any artifacts produced by them will not be available to pax exam.
- Tests are packaged into an OSGi bundle and run in Karaf
    - This means that any test dependencies that aren't already in OSGi through a feature or Pax Exam itself must be manually added as a bundle in the karaf configuration.
    - Karaf is re-launched _for every test_.  This means that instance or class variables for an integration test do not persist across tests unless they are injected or re-computed.

