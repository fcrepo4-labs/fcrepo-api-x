# API Extension Architecture (API-X)

[![Build Status](https://travis-ci.org/fcrepo4-labs/fcrepo-api-x.png?branch=master)](https://travis-ci.org/fcrepo4-labs/fcrepo-api-x)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.apix/fcrepo-api-x/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.fcrepo.apix/fcrepo-api-x/)

API-X is a framework for binding services to repository objects in order to extend the functionality of a Fedora 4 repository. It provides a data model for expressing how services bind to repository resources, registries that support service discovery, and an HTTP middleware layer that exposes services as endpoints or mediates access to the repository transparently via these services.

For more information, please see the [technical overview](src/site/markdown/apix-design-overview.md) or try out a docker based [demo](https://github.com/fcrepo4-labs/fcrepo-api-x-demo#summary) that highlights what API-X is and does by providing step-by-step evaluation tasks.

## Building

To build API-X, do:

    mvn clean install

## OSGi deployment (Karaf 4.x)

API-X is distributed as OSGi bundles and provides Karaf feature descriptors, making it easy to deploy in a Karaf container. There are several ways to install these features, please refer to the [karaf provisioning documentation](https://karaf.apache.org/manual/latest/#_provisioning) for more information.  

### Manual installation in Karaf
To do this, first, add the `fcrepo-api-x-karaf` repository in the Karaf console:

    $ feature:repo-add mvn:org.fcrepo.apix/fcrepo-api-x-karaf/LATEST/xml/features

Then, run

    $ feature:install fcrepo-api-x

This will start an instance of API-X running on default settings.  API-X will presume there is a Fedora instance at `http://localhost:8080/fcrepo/rest`.  See individual module documentation for how to override the defaults.


## Contributing
Contributors to DuraSpace projects should complete a [Contributor License Agreement](https://wiki.duraspace.org/x/ILsQAg)
or be covered by a corporate agreement.

## License
[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
