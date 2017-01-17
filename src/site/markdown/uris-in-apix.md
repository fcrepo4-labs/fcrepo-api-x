<h1><a id="top" href="#top" class="anchor">URIs in API-X</a></h1>

* [Overview of resource URIs in LDP](#overview-of-resource-uris-in-ldp)
* [API-X intercepting URIs](#api-x-intercepting-uris)
    * [Intercepting Example](#intercepting-example)
    * [Implementation notes](#implementation-notes)
* [API-X exposed service URIs](#api-x-exposed-service-uris)
    * [Exposing Example](#exposing-example)
    * [External exposed services](#external-exposed-services)
    * [Repository scoped services](#repository-scoped-services)
    * [Resource scoped services](#resource-scoped-services)
* [Multi-Tenancy](#multi-tenancy)

<h2><a id="overview-of-resource-uris-in-ldp" href="#overview-of-resource-uris-in-ldp" class="anchor">Overview of resource URIs in LDP</a></h2>

As per [RFC 3986](https://tools.ietf.org/html/rfc3986), a URI is composed of a scheme + authority + path + [query] + [fragment], where query and fragments are optional.

LDP doesn’t prescribe any particular structure to URIs, but states that relative URI resolution with respect to LDP resources must be supported as per [§4.2.1.5](http://www.w3.org/TR/ldp/#h-ldpr-gen-defbaseuri).  As mentioned in [RFC 3986 §1.2.3](https://tools.ietf.org/html/rfc3986#section-1.2.3), relative URI references can only be used in context of hierarchical URIs.  The identity of a given LDP resource, therefore, is necessarily determined by the path component of its URI (and not, for example, a query component of the URI, e.g. <code>http://localhost:8080/fedora.aspx<b>?resource=/foo</b></code>).  Likewise, the path component of a URI may contain fixed implementation-specific path components used for routing requests, such as a web application context path or servlet name.  Such components do not serve to identify individual LDP resources.

Fedora resources, therefore can be addressed as a repository base URI + resource path, where the base URI contains scheme, authority, and other fixed path components, and the resource path serves to identify an LDP resource<sup><a href="#sup1">1</a></sup>.  Although not codified in any specification (LDP or Fedora), best practice as per [a W3C working group note](https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-bp/ldp-bp.html#represent-container-membership-with-hierarchical-uris) is to represent LDP containment hierarchy in URI path elements. While most LDP implementations (including the Fedora community implementation) adhere to this recommendation, it cannot be presumed.  For the sake of API-X, it is sufficient to know that LDP resource URIs are prefixed by an invariant repository base URI, and suffixed by a path component that somehow serves to uniquely identify an LDP resource<sup><a href="#sup2">2</a></sup>.

As an example, a resource URI in Fedora `http://localhost:8080/fcrepo/rest/foo` can be represented as a base URI `http://localhost:8080/fcrepo/rest` concatenated with a resource path `/foo`.

<h2><a id="api-x-intercepting-uris" href="#api-x-intercepting-uris" class="anchor">API-X intercepting URIs</a></h2>

As discussed in the [API-X design overview][1], API-X supports the notion of ‘intercepting’ extensions that can alter the representation of repository resources by modifying incoming requests to or responses from the repository.  In addition, API-X itself may alter the representation of repository resources, such as adding link headers to support discovery of exposed services.  To achieve this, as HTTP middleware API-X may be deployed as a [reverse proxy](https://tools.ietf.org/html/rfc3040#section-2.2) (also known as a surrogate) over a Fedora repository.  

Repository resources exposed via API-X (i.e. behind the API-X reverse proxy) have URIs that are different from those authored by the repository software itself.  This is an inherent characteristic of reverse proxies, and other network components of its kind (such as load balancers or SSL termination proxies).   A URI  to a repository resource exposed via API-X will have a baseURI indicating the host, port, and invariant context path of the API-X; and will have a path component which matches the repository resource path in Fedora.  In general, the resource URIs exposed by reverse proxies like API-X are the only URIs published on the public web. 

<h3><a id="intercepting-example" href="#intercepting-example" class="anchor">Intercepting Example</a></h3>

As an example, suppose resource `/foo` is accessible in Fedora as `http://fedorahost03.example.org:13835/fcrepo/rest/foo/`.  The repository base URI in this case is `http://fedorahost03.example.org:13835/fcrepo/rest/`.  A request and response for this resource in Fedora may look something like:

Request to Fedora [for a repository resource]:

    GET /fcrepo/rest/foo/ HTTP/1.1
    Host: fedorahost03.example.org:13835
    User-Agent: curl/7.49.1
    Accept: */*
    
Response from Fedora [of a repository resource]:

    HTTP/1.1 200 OK
    ETag: "23ee310e06465b275b8c94a06449668a7bb2907a"
    Last-Modified: Wed, 20 Mar 2015 03:58:34 GMT
    Link: <http://www.w3.org/ns/ldp#Container>;rel="type"
    Accept-Patch: application/sparql-update
    Accept-Post: text/turtle...
    Allow: MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS
    Content-Type: text/turtle
    Content-Length: 1873
    Server: Jetty(9.2.3.v20140905)

    @prefix…
    <> a ldp:Container, fedora:container;
     fedora:lastModified “2015…”^^<xsd:dateTime> ;
     myns:rel <http://fedorahost03.example.org:13835/fcrepo/rest/bar/> .

Suppose there is an API-X instance configured such that the repository resource baseURI for API-X is http://archive.example.org/repository/.  The same interaction via API-X would be:

Request to API-X [for a repository resource]:

<pre>
GET <b>/repository/foo/</b> HTTP/1.1
Host: <b>archive.example.org</b>
User-Agent: curl/7.49.1
Accept: */*
</pre>

Response from API-X [of a repository resource]:

<pre>
HTTP/1.1 200 OK
ETag: "23ee310e06465b275b8c94a06449668a7bb2907a"
Last-Modified: Wed, 20 Mar 2015 03:58:34 GMT
Link: &lt;http://www.w3.org/ns/ldp#Container&gt;;rel="type"
<b>Link: &lt;http://archive.example.org/services/foo/apix:services&gt;;rel=”service”</b>
Accept-Patch: application/sparql-update
Accept-Post: text/turtle...
Allow: MOVE,COPY,DELETE,POST,HEAD,GET,PUT,PATCH,OPTIONS
Content-Type: text/turtle
Content-Length: 1873
Server: Jetty(9.2.3.v20140905)

@prefix…
&lt;&gt; a ldp:Container, fedora:Container;
 fedora:lastModified “2015…”^^&lt;xsd:dateTime&gt; ;
 myns:rel <b>&lt;http://archive.example.org/repository/bar/&gt;</b> .
</pre>

Differences between fedora vs API-X requests or responses are highlighted in bold.  Notable characteristics are:

* API-X has added a link header in the response, linking to a document describing services exposed by API-X on the object (see [Service Discovery and Binding]).
* All links to repository resources in the header or body of the response are prefixed with the API-X repository base URI.  There are no links directly to the Fedora instance proxied by API-X.
* Syntactically, relative URIs do not change at all<sup><a href="#sup3">3</a></sup>. In this example, the only relative URI is the null relative URI (`<>`).

<h3><a id="implementation-notes" href="#implementation-notes" class="anchor">Implementation notes</a></h3>

Depending on the characteristics of a given Fedora implementation, replacing Fedora repository resource base URIs with API-X repository resource base URIs may be easy, or difficult.  In particular, how Fedora generates absolute URIs is a significant deployment concern.   Three possibilities that have different implications:

1. Fedora generates absolute URIs using the `Host` header value from the incoming request.  The community implementation of Fedora follows this pattern.
1. Fedora generates absolute URIs using the repository path and a pre-configured base URI.
1. Fedora generates absolute URIs using some fixed logic that is invariant.

For (1) and (2), it’s possible to configure Fedora in a manner such that base URI substitution is trivial, and performed by the Fedora implementation itself.  For (1) to be solved trivially, the path segments in the API-X and Fedora base URIs must match.  This can usually be achieved by configuring the Fedora software.  As an example, Java-based Fedora implementation may be deployed as a war file in a servlet container.  Servlet containers typically provide means of specifying web application context paths and servlet names.  A webapp context of ‘fcrepo’ and servlet name of ‘rest’ results in `http://[host]/fcrepo/rest/` in the base URI, whereas a webapp context of ‘’ and a servlet name of ‘repository’ results in `http://[host]/repository/`.

For (3), or in corner cases where (1) cannot be satisfied by configuration alone, the only solution is for API-X to perform re-writing of URIs in the headers and bodies all HTTP requests.

<h2><a id="api-x-exposed-service-uris" href="#api-x-exposed-service-uris" class="anchor">API-X exposed service URIs</a></h2>

As mentioned in the overview document, API-X provides URIs for services available on repository resources.  As per [Service Discovery & Binding][5], URIs for services bound to a given repository resource are linked within a service document for that object.   API-X includes a ‘service’ link header to the service document (generated by API-X) enumerating (linking and describing) all services bound to an object.

<h3><a id="exposing-example" href="#exposing-example" class="anchor">Exposing Example</a></h3>

To illustrate API-X exposed service URIs, suppose there is a repository resource `/foo` accessed via API-X at the URI `http://archive.example.org/repository/foo/`. 

API-X will provide a Link header to a service document for that object, for example:
`Link: <http://archive.example.org/services/foo/apix:services>;rel=”service”`

The service document will link to all services bound to the object (simplified for the sake of clarity):

    <http://archive.example.org/repository/foo/>
      apix:hasService <http://sswap.example.org/colorRenderer>,
                      <http://archive.example.org/services/svc:oai>,
                      <http://archive.example.org/services/foo/svc:thumbnail> .
    
    <http://sswap.example.org/colorRenderer> a sswap:Resource .
    <http://archive.example.org/services/foo/svc:thumbnail> a myns:Thumbnail .
    <http://archive.example.org/services/svc:oai> a oai:OAI-PMH .


As illustrated in this example, and codified in the extension definition ontology, there are three kinds of services that can be bound to an object and made discoverable via API-X.  Each kind of service has a different set of implications with respect to URIs.

<h3><a id="external-exposed-services" href="#external-exposed-services" class="anchor">External exposed services</a></h3>

External services (indicated by an absolute binding URI in the extension definition) are not mediated by API-X;  API-X merely links to an external URI of a service wherever such an extension is bound to an object.  In the above example, an SSWAP service is available at `http://sswap.example.org/colorRenderer`.  Interaction with the service, and understanding its relationship to the object linking to it, is unspecified by API-X and entirely up to the client.  A possible motivation for exposing a [SSWAP](http://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-10-309) service as shown in the example might be to aid in the discovery of SSWAP services that can act on the contents of a repository resource, without necessarily relying on a [discovery server](http://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-10-309#Sec8).

In extension definitions, an external service is indicated by specifying an absolute URI for [`apix:exposesServiceAtURI`][2].

<h3><a id="repository-scoped-services" href="#repository-scoped-services" class="anchor">Repository scoped services</a></h3>

Repository scoped services are similar to ‘external services’, but are mediated by API-X.  The distinguishing characteristic of repository services is that their representation and operation is completely independent of the identity or contents of any objects that are bound to it by an extension definition.  These can be seen as “global” or “singleton” services that have only one instantiation in API-X.  

The [example](#exposing-example) shows an OAI-PMH provider as an example of such repository service.  Only one such service exists in API-X, and all objects bound to this service link to its one URI.  Like external services, the relationship between a given repository resource and the service is unspecified; API-X just makes the service discoverable from bound objects.  A possible motivation for exposing an OAI service as in the example may simply be to link resources that have a corresponding OAI record to the service OAI-PMH service that contains that record, to aid in discovery.

In extension definitions, an external service is indicated by specifying an absolute _path segment_ for [`apix:exposesServiceAtURI`][2].

Because API-X mediates interactions with repository scoped services services (i.e. there is an execution engine that routes a request to and from an implementing service), API-X must expose a URI for such service.  In order to be routable by API-X, this URI must convey:

*  The identity of service (correlated with the path segment defined by `apix:exposesServiceAtURI`)
*  Additional query parameters, path elements, or fragments specific to service 

The current API-X implementation proposes minting such URIs as follows:

*  API-X baseURI + service root + service identity + [query, path elements]
    * Service root is an invariant path which identifies the service-exposing component of API-X (This may be, for example, a servlet named ‘services’).
    * Service identity is the path segment specified by `apix:exposesServiceAtURI`.  This path segment SHOULD contain a reserved character (e.g. ‘:’, ‘()’, etc from rfc3986) in order to reduce the possibility of ambiguity with similarly-named LDP resources.  The convention used in the examples in the document is `svc:<service_name>`, but API-X does not enforce or require any particular convention.
    * Additional query and path elements are implementation specific. For example, an OAI endpoint may link to, or respond to <code>http://archive.example.org/services/svc:oai<b>?verb=ListRecords</b></code>

An alternate URI construct is API-X baseURI + repository root + service identity + [query, path elements].  For example, `http://archive.example.org/services/svc:oai` (proposed) vs  `http://archive.example.org/repository/svc:oai`.

The API-X implementation will need to pick an approach to minting URIs, and stick to it.

<h3><a id="resource-scoped-services" href="#resource-scoped-services" class="anchor">Resource scoped services</a></h3>

Resource scoped services are mediated by API-X, and are contextualized to a given repository resource.  That is to say, an [execution engine][3] accepts an incoming request to a resource service URI, incorporates the identity or contents of the bound repository resource into a request to an implementing (consumed) service, and formulates a response.  This pattern is most similar to Fedora 3 disseminators, albeit with no inherent constraints on the contents or HTTP method of the request or response.  Every instantiation of a resource service on a repository resource produces a unique web resource.  API-X is required to mint URIs for such instantiations.  

The [example](#exposing-example) shows a thumbnail-generating service as an example of a resource service.  Let’s say that the purpose of the service is to take the binary content of the resource, shrink it to a given size, and produce a reduced-size representation.  A client simply performs a GET on the thumbnail service URI for a particular resource to obtain a thumbnail representation.

In extension definitions, a resource service is indicated by specifying a _relative path segment_ for [`apix:exposesServiceAtURI`][2].

API-X mediates the interaction between the client and implementing service for a given repository resource.  In order to be exposed at a URI that is routable by API-X, this URI must convey:

* Identity of the service  (correlated with the path segment defined by [`apix:exposesServiceAtURI`][2])
* Identify of the repository resource
* Additional query parameters, path elements, or fragments specific to service

The current API-X implementation proposes minting such URIs as follows:

* API-X baseURI + service root + resource path + service identity + [query, path elements]
    * Service root is an invariant path which identifies the service-exposing component of API-X (This may be, for example, a servlet named ‘services’).
    * Resource path is self-explanatory
    * Service identity is the path segment specified by `apix:exposesServiceAtURI`.  This path segment SHOULD contain a reserved character (e.g. ‘:’, ‘()’, etc from rfc3986) in order to reduce the possibility of ambiguity with similarly-named LDP resources.  The convention used in the examples in the document is `svc:<service_name>`, but API-X does not enforce or require any particular convention.
    * Additional query and path elements are implementation specific, such as `http://archive.example.org/services/foo/svc:thumbnail/jpg/big` or `http://archive.example.org/services/foo/svc:thumbnail?size=big&format=jpg`

An alternate URI construct is API-X baseURI + repository root + resource path + service identity + [query, path elements].  For example, `http://archive.example.org/services/foo/svc:thumbnail` (proposed) vs  `http://archive.example.org/repository/foo/svc:thumbnail` (alternate)

<h2><a id="multi-tenancy" href="#multi-tenancy" class="anchor">Multi-Tenancy</a></h2>

While multi-tenancy is not a use case or explicit goal of API-X, there is nothing theoretically preventing multiple independent instances of API-X from being deployed over a single repository.  If URIs obey the LDP best practice of path components matching containment hierarchy, and a ‘tenant’ has a connected LDP hierarchy rooted at some container, then choosing an API-X base URI containing a path to the root container could potentially be a trivially easy way to bind an API-X instance to a tenant’s subset of the repository.  This may be an interesting topic to explore at some point.

---

<a id="sup1" href="#sup1"><sup>1</sup></a> URI fragments (#hash) are not relevant to identifying LDP resources, nor are query components.

<a id="sup2" href="#sup2"><sup>2</sup></a> The Fedora [Messaging SPI][4] makes use of the distinction between resource path and repository base URI as well, codifying the distinction as part of Fedora’s specification.

<a id="sup3" href="#sup3"><sup>3</sup></a> Use of relative URIs is an LDP [best practice](https://dvcs.w3.org/hg/ldpwg/raw-file/default/ldp-bp/ldp-bp.html#use-relative-uris).

[1]: ./apix-design-overview.md "API-X Design Overview"
[2]: ./extension-definition-and-binding.md#apixexposesserviceaturi "API-X Extension Definition and Binding - apix:exposesServiceAtURI"
[3]: ./execution-and-routing.md#execution-engines "API-X Execution & Routing"
[4]: https://fcrepo.github.io/fcrepo-specification/#messaging-spi "Fedora Specification - Fedora Messaging SPI"
[5]: ./service-discovery-and-binding.md "API-X Service Discovery & Binding"
