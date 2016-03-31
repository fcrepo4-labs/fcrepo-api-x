FROM apix-poc/java:latest

#####################
# Installs fuseki server from self-contained jar 
ENV FUSEKI_VERSION=2.3.1 \
    FUSEKI_HOME=${APPS}/apache-jena-fuseki-2.3.1 \
    FUSEKI_BASE=${SHARED}/fuseki-data \
    FUSEKI_JAR=fuseki-server.jar \
    FUSEKI_DEFAULT_DATASET=/fcrepo

RUN export FUSEKI_DIST=apache-jena-fuseki-${FUSEKI_VERSION}.tar.gz && \
    mkdir -p ${APPS} && \
    curl -L http://archive.apache.org/dist/jena/binaries/${FUSEKI_DIST} \
        > /tmp/${FUSEKI_DIST}  && \
    echo "ba7aea73f27a735489fb84ec0c7894a53bdc34f9 */tmp/${FUSEKI_DIST}" \
        | sha1sum -c - && \
    tar -C ${APPS} -xzvf /tmp/${FUSEKI_DIST} && \
    rm ${FUSEKI_HOME}/fuseki.war && \
    rm /tmp/${FUSEKI_DIST}


## Default command.
CMD mkdir -p ${FUSEKI_BASE} && \
    mkdir -p ${FUSEKI_BASE}/databases/${FUSEKI_DEFAULT_DATASET} && \
    java -jar ${FUSEKI_HOME}/${FUSEKI_JAR} \
    --update \
    --loc=${FUSEKI_BASE}/databases/${FUSEKI_DEFAULT_DATASET} \
    ${FUSEKI_DEFAULT_DATASET} \
    >> ${FUSEKI_BASE}/fuseki.log 2>&1