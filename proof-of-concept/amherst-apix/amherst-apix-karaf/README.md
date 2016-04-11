Karaf Provisioning Feature
==========================

A karaf provisioning feature for the modules contained in this repository.


Deploying in OSGi
-----------------

Each of these projects can be deployed in an OSGi container. For example using
[Apache ServiceMix](http://servicemix.apache.org/) or
[Apache Karaf](http://karaf.apache.org), you can run the following
command from its shell:

    feature:repo-add mvn:org.fcrepo.apix.poc/amherst-apix-karaf/LATEST/xml/features
    feature:install <name of module>

