FROM apix-poc/java:latest

COPY target/*.jar ${APPS}/

ENV DATA_DIR=${SHARED}/poc-route-triplestore

CMD mkdir -p ${DATA_DIR} && java -jar ${APPS}/*.jar >> ${DATA_DIR}/stdout.log
