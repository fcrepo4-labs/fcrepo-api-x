# API-X proof-of-concept services

Right now, the Karaf and PHP docker images are in-progress, so
there aren't any API-X specific services here at the moment.  There is,
however, a working core of Fedora4, Fuseki, and a camel route connecting the two

## fedora
Vagrant baseURI: [http://10.11.8.11:8080/rest](http://10.11.8.11:8080/rest)

## route-indexing-triplestore
This is just the route from fcrepo-camel-toolbox, it does not expose
any web service

## fuseki
Vagrant baseURI: [http://10.11.8.11:3030](http://10.11.8.11:3030)
Vagrant fcrepo SPARQL endpoint [http://10.11.8.11:3030/fcrepo](http://10.11.8.11:3030/fcrepo)