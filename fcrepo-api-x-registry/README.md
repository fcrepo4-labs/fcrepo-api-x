# API-X registry/http module

This module contains base implementations of internal API-X registries and HTTP clients.  These influence how API-X itself (as a client) interacts with Fedora or other web resources in order to persist or look up extension definitions, ontologies, service instances, etc.  Low-level connection
details such as timeouts, Authn username/password, etc are configured here.

By default, API-X uses a non-authenticated HttpClient to interact with Fedora.  Timeout and Authn credentials for basic authentication can be optionally provided to configure this default http client.  In many cases, this is sufficient to allow API-X to use an authn-protected Fedora instance for persisting its own internal registries.  

In cases where this internal client is not sufficient, a pre-configured [HttpClient](https://hc.apache.org/httpcomponents-client-ga/tutorial/html/index.html) can be provided for API-X to use.  

## OSGi Deployment

This module is automatically deployed when the `fcrepo-api-x` feature is installed in Karaf.

To provide an alternative `HttpClient` implementation for API-X to use when communicating with Fedora, publish a `org.apache.http.client.HttpClient` service to the OSGi registry.  If there is more than one, API-X will use the one with the highest service ranking.

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.apix.registry.cfg`. The following
values are available for configuration:

### `timeout.connect.ms`

Timeout in milliseconds for http connect

    timeout.connect.ms=1000

### `timeout.socket.ms`
    
Timeout in milliseconds for socket connect

    timeout.socket.ms=1000

### auth.${scheme}.${port}.${host}.username
    
Username for authentication, against a specific host, port, and scheme

    auth.http.8080.localhost.username = fedoraAdmin

### auth.${scheme}.${port}.${host}.password

Password for authentication against a specific host, port, and scheme     

    auth.http.8080.localhost.password = secret3
