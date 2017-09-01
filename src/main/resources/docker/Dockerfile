FROM pandorasystems/karaf:${KARAF_VERSION}

MAINTAINER Christopher Johnson <christopher_hanna.johnson@uni-leipzig.de>
LABEL description = "Provides a Karaf container configured with API-X features"

# The port Karaf will listen on when debugging is enabled
ENV DEBUG_PORT=${DEBUG_PORT}
ENV JAVA_DEBUG_PORT=${DEBUG_PORT}
ENV MAVEN_REPO=/build/repository
# The version of API-X being included in the image
ENV APIX_VERSION=${version}

# The Fedora repository base URI
ENV FCREPO_HOST=${FCREPO_HOST}
ENV FCREPO_PORT=${FCREPO_PORT}
ENV FCREPO_CONTEXT_PATH=${FCREPO_CONTEXT_PATH}
ENV FCREPO_PROXYURI=http://${FCREPO_HOST}:${FCREPO_PORT}/${FCREPO_CONTEXT_PATH}
ENV FCREPO_BASEURI=http://${FCREPO_HOST}:${FCREPO_PORT}/${FCREPO_CONTEXT_PATH}/rest
ENV ACTIVEMQ_HOST=${ACTIVEMQ_HOST}

# API-X base URI
ENV APIX_HOST=${APIX_HOST}
ENV APIX_PORT=${APIX_PORT}
ENV APIX_LOADER_HOST=${APIX_LOADER_HOST}
ENV APIX_LOADER_PORT=${APIX_LOADER_PORT}
ENV APIX_PROXY_PATH=${FCREPO_CONTEXT_PATH}
ENV APIX_INTERCEPT_PATH=${FCREPO_CONTEXT_PATH}/rest
ENV APIX_BASEURI=http://${APIX_HOST}:${APIX_PORT}/${APIX_INTERCEPT_PATH}

# REGISTRY
ENV REGISTRY_ONTOLOGY_CREATE=${REGISTRY_ONTOLOGY_CREATE}
ENV REGISTRY_EXTENSION_CREATE=${REGISTRY_EXTENSION_CREATE}
ENV REGISTRY_SERVICE_CREATE=${REGISTRY_SERVICE_CREATE}
ENV REGISTRY_ONTOLOGY_INDEX=${REGISTRY_ONTOLOGY_INDEX}

EXPOSE ${APIX_PORT}
EXPOSE ${DEBUG_PORT}
EXPOSE ${APIX_LOADER_PORT}

COPY repository/org/fcrepo/apix/*.cfg etc/

# Temporary for local development; copy any Maven artifacts into `maven/` that you want the image build process to see (e.g. locally build features that have yet to be published)
RUN mkdir -p ${mavenRepo}
ADD repository/ ${mavenRepo}

RUN bin/start && \
bin/client -r 10 -d 5 "feature:repo-add mvn:org.fcrepo.apix/fcrepo-api-x-karaf/${version}/xml/features" && \
bin/client -r 10 -d 5 "feature:install fcrepo-api-x" && \
bin/stop

COPY repository/org/fcrepo/apix/docker-healthcheck.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-healthcheck.sh

COPY repository/org/fcrepo/apix/entrypoint.sh /entrypoint.sh
RUN chmod 700 /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]

CMD [ "server" ]