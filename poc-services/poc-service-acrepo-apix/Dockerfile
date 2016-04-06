FROM apix-poc/karaf:latest

ENV APIX_REST_HOST=localhost \
    APIX_REST_PORT=13431 \
    APIX_REST_PROXY=/fcrepo/rest \
    APIX_REST_BINDING=/apix/bind \
    APIX_REST_REGISTRY=/apix/registry \
    APIX_PREFIX=svc: \
    FCREPO_BASEURL=localhost:8080/rest \
    APIX_POC_VERSION=0.0.1-SNAPSHOT

COPY cfg/*.cfg ${KARAF_LOCAL_CONFIG}
COPY cfg/*.cfg ${KARAF_HOME}/etc
COPY poc-service-acrepo-apix-dist/target/feature.xml poc-service-acrepo-apix-dist/target/poc-service-acrepo-apix-dist-${APIX_POC_VERSION}.kar /tmp/
    
RUN ${KARAF_HOME}/bin/start && \
    echo "kar:install file:/tmp/poc-service-acrepo-apix-dist-${APIX_POC_VERSION}.kar; " \
          | ${KARAF_HOME}/bin/client -u karaf -b -r 10 ; \ 
    echo "feature:repo-add file:/tmp/feature.xml; " \
          "feature:install camel-http4; " \
          "feature:install camel-blueprint; " \
          "feature:install camel-jetty9; " \
          "feature:install camel-jackson; " \
          "feature:install fcrepo-camel; " \
          "feature:install acrepo-jsonld-service; " \
          "feature:install acrepo-registry-api; " \
          "feature:install acrepo-registry-memory; " \
          "feature:install acrepo-binding-api; " \
          "feature:install acrepo-binding-memory; " \
          "feature:install acrepo-apix; " \
	  | ${KARAF_HOME}/bin/client -u karaf -b -r 10 &&  \
         ${KARAF_HOME}/bin/stop
