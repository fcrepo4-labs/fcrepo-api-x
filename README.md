# API-X development

This repository contains development and proof-of-concept code for API-X.  API-X may
be demonstrated in action by running [Docker Compose](https://docs.docker.com/compose/overview/) or
[Vagrant](https://www.vagrantup.com/about.html).  Docker Compose and Vagrant offer two distinct
approaches for running relevant services (e.g. fedora itself, message routing,
API-X extensions, etc) inside [Docker](https://www.docker.com/what-docker) containers in order to test, develop, or measure API-X in action.
Docker-compose offers a high degree of control which more suited towards testing and development,
while Vagrant is extremely easy to use and perhaps best suited for quickly running a demonstration.

## Prerequisites 

Building the code requires Maven 3.x, and Java 8.

Running the various API-X services in Docker requires installing additional software:

### Vagrant
Running API-X in Docker managed by Vagrant (e.g. for demonstration purposes) requires Vagrant
and Virtualbox.  Linux users can avoid installing Virtualbox if they use their own local
installed Docker engine to run containers.
* [Vagrant Download](https://www.vagrantup.com/downloads.html) (tested on version 1.8.1)
* [VirtualBox Download](https://www.virtualbox.org/wiki/Downloads) (tested on version 5.0.14 for OS X hosts, amd64; _not_ the extension pack)

### Docker Compose
Docker compose orchestrates building and running a ser of docker images via communicating
directly with a Docker daemon.  Linux hosts that have Docker running locally can just use
docker compose directly to provision containers.  Users of MacOS or Windows (or Linux users
that do not have docker installed locally, or do not want to use it) need to install
Docker Machine, which manages Docker containers in a Virtualbox instance.  Because several
cooperating tools are required to make this work (docker, docker-compose, docker-machine, and virtualbox),
MacOS and Windows users are better off installing Docker Toolbox, which contains all
necessary tools in a single installation package.
* [Docker Toolbox Download](https://www.docker.com/products/docker-toolbox) for MacOS and Windows
* Linux users (and adventurous MacOS users) can separately install:
    * [Docker](https://docs.docker.com/engine/installation/linux/)
    * [Docker Compose](https://docs.docker.com/compose/install/)
    * [Docker Machine](https://docs.docker.com/machine/install-machine/) (for specifically running in a VM instead of locally)
    * [VirtualBox](https://www.virtualbox.org/wiki/Downloads)


## Quickstart

### Vagrant
By default, this will launch a Virtualbox VM running Ubuntu 14.04 LTS (a "docker host VM") and will run docker
images within this VM.  Additionally, this VM will expose ports on your local machine (e.g. for viewing with a web
browser).  This pattern works on all platforms (Linux, Mac OS, and Windows).  Linux hosts may choose to configure
Vagrant to launch Docker on their own machine, rather than launching a separate VM.  This is discussed in a separate
section.

* `mvn clean install`-PPOC
    This is necessary because none of the apix-poc images or artifacts are published to
    a repository for download; everything needs to be build from scratch locally.
* `vagrant up`
* try `vagrant up` again if something fails (e.g. failed download)

For more information see [vagrant.md](vagrant.md)

### Docker Compose
On Linux machines with a docker engine running, Docker Compose can be used to run api-x docker images directly on
the local machine.  Otherwise, docker may be run inside of a virtual machine.

* For Mac, Windows, and Linux users wishing to run docker in a VM, start docker machine.  Linux users who
  have Docker engine (> 1.10) on their local machine can skip this step
    * `docker-machine create -d virtualbox --virtualbox-memory "2048" apix`
    * `eval $(docker-machine env apix)`
    * _Make sure that the api-x code (the fcrepo-api-x) directory is accessible to the docker machine VM_.  For
    mac users, if the code is in your home sirectory somewhere under /Users, then you're good.  Linux users
    have some work to do, see [setting up docker machine](docker/README.md)
* run `mvn clean install -PPOC` to build the api-x code
* run `docker-compose up -d` to build & run docker images of API-X services.

For more information see [docker/README.md](docker/README.md)

## Verification
These verification steps can be used to assure that the docker instance(s) are running and are accessible.

First, determine the address `${ADDR}` of the machine running Docker
* For Vagrant users, this is pre-configured to be `10.11.8.11`
* For Docker Compose users:
	* For Linux duning services directly on a local docker, this is `localhost`
	* For Docker Machine users, you can find this by running `docker-machine ip apix`

Next, verify that some services are running
* Verify Fedora is running. Point your browser to `http://${ADDR}:8080/fcrepo/rest`
	* You should see Fedora, with an empty root container
	* Create an object through Fedora's UI
* point your browser to Fuseki at `http://${ADDR}:3030/fcrepo-triple-index`
	* You should see some triples!
	* Note, ths indexing process is asynchronous, so there may be a delay before these are visible
* Look at the Fedora as seen through the Amherst API-X proof of concept `http://${ADDR}:8081/fcrepo/rest`
* Look at Fedora as seen through a simple reverse proxy service `http://${ADDR}:8082/fcrepo/rest`

## Contributing
Contributors to DuraSpace projects should complete a [Contributor License Agreement](https://wiki.duraspace.org/x/ILsQAg)
or be covered by a corporate agreement.

## License
[Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
