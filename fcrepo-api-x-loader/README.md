Facilitates self- or manual registration of extensions.

Given the URI of service endpoint, will attempt to perform an `OPTIONS` request against that URI.  If the service responds with a resource containing an [extension definition](../src/site/markdown/extension-definition-and-binding.md#extension-definition) and/or a [service](../src/site/markdown/service-discovery-and-binding.md) description, it will load (or update) these into the appropriate registries, and register the given service endpoint URI as an available service  instance endpoint.

# Usage
Exposes an endpoint `${loader.host}:${loader.port}/load` that responds to `POST` and `GET`

- `GET`: returns an html document with a text field so that a user can manually enter a service endpoint URI
- `POST` accepts a `application/x-www-form-urlencoded` form posting expecting parameter `service.uri`, or `text/plain` with the uri in the request body
    - If successful, it will return a `303` redirect to the corresponding LDP resource in the extension or service registry.

## Implementation notes
- The given service endpoint will only be registered if it is unambiguous.  That is to say, if the extension/service document describes or references a single service.
- Extension documents are owl2 documents, and thus may embed ontology statements such as binding class definitions.  When this happens, the extension document will be ingested into Fedora as a _binary_ resource (i.e. a literal document, not an object).  This is to accommodate commonly used constructs such as blank nodes in a manner that is directly compatible with standard owl tooling such as reasoners, and is easy to understand.  
- If a service/extension document contains embedded ontology statements, the `apix:bindsTo` relation MUST contain a URI and not a blank node.  For example: <pre>
@prefix apix:&lt;http://fedora.info/definitions/v4/api-extension#&gt; .
@prefix owl:&lt;http://www.w3.org/2002/07/owl#&gt; .
@prefix ebucore:&lt;http://www.ebu.ch/metadata/ontologies/ebucore/ebucore#&gt; .
@prefix fedora:&lt;http://fedora.info/definitions/v4/repository#&gt; .
&nbsp;
&lt;&gt; a apix:Extension;
    apix:exposesService &lt;http://example.org/LoaderIT/ont&gt;;
    apix:exposesServiceAt "test:LoaderIT/ont";
    <b>apix:bindsTo &lt;#class&gt; </b>.
&nbsp;
&lt;#class&gt; owl:unionOf (
        fedora:Binary
        [ a owl:Restriction; owl:onProperty ebucore:mimeType; owl:hasValue "text/plain" ]
        [ a owl:Restriction; owl:onProperty ebucore:mimeType; owl:hasValue "text/html" ] ) .
</pre>

# Deployment in Karaf
`feature:repo-add mvn:org.fcrepo.apix/fcrepo-api-x-karaf/LATEST/xml/features`
This service is installed implicitly via `feature:install fcrepo-api-x`
This service is installed manually via `feature:install fcrepo-api-x-loader`

## Configuration
- `org.fcrepo.apix.loader.cfg`
    - `loader.port` the port the loader shall listen on
    - `loader.host` the loader host/interface to bind to.  Use `0.0.0.0` to bind to all interfaces, and accept traffic from anywhere

