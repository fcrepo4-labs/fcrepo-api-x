FROM pandorasystems/karaf

MAINTAINER Elliot Metsger <emetsger@jhu.edu>
LABEL description = "Provides a Karaf container configured with API-X features"

# The port Karaf will listen on when debugging is enabled
ENV DEBUG_PORT 5011
ENV JAVA_DEBUG_PORT ${DEBUG_PORT}
ENV MAVEN_REPO=/build/repository
# The version of API-X being included in the image
ENV APIX_VERSION LATEST
ENV KARAF_VERSION LATEST

# The Fedora repository base URI
ENV FCREPO_HOST=fcrepo
ENV FCREPO_PORT=8080
ENV FCREPO_CONTEXT_PATH=fcrepo
ENV FCREPO_PROXYURI=http://${FCREPO_HOST}:${FCREPO_PORT}/${FCREPO_CONTEXT_PATH}
ENV FCREPO_BASEURI=http://${FCREPO_HOST}:${FCREPO_PORT}/${FCREPO_CONTEXT_PATH}/rest
ENV ACTIVEMQ_HOST=activemq

# API-X base URI
ENV APIX_HOST=apix
ENV APIX_PORT=80
ENV APIX_LOADER_HOST=apix
ENV APIX_LOADER_PORT=8081
ENV APIX_PROXY_PATH=${FCREPO_CONTEXT_PATH}
ENV APIX_INTERCEPT_PATH=${FCREPO_CONTEXT_PATH}/rest
ENV APIX_BASEURI=http://${APIX_HOST}:${APIX_PORT}/${APIX_INTERCEPT_PATH}

EXPOSE ${APIX_PORT}
EXPOSE ${DEBUG_PORT}
EXPOSE ${APIX_LOADER_PORT}

# Allow the string "apix" to be recognized by Karaf as the Fedora API-X feature repository

RUN echo "apixrepo=mvn:org.fcrepo.apix/fedora-api-x-karaf/${APIX_VERSION}/xml/features" >> etc/org.apache.karaf.features.repos.cfg

# Add apixrepo as a registered features repository by default

RUN sed -e "s:^\(    mvn\:org.apache.karaf.features/enterprise/${KARAF_VERSION}/xml/features\):\1, \\\
    mvn\:org.fcrepo.apix/fcrepo-api-x-karaf/${APIX_VERSION}/xml/features \\\:" \
        -i etc/org.apache.karaf.features.cfg

# Update the Karaf logging configuration so that `bin/karaf` will echo logging
# to the console, viewable by `docker logs`

RUN sed -e "s/osgi:/stdout, osgi:/" -i etc/org.ops4j.pax.logging.cfg

# Copy API-X Karaf feature configuration files into the image
# (these files cannot be distributed with the Karaf feature due to issues with Karaf)

COPY repository/org/fcrepo/apix/*.cfg etc/

# Temporary for local development; copy any Maven artifacts into `maven/` that you want the image build process to see (e.g. locally build features that have yet to be published)
RUN mkdir -p ${MAVEN_REPO}
ADD repository/ ${MAVEN_REPO}

#RUN \
#  echo "eval 'alias mvn=\"mvn -Dmaven.repo.local=\${MAVEN_REPO}\"'" \
#    >> /etc/skel/.bashrc && \
#  echo "eval 'alias mvn=\"mvn -Dmaven.repo.local=\${MAVEN_REPO}\"'" \
#    >> ~/.bashrc

# Install all of the latest features found in the apixrepo features repository (typically these features will be SNAPSHOT versions) ...
# ... but do not create registry containers or attept to index registry content, as the Fedora repository is not available at image build time

RUN export REGISTRY_ONTOLOGY_CREATE=false  && \
    export REGISTRY_EXTENSION_CREATE=false && \
    export REGISTRY_SERVICE_CREATE=false   && \
    export REGISTRY_ONTOLOGY_INDEX=false   && \
    bin/start && \
    bin/client -r 10 -d 5 "feature:repo-add mvn:org.fcrepo.apix/fcrepo-api-x-karaf/${APIX_VERSION}/xml/features" && \
    bin/client -r 10 -d 5 "feature:install fcrepo-api-x" && \
    bin/stop

COPY repository/org/fcrepo/apix/docker-healthcheck.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-healthcheck.sh

COPY repository/org/fcrepo/apix/entrypoint.sh /entrypoint.sh
RUN chmod 700 /entrypoint.sh

ENTRYPOINT [ "/entrypoint.sh" ]

CMD [ "server" ]
