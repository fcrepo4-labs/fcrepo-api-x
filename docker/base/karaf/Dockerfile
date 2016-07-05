FROM apix-poc/java:latest

ENV KARAF_HOME=${APPS}/apache-karaf  \
    KARAF_DEPLOY=${APPS}/apache-karaf/deploy \
    KARAF_SHARED=${SHARED}/karaf \
    KARAF_LOCAL_CONFIG=/etc/karaf/cfg

RUN export KARAF_VERSION=4.0.5 && \
    mkdir -p ${KARAF_LOCAL_CONFIG} && \ 
    curl -L http://archive.apache.org/dist/karaf/${KARAF_VERSION}/apache-karaf-${KARAF_VERSION}.tar.gz | tar xzf - -C ${APPS} && \
    ln -s ${APPS}/apache-karaf-${KARAF_VERSION} ${KARAF_HOME} && \
    cat ${KARAF_HOME}/etc/org.ops4j.pax.logging.cfg \
        | sed s/karaf.data/env:KARAF_SHARED/ \ 
        | sed 's/osgi:/stdout, osgi:/' \
        > /tmp/org.ops4j.pax.logging.cfg && \
    mv /tmp/org.ops4j.pax.logging.cfg ${KARAF_HOME}/etc/ && \
    cat ${KARAF_HOME}/etc/org.apache.felix.fileinstall-deploy.cfg \
        | sed 's/${karaf.base}\/deploy/\${env:KARAF_SHARED}\/deploy/' \
        > ${KARAF_HOME}/etc/org.apache.felix.fileinstall-shared.cfg
RUN ${KARAF_HOME}/bin/start && \
    echo "" | ${KARAF_HOME}/bin/client -u karaf -b -r 20 && \
    ${KARAF_HOME}/bin/stop && \
    rm -rf ${KARAF_HOME}/instances/* && \
    chmod a+rw -R ${KARAF_HOME}/data ${KARAF_HOME}/etc ${KARAF_HOME}/instances ${KARAF_HOME}/lock

# Copy local config to the shared deploy dir, and run karaf
# We do this so that any custom config files placed into the image
# are shared and editable, which can be helpful for development
CMD mkdir -p ${KARAF_SHARED}/deploy && \
    find ${KARAF_LOCAL_CONFIG}/* \
       | xargs -I{} echo "false | cp -i {} ${KARAF_SHARED}/deploy" | sh && \
    ${KARAF_HOME}/bin/karaf
