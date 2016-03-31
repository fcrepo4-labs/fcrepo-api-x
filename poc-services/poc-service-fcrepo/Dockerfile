FROM apix-poc/java:latest

#####################
# Runs Fedora via self-contained jar 
ENV FCREPO_VERSION=4.5.0 \
    DATA_DIR=${SHARED}/fcrepo-data \
    FCREPO_PORT=8080
    
RUN export FCREPO_JAR=fcrepo-webapp-${FCREPO_VERSION}-jetty-console.jar && \
    mkdir -p ${APPS} && \
    curl -L https://github.com/fcrepo4/fcrepo4/releases/download/fcrepo-${FCREPO_VERSION}/${FCREPO_JAR} \
        > ${APPS}/${FCREPO_JAR}

CMD mkdir -p ${DATA_DIR} && \ 
    java -Dfcrepo.home=${DATA_DIR} \
    -jar ${APPS}/fcrepo-webapp-${FCREPO_VERSION}-jetty-console.jar \
    --port ${FCREPO_PORT} \
    --headless 2>&1 \
    | tee -a ${DATA_DIR}/fcrepo.log
