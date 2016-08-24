FROM apix-poc/java:latest

#####################
# Installs fuseki server from self-contained jar 
ENV FUSEKI_VERSION=2.3.1 \
    FUSEKI_HOME=${APPS}/apache-jena-fuseki-2.3.1 \
    FUSEKI_BASE=${SHARED}/fuseki-data \
    FUSEKI_JAR=fuseki-server.jar \
    FUSEKI_DEFAULT_DATASET=/fcrepo-triple-index

RUN export FUSEKI_DIST=apache-jena-fuseki-${FUSEKI_VERSION}.tar.gz && \
    mkdir -p ${APPS} && \
    curl -L http://archive.apache.org/dist/jena/binaries/${FUSEKI_DIST} \
        > /tmp/${FUSEKI_DIST}  && \
    echo "ba7aea73f27a735489fb84ec0c7894a53bdc34f9 */tmp/${FUSEKI_DIST}" \
        | sha1sum -c - && \
    tar -C ${APPS} -xzvf /tmp/${FUSEKI_DIST} && \
    rm ${FUSEKI_HOME}/fuseki.war && \
    rm /tmp/${FUSEKI_DIST}


# Start fuseki.  For some reasonm shutdowns are not clean, so remove lock file if it exists
CMD mkdir -p ${FUSEKI_BASE} && \
    mkdir -p ${FUSEKI_BASE}/databases/${FUSEKI_DEFAULT_DATASET} && \
    find ${FUSEKI_BASE} -name *.lock | xargs -r rm  && \
    java -jar ${FUSEKI_HOME}/${FUSEKI_JAR} \
    --update \
    --loc=${FUSEKI_BASE}/databases/${FUSEKI_DEFAULT_DATASET} \
    ${FUSEKI_DEFAULT_DATASET} 2>&1 \
    | tee -a ${FUSEKI_BASE}/fuseki.log 
