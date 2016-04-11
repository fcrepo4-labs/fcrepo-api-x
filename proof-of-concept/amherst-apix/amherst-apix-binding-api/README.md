API-X Service Binding API
=========================

This is the Service Binding API for the API-X Framework.
A binding implementation needs only to implement this API.

Building
--------

To build this project use

    mvn install

Deploying in OSGi
-----------------

Each of these projects can be deployed in an OSGi container. For example using
[Apache Karaf](http://karaf.apache.org) version 4.x or better, you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.apix.poc/amherst-apix-karaf/LATEST/xml/features
    feature:install amherst-apix-binding-api

Or by copying any of the compiled bundles into `$KARAF_HOME/deploy`.

