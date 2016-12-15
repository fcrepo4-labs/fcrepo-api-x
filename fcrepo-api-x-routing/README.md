# API-X Routing module

This module contains the API-X [routing](../src/site/markdown/execution-and-routing.md#routing) and [generic](../src/site/markdown/execution-and-routing.md#generic-endpoint-proxy) [implementations](../src/site/markdown/execution-and-routing.md#generic-intercepting-proxy) of execution components.

## OSGi Deployment

This module is automatically deployed when the `fcrepo-api-x` feature is installed in Karaf.

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.apix.routing.cfg`. The following
values are available for configuration:

Host name to use when linking to API-X URIs.  Usually, this will be a registered DNS name (e.g. `archive.example.edu`)

    apix.host=127.0.0.1
    
Port to listen on for API-X

    apix.port=8081

Host interface to listen for API-X connections.  `0.0.0.0` listens on all interfaces.

    apix.listen.host=0.0.0.0
    
Context path for API-X service discovery documents

    apix.discoveryPath=discovery
    
Context path for API-X exposed service endpoints

    apix.exposePath=services
    
Context path for API-X intercepting.  NOTE:  For best results, this should be identical to the context and servlet path componenent of fedora.  So if Fedora is `http://host:port/fcrepo/rest`, this should be `fcrepo/rest`.  See [URIs in API-X](../src/site/markdown/uris-in-apix.md#implementation-notes) for more information.  Any URI containing this path will be considered a candidate for interception by an intercepting extension
    
    apix.interceptPath=fcrepo/rest

Proxy path.  API-X will proxy requests under this URI path, but *not* necessarily apply intercepting extensions.

    apix.proxyPath=fcrepo
    
Fedora Prooxy URI.  API-X will proxy requests under to Fedora this URI

    apix.proxyURI=http://localhost:8080/fcrepo
    
Fedora baseURI.  This is the URI of the Fedora root resource.

    fcrepo.baseURI=http://localhost:8080/fcrepo/rest
    
Use relative URIs in service discovery documents, when possible.

      discovery.relativeURIs=true
      
Use proxy/intercept URIs for fedora resources.  If this is 'false', all repository resource URIs in discovery documents will point to the fedora repository directly (and therefore will not be proxied via API-X)

      discovery.interceptURIs=true
