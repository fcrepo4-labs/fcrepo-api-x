FROM alpine:3.4

ENV SHARED=/shared \
    APPS=/opt \
    JAVA_HOME=/usr/lib/jvm/default-jvm \
    LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/usr/lib/jvm/default-jvm/jre/lib/amd64:/usr/lib/jvm/default-jvm/jre/lib/amd64/jli:/usr/lib/jvm/default-jvm/jre/lib/amd64/server

#Basic setup: Install java, curl
RUN apk add --update openjdk8-jre && \
    apk add --update curl && \
    apk add --update bash && \
    rm -rf /var/cache/apk/* && \
    mkdir ${SHARED} && \
    mkdir ${APPS}  && \ 
    chmod a+rwx ${SHARED}
