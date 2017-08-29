<h1><a id="top" href="#top" class="anchor">API-X Design: Execution & Routing</a></h1>

* [Introduction](#introduction)
* [Execution](#execution)
    * [Intercepting modality](#intercepting-modality)
    * [Exposing modality](#exposing-modality)
    * [Services](#services)
        * [Consumed service](#consumed-service)
        * [Exposed service](#exposed-service)
    * [Execution Engines](#execution-engines)
        * [Generic Endpoint Proxy](#generic-endpoint-proxy)
            * [Endpoint Example](#endpoint-example)
        * [Generic Intercepting Proxy](#generic-intercepting-proxy)
            * [Intercepting Example](#intercepting-example)
* [Routing](#routing)
    * [Intercepting modality](#intercepting-modality)
    * [Exposing modality](#exposing-modality)

<h1><a id="introduction" href="#introduction" class="anchor">Introduction</a></h1>

In the API-X architecture, a service registry contains descriptions of services, and locations of service instances.  An [extension registry][0] contains extension definitions that describe resources an extension binds to, its [modality][1] (i.e. exposing or intercepting), and lists the services consumed and/or exposed by an extension.  A routing engine determines the set of extensions that apply to a given HTTP request from a user, and an execution engine is responsible for invoking a _consumed_ service in order to implement the extension.  See the API-X [workflow][2] for an overview.  This document describes the routing and execution of an extension on a given request.

<h1><a id="execution" href="#execution" class="anchor">Execution </a></h1>

Execution is the process by which a client HTTP request is acted upon by an extension.  The mechanism of action differs based upon the extension’s modality.

<h2><a id="intercepting-modality" href="#intercepting-modality" class="anchor">Intercepting modality</a></h2>

In the [intercepting][3] modality, the client sends an HTTP request to a repository resource according to Fedora’s APIs (e.g. `POST` to a container to create a new LDP resource, `PATCH` to update a resource).  API-X intercepts the request on its way to and from the repository, and performs one or more of the following actions:

* Extracts content from the incoming request or repository resource associated with the incoming request.
* Formulates a request to a _consumed_ service from the information in (1), and invokes it somehow.
* Use the information in (1) and the response from from the _consumed_ service to modify incoming requests or outgoing responses somehow. 

These actions are performed by an [execution engine](#execution-engines) that corresponds to a given consumed service. The [routing](#routing) component decides which execution engine will handle a given request for an extension.

<h2><a id="exposing-modality" href="#exposing-modality" class="anchor">Exposing modality</a></h2>

In the [exposing][4] modality, the client sends an HTTP request to a URI [exposed by API-X][5] invoking a service on a particular repository resource.  The nature of this request is unspecified _a priori_, and depends on the API of the service being exposed.  For example, a service that generates a thumbnail representation of an image in Fedora may simply be invoked with a `GET` request from a client.  API-X serves as a middleware layer (for example, a proxy) which mediates the interaction between the client and a service.  

In response to a client request to an exposed URI, API-X performs one or more of the previously described [intercepting](#intercepting-modality) actions, and formulates a response to the client, in accordance with the semantics of the _exposed service_.

These actions are performed by an [execution engine](#execution-engines) that corresponds to the _exposed_ and _consumed_ services in the extension definition.

<h2><a id="services" href="#services" class="anchor">Services</a></h2>

A service in API-X is loosely defined as “anything that can receive an HTTP request message, and return an HTTP response message”.  Services vary in their interaction model, protocol, data models, etc.  Their implementation may range from a camel route running in the same VM as API-X that processes HTTP messages, to web service somewhere on the Internet at large.  The API-X service registry contains (or links to) descriptions of registered services.  These descriptions may use any arbitrary standard, e.g. (a [SSWAP Resource Description Graph (RDG)](http://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-10-309#Fig2),  [SAWSDL](https://www.w3.org/TR/sawsdl/), [Hydra](https://www.hydra-cg.com/spec/latest/core/), etc), or may simply label the service by giving it a `rdf:type` that distinguishes it from others (e.g. `myns:ThumbnailService`).  

<h3><a id="consumed-service" href="#consumed-service" class="anchor">Consumed service</a></h3>

Both intercepting and exposing modalities have the notion of a _consumed_ service.  When servicing a request, an [execution engine](#execution-engines) formulates a request to an instance of the consumed service, invokes it, and does something useful with the response.  As such, clients generally do not directly interact with consumed services.  Consumed services can be thought of as the _implementation_ of an extension.

<h3><a id="exposed-service" href="#exposed-service" class="anchor">Exposed service</a></h3>

Exposed services are only relevant to the exposing modality.  When servicing a request, an [execution engine](#execution-engines) invokes a consumed service, and formulates a response to send back to the client.  Exposed services can be thought of as the _interface_ of an extension.

<h2><a id="execution-engines" href="#execution-engines" class="anchor">Execution Engines</a></h2>

This API-X design proposes pluggable “execution engines” that are tasked with invoking a specific kind of services to implement one of the API-X intercepting or exposing modalities.  A given execution engine MAY use the service description of a consumed service in order to instruct it how to invoke a given service, or this knowledge may be implicit.  When executing an extension, the routing engine will select an appropriate execution based upon the nature of the service consumed and exposed by the extension.  This information is present in the [extension registry][0] in the form of [`apix:consumesService`][7] and [`apix:exposesService`][8] properties, which link to entries in the [service registry][6].   Internally, API-X maintains a map between execution engines and services.  

API-X will contain an initial set of execution engines that will grow as need dictates.

<h3><a id="generic-endpoint-proxy" href="#generic-endpoint-proxy" class="anchor">Generic Endpoint Proxy</a></h3>

This execution engine supports the exposing modality.  It operates by proxying a request to a service instance, and returning the results unmodified. An  `Apix-Ldp-Resource` header is added to the request to identify the repository resource being acted upon. A service may use this URI to retrieve content from the resource as necessary.  The request sent to the consumed service instance has the following characteristics:

* The HTTP method matches the incoming request method.
* The HTTP path has the relative portion of the request with respect to the exposed service URI appended to it.
* The `Host` header is preserved as the API-X host.
* An `Apix-Ldp-Resource` header is added to the request, containing the URI of the resource exposing the service in the Fedora repository.
* An API-X header `Apix-Exposed-Uri` is added to the request containing the URI of the exposed service.  A service may use this as a base URI when formulating a response that contains hypertext or links, but should consider using relative URIs instead.
* The body and all other headers are preserved.

The generic endpoint proxy can implement an extension where [`apix:exposesService`][8] and [`apix:consumesService`][7] are equivalent.  It will use a [service instance registry][6] to discover a registered instance of the service indicated by `apix:consumesService`, and proxy the request to that service instance.

<h4><a id="endpoint-example" href="#endpoint-example" class="anchor">Endpoint Example</a></h4>

A client sends a request to an exposed service on object foo, exposed at `http://archive.example.org/services/foo/svc:Thumbnail`.

    GET /services/foo/svc:Thumbnail/
    Host: archive.example.org
    Accept: */*
    User-Agent: whatever

The generic endpoint proxy discovers a service endpoint at `http://backend01.internal:8080/services/thumb`, and proxies the following request to it:

    GET /services/thumb/
    Host: archive.example.org
    Accept: */*
    User-Agent: whatever
    Apix-Ldp-Resource: http://fedora01.backend.local/repository/foo/
    Apix-Exposed-Uri: http://archive.example.org/services/foo/svc:Thumbnail/

The service returns a response, which is sent back to the client unmodified.  In this example, it’s an RDF document listing thumbnail resources:

    HTTP/1.1 200 OK
    Content-Type: text/turtle; charset=UTF-8
    Allow: HEAD, OPTIONS, GET
    Content-Length: 123
    
    @prefix myns: <http://example.org/myns> .
    <> a myns:ThumbnailMenu ;
       myns:hasThumbnail <small> ;
       myns:hasThumbnail <big> .
    
    <small> a myns:Thumbnail ;
       myns:size “120x120” .
    
    <big> a myns:Thumbnail ;
       myns:size “240x240” .

The client processes this response, and decides it wants to retrieve the small thumbnail:

    GET /services/foo/svc:Thumbnail/small
    Host: archive.example.org
    Accept: */*
    User-Agent: whatever

The generic endpoint proxy sends the request to the service:

    GET /services/thumb/small
    Host: archive.example.org
    Accept: */*
    User-Agent: whatever
    Apix-Ldp-Resource: http://fedora01.backend.local/repository/foo/
    Apix-Exposed-Uri: http://archive.example.org/services/foo/svc:Thumbnail/

Finally, the service responds with an image:

    HTTP/1.1 200 OK
    Content-Type: image/jpeg
    Allow: HEAD, OPTIONS, GET
    Content-Length: 123456
    
    ….

<h3><a id="generic-intercepting-proxy" href="#generic-intercepting-proxy" class="anchor">Generic Intercepting Proxy</a></h3>

This execution engine supports the intercepting modality.  It operates by proxying a request or response to a service instance, and inspecting the response.  Like the generic endpoint proxy, it adds a header `Apix-Ldp-Resource` to the request before sending it to the consumed service instance.

* If the response  from a given service is a 2xx code with no body, the extension will pass the request unmodified.   Headers will be merged.
* If the response from a given service is a 2xx code with a body,   the body of the request will be substituted with the response body.  Headers will be merged.
* If the response from a given service is an error or redirect (3xx+), that response will immediately be returned to the client.

Responses from the repository are also intercepted and passed to the extension under similar rules:

* If the response from a given service is a 2xx, headers will be merged, and the body will be replaced, if present
* If the response from a given service is an error or redirect, it is ignored.

<h4><a id="intercepting-example" href="#intercepting-example" class="anchor">Intercepting Example</a></h4>

A client wishes to create a repository resource via `POST` to container `http://archive.example.org/repository/path/to/foo`.  An intercepting validation extension is bound to the container.

    POST repository/path/to/foo
    Host: archive.example.org
    Content-Type: text/turtle
    Slug: theNewResource
    
    ...resource content...

The generic intercepting proxy discovers a consumed service at `http://backend14.internal:8080/services/validation`, and sends it a request
 
    POST /services/validation
    Host: backend14.internal:8080
    Content-Type: text/turtle
    Slug: theNewResource
    Apix-Ldp-Resource: http://fedora01.backend.local/repository/path/to/foo
    
    ...resource content...

The service performs validation on the request, determines it is OK, and sends back success with no content.  The service happens to contain a ‘Validation’ header in its response:

    HTTP/1.1 204 No Content
    Validation: OK
    
The generic intercepting proxy folds in the header and passes the request along to the next extension:

    POST repository/path/to/foo
    Host: archive.example.org
    Content-Type: text/turtle
    Slug: theNewResource
    Validation: OK
    
    ...resource content...

<h1><a id="routing" href="#routing" class="anchor">Routing</a></h1>

The routing engine accepts requests from a client, and determines how that request is serviced.  It needs to:

* Determine if a request is for a repository resource or for an exposed service.
* Consult the extension registry, and to see if any extensions apply to a given resource.
* Verifies the topology of extensions servicing requests (e.g. to avoid cycles)
* Inspect the extension definition (in particular [`apix:consumesService`][7]) to determine which execution engine can service the request.
* Send the request to the appropriate execution engine.

<h2><a id="intercepting-modality" href="#intercepting-modality" class="anchor">Intercepting modality</a></h2>

In the intercepting modality, a pipeline of extensions may modify a request to a repository resource, or a response from the repository.  The routing engine, therefore, needs to:

* Determine the list of intercepting extensions that apply to the request.
    * Extension definitions specify binding conditions that may involve owl reasoning.  API-X may offer different [implementations][9] of reasoners; some may pre-compute bindings, others may perform reasoning at transaction time.
* Assemble them into a sequence.
    * At the moment, the execution order is undefined and up to the implementation.
* Assign execution engines to each extension.
* Route the request or response message to each execution engine.

<h2><a id="exposing-modality" href="#exposing-modality" class="anchor">Exposing modality</a></h2>

The extension definition ontology defines a property [`apix:exposesServiceAtURI`][10] indicating “where the service should be exposed”.  For [repository scoped][11] and [resource scoped][12] services, the expectation is that API-X will produce a URI that exposes the service as mediated through API-X.  Therefore, value is used to generate URIs that the routing component can unambiguously associate with the extension that specifies it.  To the client, these URIs are opaque, so a client SHOULD NOT assume any particular URI structure to service URIs exposed by API-X.  

By parsing the URI to an exposed service, the routing engine can identify the extension that exposes a given service.  The routing engine, therefore, needs to:

* Verify that the extension is indeed bound to the repository resource exposing the URI, if applicable.
* Select the appropriate execution engine.
* Route the request to the execution engine, and return the response to the user.

---

[0]: ./extension-definition-and-binding.md#extension-registry-and-api "Extension Registry & API - API-X Extension Definition & Binding"
[1]: ./apix-design-overview.md#modality "Modality - API-X Design Overview"
[2]: ./apix-design-overview.md#workflow "Workflow - API-X Design Overview"
[3]: ./apix-design-overview.md#intercepting "Intercepting - API-X Design Overview"
[4]: ./apix-design-overview.md#exposing "Exposing - API-X Design Overview"
[5]: ./uris-in-apix.md#api-x-exposed-service-uris "API-X exposed service URIs - URIs in API-X"
[6]: ./service-discovery-and-binding.md#service-registry "Service Registry - API-X Service Discovery & Binding"
[7]: ./extension-definition-and-binding.md#apixconsumesservice "apix:consumesService - API-X Extension Definition & Binding"
[8]: ./extension-definition-and-binding.md#apixexposesservice "apix:exposesService - API-X Extension Definition & Binding"
[9]: ./extension-definition-and-binding.md#implementations "Implementations - API-X Extension Definition & Binding"
[10]: ./extension-definition-and-binding.md#apixexposesserviceaturi "apix:exposesServiceAtURI - API-X Extension Definition & Binding"
[11]: ./uris-in-apix.md#repository-scoped-services "Repository Scoped Services - URIs in API-X"
[12]: ./uris-in-apix.md#resource-scoped-services "Resource Scoped Services - URIs in API-X"
