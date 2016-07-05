FROM apix-poc/java:latest
ENV MAVEN_VERSION=3.3.9 \
    MAVEN_HOME=/opt/apache/maven 

#RUN echo "@edge http://dl-cdn.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories && \
RUN     mkdir /mvn && \
	mkdir -p /opt/apache/maven && \
	apk --update --no-cache add git openjdk8 && \
	curl -fsSL http://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
        | tar -xzC /opt/ \
	&& ln -s /opt/apache-maven-${MAVEN_VERSION}/bin/mvn /usr/bin/mvn

WORKDIR /mvn
VOLUME /root/.m2
VOLUME /mvn
