FROM apix-poc/java:latest

ENV PATH=${PATH}:${APPS}/apache-maven/bin \
    USER_HOME=/tmp

RUN export MVN_VERSION=3.3.9 && \
    mkdir -p ${APPS} && \
    apk add --update openjdk8 && rm -rf /var/cache/apk/* && \
    cd ${APPS} && \
    curl -O http://www.motorlogy.com/apache/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz && \
    tar xzvf apache-maven-${MVN_VERSION}-bin.tar.gz && \
    ln -s apache-maven-${MVN_VERSION} apache-maven
    
CMD mvn -Duser.home=${USER_HOME} install