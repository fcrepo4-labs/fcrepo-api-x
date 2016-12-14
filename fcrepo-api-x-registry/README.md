# API-X Registry module

This module contains base implementations of internal API-X registries

## OSGi Deployment

This module is automatically deployed when the `fcrepo-api-x` feature is installed in Karaf.

## Configuration

The application can be configured by creating a file in
`$KARAF_HOME/etc/org.fcrepo.apix.registry.cfg`. The following
values are available for configuration:

Timeout in milliseconds for http connect

    timeout.connect.ms=1000
    
Timeout in milliseconds for socket connect

    timeout.socket.ms=1000
