# API-X Listener module

This module contains components that listen for repository events/messages and allow API-X to update its own internal indexes and caches.

## OSGi Deployment

This module is automatically deployed when the `fcrepo-api-x` feature is installed in Karaf.

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.apix.listener.cfg`. The following
values are available for configuration:

URI of the message topic or queue to consume from:

    input.uri=activemq:topic:fedora
    
URI of the message broker to connect to:

    broker.uri=tcp://localhost:61616
    
Fedora base URI:

    fcrepo.baseURI=http://localhost:8080/fcrepo/rest
