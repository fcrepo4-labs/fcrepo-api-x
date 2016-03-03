# API Extension Architecture proof of concept
This is a working proof of concept of a possible implementation of the Fedora API Extension Architecture (API-X).

It is written in PHP for portability and simplicity, however other languages should be considered for a real implementation.

Please read through the code. There are abundant comments explaining what each component does. The most relevant components are the confguration files.

This PoC is focused on the synchronous validation use case diagram found in the doc/pdf folder and consists of the following parts:

- An API core (`core.php`). This is the entry point for incoming requests. The core routes requests through external services that valaidate or manipulate data otherwise.
- An extension configuration (`ext.conf.inc`). This file defines the individual extensions and what each of them does.
- A route configuration (`ext_routing.conf.inc`). This is where extensions are put together in "workflows" or "routes".
- Services and their specific configurations (`services/*`). These are called by the core based on the extension configuration.

## Requirements
- PHP >=5.4
- Bash

## Usage
After cloning the repository, set the startup script executable and run it:

    chmod a+x servers.sh
    ./servers.sh start

This will spin up three HTTP servers:

- An API-X Core at http://localhost:9800
- A validation service at http://localhost:9801
- A mock Fedora repository at http://localhost:9802

You can change the server ports by changing the `CORE_PORT`, `SVC_PORT` and `REPO_PORT` environment variables in `servers.sh`.

To stop the servers:

    ./servers.sh stop

## Testing

Send some sample metadata:

    curl -i -X POST --data-binary 'properties={"skos:prefLabel" : ["This is the main title."], "rdfs:label" : ["This is another title", "This is yet another title"], "rdfs:comment" : ["Comment1" , "Comment2"]}' http://localhost:9800

This should send a 201 back and write the metadata into a dump file. The dump file is located in `TMP_DIR/apix_poc.fcrepo.out` where `TMP_DIR` is `/tmp` by default. This can be changed in `servers.sh` too.

Now send some invalid metadata:

    curl -i -X POST --data-binary 'properties={"skos:prefLabel" : ["This is the main title.", "This is another main title"], "rdfs:label" : ["This is another title", "This is yet another title"], "rdfs:comment" : ["Comment1" , "Comment2"]}' http://localhost:9800

The cardinality rule mandates that there is one and only one value for `skos:prefLabel` so the request should fail with a 400. 

    curl -i -X POST --data-binary 'properties={"skos:prefLabel" : ["This is the main title."], "rdfs:label" : ["This is another title", "This is yet another title", 25], "rdfs:comment" : ["Comment1" , "Comment2"]}' http://localhost:9800

The datatype validation rules requiers `rdfs:label` to be a string, so this will result in another 400. 


