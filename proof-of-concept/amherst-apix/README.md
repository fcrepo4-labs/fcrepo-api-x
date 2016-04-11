Amherst College API-X proof of concept
===================================


This is a collection of OSGi services that implement an API-X proof-of-concept

Services
--------

* `amherst-apix-apix`: An implementation of the API-X framework
* `amherst-apix-registry-api`: An API for an API-X service registry
* `amherst-apix-registry-memory`: An in-memory implementation of the API-X service registry
* `amherst-apix-binding-api`: An API for an API-X service binding
* `amherst-apix-binding-memory`: An in-memory implementation of the API-X service binding mechanism

Building
--------

To build this project use

    mvn install

Deploying in OSGi
-----------------

Each of these projects can be deployed in an OSGi container. For example using
[Apache Karaf](http://karaf.apache.org) version 4.x and above, you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.apix.poc/amherst-apix-karaf/LATEST/xml/features
    feature:install amherst-apix
    feature:install amherst-apix-registry-api
    feature:install amherst-apix-registry-memory
    feature:install amherst-apix-binding-api
    feature:install amherst-apix-binding-memory

Or by copying any of the compiled bundles into `$KARAF_HOME/deploy`.


More information
----------------

For more information, please visit https://acdc.amherst.edu or https://acdc.amherst.edu/wiki/

