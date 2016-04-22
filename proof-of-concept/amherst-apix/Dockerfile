FROM apix-poc/karaf:latest

ENV APIX_REST_HOST=localhost \
    APIX_REST_PORT=13431 \
    APIX_REST_PROXY=/fcrepo/rest \
    APIX_REST_BINDING=/apix/bind \
    APIX_REST_REGISTRY=/apix/registry \
    APIX_PREFIX=svc: \
    FCREPO_BASEURL=localhost:8080/rest \
    APIX_POC_VERSION=0.0.1-SNAPSHOT

COPY cfg/*.cfg amherst-apix-karaf/target/*.kar ${KARAF_HOME}/deploy/
    
# Doesn't seem to be an other way to assure all features and 
# dependencies are installed.  kar:install has NullPointerException bug,
# When using Camel snapshot versions, it appears that Spring needs to be
# explicitly enabled via feature:install
RUN ${KARAF_HOME}/bin/start && \
    echo "service:wait -t 60000 org.fcrepo.apix.poc.amherst.binding.api.BindingService " \
      | ${KARAF_HOME}/bin/client -u karaf -b -r 10 ; \
    echo "feature:install spring spring-tx amherst-apix-core "\
        | ${KARAF_HOME}/bin/client -u karaf -b -r 10 ; \
    echo "service:wait -t 60000 org.apache.camel.CamelContext; " \
          | ${KARAF_HOME}/bin/client -u karaf -b -r 10 ; \ 
    ${KARAF_HOME}/bin/stop && \
    chmod a+rw -R ${KARAF_HOME}/data ${KARAF_HOME}/etc ${KARAF_HOME}/instances ${KARAF_HOME}/lock && \
    rm -rf ~/.m2
