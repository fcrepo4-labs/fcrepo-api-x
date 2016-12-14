# API-X Jena module

This module contains [jena](jena.apache.org)-based implementations of the [ontology registry](../src/site/markdown/extension-definition-and-binding.md#ontology-registry), [ontology reasoning service](../src/site/markdown/extension-definition-and-binding.md#owl-reasoning), and various other internal API-X components

## OSGi Deployment

This module is automatically deployed when the `fcrepo-api-x` feature is installed in Karaf.

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.apix.jena.cfg`. The following
values are available for configuration:

Fedora URI of the container serving as an extension registry:

    registry.extension.ldp.container=http://localhost:8080/fcrepo/rest/apix/extensions
    
Fedora URI of the container serving as an ontology registry:
   
    registry.ontology.ldp.container=http://localhost:8080/fcrepo/rest/apix/ontologies
    
Fedora URI of the container serving as a service registry:

    registry.service.ldp.container=http://localhost:8080/fcrepo/rest/apix/services

Whether to create registries if not already present:

    registry.extension.create=true
    registry.ontology.create=true
    registry.service.create=true
    
Default content to populate the registry container with if API-X creates the container when not present:

    registry.service.content=classpath:/objects/service-registry.ttl
    
Whether to index ontologies by ontology IRI

    registry.ontology.index=true
    
Whether to persist all encountered ontologies to the repository

    registry.ontologies.persist=true
    
Whether to always persist ontologies as binary resources:

    registry.ontologies.binary=true
