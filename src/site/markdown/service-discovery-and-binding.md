<h1><a id="top" href="#top" class="anchor">Service Discovery and Binding</a></h1>

* [Introduction](#introduction)
* [Ontology](#ontology)
    * [`apix:Service`](#apixservice)
    * [`apix:canonical`](#apixcanonical)
    * [`apix:ServiceInstance`](#apixserviceinstance)
    * [`apix:hasEndpoint`](#apixhasendpoint)
    * [`apix:exposesServiceInstance`](#apixexposesserviceinstance)
    * [`apix:isFunctionOf`](#apixisfunctionof)
* [Registry](#registry)
    * [Service Registry](#service-registry)
    * [Service Instance Registry](#service-instance-registry)
        * [Example: LDP Service Registry](#example-ldp-service-registry)
        * [Example: LDP Service Instance Registry](#example-ldp-service-instance-registry)
        * [Example: Zookeeper Service Instance Registry](#example-zookeeper-service-instance-registry)
* [Discovery](#discovery)
    * [Service Document](#service-document)
        * [Example](#service-document-example)
        * [Indexing and messaging](#indexing-and-messaging)
* [Distributed Shared State](#distributed-shared-state)
* [Notes](#notes)
    * [Alternate implementation of service registry in LDP](#alternate-implementation-of-service-registry-in-ldp)

<h1><a id="introduction" href="#introduction" class="anchor">Introduction</a></h1>

Clients interacting with the API Extension framework need a mechanism to discover the services that apply to a given repository resource. Likewise, services themselves will need a mechanism by which they can register or bind themselves to the API-X framework.

A principal role of the Service Discovery and Binding (SD&B) component is to support an architecture that recognizes that services come and go, sometimes unexpectedly. To that end, it should be possible to decouple the lifecycle of a particular service instance from the lifecycle (i.e. deployment) of the API-X framework, including the SD&B component. Furthermore, it should be possible to deploy this component in a distributed fashion across multiple machines, both to support high availability and high levels of concurrency. It should also be possible for services to be deployed on an arbitrary number of external hosts using any language or framework. With that structure, network partitions and service failure should not affect the overall operation of this component nor the overall operation of API-X.

In many ways, the SD&B component can be thought of as a management interface, distinct from individual service endpoints.  It is a counterpart to the [Extension Definition and Binding][0] component in API-X.  These components  can be viewed as a broker between clients, repository resources and external services.

The high level objectives of the service discovery and binding component are to support the following:

* Service Discovery (i.e. client interaction):
    * list all services bound to a given fedora object (i.e. [external][1] and [resource services][2])
    * list all services bound to the repository (i.e. [repository services][3])
    * list service status (availability/non-availability)
    * provide some level of description of services (e.g. as RDF)
    * use REST semantics
* Service Registry (i.e. service interaction)
    * Service instances should be able to register and deregister themselves from API-X
    * It should be possible for individual services to be available at N hosts (e.g. for high availability)
    * If a particular service instance fails or is removed, API-X should know about that (optional)
* Deployment
    * the SB&D component should be capable of being deployed in a fully distributed environment, across multiple hosts, and such deployment should be entirely transparent to clients.
    * it should be possible for the SD&B interface to be deployed on separate hosts from the services themselves.

<h1><a id="ontology" href="#ontology" class="anchor">Ontology</a></h1>

The API-X service ontology is expected to evolve during API-X development.  A terse Turtle representation of the current proposed API-X service ontology is as follows:

    @prefix apix:<http://example.org/apix#> .
    @prefix fcr:<http://fedora.info/definitions/v4/repository#> .
    @prefix owl:<http://www.w3.org/2002/07/owl#> .
    @prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> .
    
    apix:Service a owl:Class .
    
    apix:ServiceInstance a owl:Class .
    
    apix:ServiceInstanceRegistry a owl:Class .
    
    apix:ServiceDocument a owl:Class .
    
    apix:ServiceRegistry a owl:Class .
    
    apix:isFunctionOf a owl:ObjectProperty ;
        rdfs:domain apix:ServiceInstance ;
        rdfs:range fcr:Resource .
    
    apix:hasServiceInstance a owl:ObjectProperty ;
        rdfs:domain apix:Service ;
        rdfs:range: apix:ServiceInstance .
    
    apix:isServiceInstanceOf a owl:ObjectProperty ;
        owl:inverseOf apix:hasServiceInstance .
    
    apix:exposesServiceInstance a owl:ObjectProperty ;
        rdfs:domain fcr:Resource ;
        rdfs:range: apix:ServiceInstance .
    
    apix:serviceInstanceExposedBy a owl:ObjectProperty ;
         owl:inverseOf apix:exposesServiceInstance .
    
    apix:hasServiceDocument a owl:ObjectProperty ;
         rdfs:range: apix:ServiceDocument ;
         rdfs:domain: fcr:Resource .
    
    apix:isServiceDocumentFor a owl:ObjectProperty;
        owl:inverseOf apix:hasServiceDocument .
    
    apix:hasEndpoint a owl:ObjectProperty ;
        rdfs:domain apix:ServiceInstance ;
        rdfs:subPropertyOf owl:sameAs .
    
    apix:canonical a owl:ObjectProperty ;
        rdfs:domain: apix:Service ;
        rdfs:subPropertyOf owl:sameAs .
    
    apix:isServiceInstanceRegistryFor a owl:ObjectProperty ;
       rdfs:domain: apix:ServiceInstanceRegistry ;
       rdfs:range: apix:Service .
    
    apix:hasServiceInstanceRegistry a owl:ObjectProperty ;
       owl:inverseOf apix:isServiceInstanceRegistryFor ;

<h2><a id="apixservice" href="#apixservice" class="anchor">apix:Service</a></h2>

A service in API-X is loosely defined as “anything that can receive an HTTP request message, and return an HTTP response message”.  It is the abstract notion of a web resource accessed through HTTP or using HTTP message semantics.  Services vary in their interaction model, protocol, data models, etc.  Their implementation may range from a camel route running in the same VM as API-X that processes HTTP messages, to a web service somewhere on the Internet at large.

<h2><a id="apixcanonical" href="#apixcanonical" class="anchor">apix:canonical</a></h2>

This optional property serves to canonically identify a service.  If the canonical service URI is dereferenceable, it should contain an authoritative description of the service or associated specification.

<h2><a id="apixserviceinstance" href="#apixserviceinstance" class="anchor">apix:ServiceInstance</a></h2>

A service instance is the concrete instantiation of a service as a web resource.  If present, the property [`apix:hasEndpoint`](#apixhasendpoint) identifies a URI that resolves to the service instance representation.  A client engages with a service at this URI according to the service’s own API.  If the URI for an `apix:ServiceInstance` is not equivalent to the URI indicated by `apix:hasEndpoint`, then it SHOULD NOT have a representation (i.e. it SHOULD be a fragment URI, or a 303 redirect). 

<h2><a id="apixhasendpoint" href="#apixhasendpoint" class="anchor">apix:hasEndpoint</a></h2>

This property indicates the URL of any service endpoints.
 
<h2><a id="apixexposesserviceinstance" href="#apixexposesserviceinstance" class="anchor">apix:exposesServiceInstance</a></h2>

This property links a repository resource to the URI of any service(s) [exposed][4] on it via API-X.  Such [URIs][5] are are potentially [routed][6] by API-X and implemented by an extension.  Its inverse is `apix:serviceInstanceExposedBy`.

<h2><a id="apixisfunctionof" href="#apixisfunctionof" class="anchor">apix:isFunctionOf</a></h2>

This property is used to indicate that a given service instance is a function of a given object.  That is to say, the contents or identity of the given object influences the behaviour or representation of the service.  For example, a given ‘Thumbnail’ service may produce a reduced-sized thumbnail of an image.  The thumbnail service instance as instantiated on a specific object produces a thumbnail image of that specific object.   See also [resource services][2].

<h1><a id="registry" href="#registry" class="anchor">Registry</a></h1>

The API-X service ontology provides an information model that forms the basis of a service registry.   The service registry in API-X contains a listing of known services, and may also function as a registry of service instances.  LDP serves as an API to API-X service registry.  In the examples shown in this section, terms from the API-X service ontology are in bold.

API-X does not provide any specific query interface for its service registry, but the information model is straightforward to index and query via SPARQL in a triplestore.

<h2><a id="service-registry" href="#service-registry" class="anchor">Service Registry</a></h2>

The API-X service registry consists of at least one resource of type `apix:ServiceRegistry`.  This resource contains an enumeration of all known [services](#apixservice) in the registry.  Such a resource may be writable.  The current API-X implementation instantiates the service registry as an LDP container, for which services can be added or deleted via POST, PUT, or DELETE as specified by LDP. 

If a service has instances in a registry, then it SHOULD link to the service instance registry via `apix:hasServiceInstanceRegistry`.

<h2><a id="service-instance-registry" href="#service-instance-registry" class="anchor">Service Instance Registry</a></h2>

A service instance registry is a resource type `apix:ServiceInstanceRegistry` that describes how service instances may be discovered.  In particular, it contains information that can be used for retrieving a list of service instances from a registry implemented in a variety of ways.  The API-X service ontology defines an LDP-based service registry where service instances are enumerated in an LDP resource.  Other technologies may be used for tracking and monitoring service health, such as Apache Zookeeper, Eureka, Consul, etc.  In the case where service instances are managed by external system such as these, the corresponding API-X service instance registry may contain statements from external ontologies that describe how to access the service list in a manner appropriate to the technology.

The `apix:LdpServiceInstanceRegistry` registry links services to instances via `apix:hasServiceInstance`.

Service instance registries SHOULD link to the service it serves a registry for via `apix:isServiceRegistryFor`.

<h3><a id="example-ldp-service-registry" href="#example-ldp-service-registry" class="anchor">Example: LDP Service Registry</a></h3>

This example shows how an API-X service registry can be exposed in LDP, providing an API for registering, and deregistering services. 

Registry:

<pre>
<b>&lt;&gt; a</b> ldp:DirectContainer, <b>apix:LdpServiceRegistry</b> ;
  ldp:membershipResource &lt;&gt; ;
  ldp:hasMemberRelation apix:containsService ;
  ldp:contains &lt;Thumbnail&gt; ;
  <b>apix:containsService &lt;Thumbnail&gt; .</b>
</pre>

Service:

<pre>
<b>&lt;Thumbnail&gt; a apix:Service</b> ;
   rdfs:label “Thumbnail Service” ;
   <b>apix:canonical &lt;http://example.org/ThumbnailService&gt;</b>
</pre>   

In this example, the service registry is represented as an LDP direct container.  As such, it presents the following API:

* Create service registry entry:  `POST` or `PUT` to the service registry URI.
    * Because the registry is a DirectContainer, the relationship `apix:containsService` is added to the registry every time a service is added.
* Remove service from registry: `DELETE` to the service registry entry.  As per LDP, this will remove the `apix:containsService` relation
* Update service definition: `PUT`, `PATCH`.

<h3><a id="example-ldp-service-instance-registry" href="#example-ldp-service-instance-registry" class="anchor">Example: LDP Service Instance Registry</a></h3>

This example expands on the [service registry](#example-ldp-service-registry) example from above.  Here, we amend the service resources to be LDP indirect containers, and instances of `apix:ServiceInstanceRegistry`.  In this manner, a service resource presents leverages LDP to present an API for adding or removing service instances. 

Instantiated this way, a service resource presents the following API for adding or removing service instances:

* Create a service instance entry: `POST` or `PUT` a service instance representation to the service URI
    * Because the service resource is an LDP indirect container, the relationship `apix:hasServiceInstance` is automatically added 
    * Because the service resource is an indirect container, it expects a certain property to be present in the service instance resource.  In this case, it is `ore:describes`.
    * The service instance document is structured as a ORE ReM so that it is clear that the service instance resource therein does not have a concrete representation.  See [`apix:ServiceInstance`](#apixserviceinstance).
* Remove a service instance entry: `DELETE` to the service instance URI
* Update a service instance entry: `PUT`, `PATCH`

Service:

<pre>
<b>&lt;Thumbnail&gt;</b> <span style="color:blue;">a apix:Service</span>, ldp:IndirectContainer, <span style="color:orange;">apix:LDPServiceInstanceRegistry</span> ;
   ldp:membershipResource &lt;&gt; ;
   ldp:hasMemberRelation apix:hasServiceInstance ;
   ldp:insertedContentRelation ore:describes ;
   ldp:contains &lt;Thumbnail/instance1&gt; ;
   rdfs:label “Thumbnail Service” ;
   <span style="color:blue;">apix:hasServiceInstanceRegistry &lt;&gt; ;
   apix:hasServiceInstance &lt;Thumbnail/instance1#instance&gt; ;
   apix:canonical &lt;http://example.org/ThumbnailService&gt;</span> .
</pre>

Instance:

<pre>
&lt;Thumbnail/instance1&gt;
   ore:describes &lt;#instance&gt; .

<b>&lt;#instance&gt; a apix:ServiceInstance ;
    apix:hasEndpoint &lt;http://backend01.cloud.local:1234/Thumbnail&gt;
    apix:isServiceInstanceOf &lt;http://example.org/ThumbnailService&gt;</b> .
</pre>

<h3><a id="example-zookeeper-service-instance-registry" href="#example-zookeeper-service-instance-registry" class="anchor">Example: Zookeeper Service Instance Registry</a></h3>

This example uses an external ontology (not shown here) to demonstrate a service registry implemented in Apache Zookeeper. For any such registry to be usable in API-X, API-X must have a software component that can engage the service based on the contents of the registry in order to lookup services.  

Service:

    <Thumbnail> a ore:ResourceMap ;
       ore:describes <#service> .
    
    <#service> a apix:Service ;
       rdfs:label “Thumbnail Service ;
       apix:hasInstanceRegistry <http://example.org/zk_registry/Thumbnail>;
       apix:canonical <http://example.org/ThumbnailService>

Registry:

    <http://example.org/zk_registry/Thumbnail> a 
            zoo:ZookeeperServiceInstanceRegistry
        zoo:hasZooKeeperEnsemble: (“host:1-2181”, “host-2:2181”) ;
        zoo:hasParentNode “/service/Thumbnail” .


<h1><a id="discovery" href="#discovery" class="anchor">Discovery</a></h1>

Service discovery in API-X can occur in two different ways:

* The client follows link(s) to instances of services available on an object, and selects which one to engage with
* The client inspects the service and extension registries to select a service instance to engage.

This section focuses on the former.  The latter can may be achieved by understanding contents of the API-X [service](#ontology) and [extension][8] ontologies; a description of which is outside the scope of this document.

A repository object (RDFSource) can directly link to services instances simply by the presence of one or more [`apix:exposesServiceInstance`](#apixexposesserviceinstance) properties.  API-X does not inherently create these links inside of objects, but it may be possible via asynchronous messaging to write a service that updates the contents of an object whenever services are added or removed.

To provide a lightweight and dynamic means of linking an object to services, API-X (acting as a reverse proxy via the [intercepting][9] modality) may add a link header that identifies a document describing the services on an object.   This header uses the iana ‘service’ link rel

    Link: <http://localhost:8080/apix/resource/svc:list>; rel="service"

<h2><a id="service-document" href="#service-document" class="anchor">Service Document</a></h2>

A service document (represented in the ontology as `apix:ServiceDocument`) enumerates the service instances.  This document MUST enumerate all known service instances for extensions bound to an object, and MAY include additional descriptive information about services beyond the api-x service ontology that may be present in the associated `apix:Service` or [canonical](#apixcanonical) service resources.  This document SHOULD be an ORE ResourceMap that aggregates all service instances for an object.  This document MAY be dynamically generated by API-X, or MAY be persisted as a repository resource.  It SHOULD have an `apix:isServiceDocumentFor` property which links to the object for which it contains service instances.  Each service instance SHOULD link back to the object via `apix:isServiceDocumentFor`, and SHOULD link to the canonical service URI via `apix:isInstanceOf`.

<h3><a id="service-document-example" href="#service-document-example" class="anchor">Example</a></h3>

A client performs a HEAD on a repository resource: `http://archive.example.org/repository/foo/`:

    HEAD /repository/foo
    Host: archive.example.org

The client gets back a response.  The bold link header is introduced by API-X:

<pre>
HTTP/1.1 200 OK
Date: Tue, 26 Jul 2016 20:03:28 GMT
ETag: "0b1cd89a3bda109a145c44b0731176ba639174e8"
Last-Modified: Tue, 26 Jul 2016 19:17:34 GMT
Content-Type: image/tiff
Accept-Ranges: bytes
Link: &lt;http://www.w3.org/ns/ldp#Resource&gt;;rel="type"
Link: &lt;http://www.w3.org/ns/ldp#NonRDFSource&gt;;rel="type"
Link: &lt;http://archive.example.org/repository/foo/fcr:metadata&gt;; rel="describedby"
<b>Link: &lt;http://archive.example.org/services/foo/svc:list&gt;; rel=”service”</b>
Allow: DELETE,HEAD,GET,PUT,OPTIONS
Content-Length: 123456
Server: Jetty(9.2.3.v20140905)
</pre>

The client then follows the link to the service document

    <http://archive.example.org/services/foo/svc:list> a apix:ServiceDocument ;
      apix:isServiceDocumentFor <http://archive.example.org/repository/foo/> ;
      ore:describes <#services> .
    
    <#services>
      ore:aggregates <#a>, <#b>, <#c> ;
    
    <#a> a apix:ServiceInstance ;
      apix:serviceInstanceExposedBy <http://archive.example.org/repository/foo/> ;
      apix:isFunctionOf <http://archive.example.org/repository/foo/> ;
      apix:isServiceInstanceOf <http://example.org/ThumbnailService> ;
      apix:hasEndpoint <http://archive.example.org/services/foo/svc:Thumbnail> .
    
    <#b> a apix:ServiceInstance ;
      apix:serviceInstanceExposedBy <http://archive.example.org/repository/foo/> ;
      apix:isServiceInstanceOf <http://example.org/OAIService> ;
      apix:hasEndpoint <http://archive.example.org/services/svc:oai> .
    
    <#c> a apix:ServiceInstance a sswap:Resource ;
      apix:serviceInstanceExposedBy <http://archive.example.org/repository/foo/> ;
      apix:isServiceInstanceOf <http://sswap.example.org/services/ImageManip> ;
      apix:hasEndpoint <http://sswap.example.org/services/ImageManip> .

This example shows a service discovery document illustrating the three flavors of exposed service:

* `http://archive.example.org/services/foo/svc:Thumbnail` is a [_resource_][2] service that inherently produces a representation (e.g. a thumbnail image) tied to the identity the resource bound to it.  The presence of `apix:isFunctionOf` illustrates this relationship.
* `http://archive.example.org/services/svc:oai` is an [_repository_][3] service whose representation is not dependent on the contents of the bound resource.  In this example, the service may be an OAI service that contains the bound resource as an item within it.  Exposing it as a service allows the object to link to the OAI service that contains it.
* `http://sswap.example.org/services/ImageManip` is an [external exposed service][5] that is not mediated by API-X at all.  In this case, it identifies an SSWAP service that can be engaged by the client (according to the SSWAP protocol) to provide various image manipulation services on the object.

<h3><a id="indexing-and-messaging" href="#indexing-and-messaging" class="anchor">Indexing and messaging</a></h3>

There are significant advantages to persisting the service document for any object in the repository:

* Any addition or modification of a service document is emitted by the repository as an event, so clients that act by asynchronous messaging can act on that event
* Indexing service documents in a triple store becomes straightforward, using existing methods such as the fcrepo camel toolbox
    * Once indexed, querying for service instances, objects that have a particular service, list of instances of a particular service, etc becomes simple.
* Generation of the service document may be expensive, the repository serves as a sort of cache

Due to these these characteristics, the API-X implementation shall contain optional, separately deployable functionality to asynchronously persist and update service documents based on changes to the object (which may invalidate service binding, or add new ones), or changes to the set of installed extensions.   This functionality may possibly be deployable as a separate asynchronous message-oriented application or OSGI bundle (TBD).

<h1><a id="distributed-shared-state" href="#distributed-shared-state" class="anchor">Distributed Shared State</a></h1>

The API-X architecture should support a distributed deployment model. As such, in a distributed context, shared state of the service registry must be managed carefully. ZooKeeper is one obvious choice for this, as it avoids creating a single point of failure. If that is not a concern, a shared database would accomplish the same thing. There are two types of shared data that each node of the API-X discovery service will need to have access to:

* Basic configuration information about the cluster
* Descriptions of each service 
* For each registered service, a list of each active service instance and the corresponding HTTP endpoint

If API-X persists the service registry contents in the repository as objects, these conditions are satisfied.  

<h1><a id="notes" href="#notes" class="anchor">Notes</a></h1>

(not part of documentation, for discussion only)

<h2><a id="alternate-implementation-of-service-registry-in-ldp" href="#alternate-implementation-of-service-registry-in-ldp" class="anchor">Alternate implementation of service registry in LDP</a></h2>

The service and service instance registry examples in LDP might have a slightly cleaner representation using an indirect container for the service registry, and presenting the LDP apix:LDPServiceInstanceRegistry and apix:Service as distinct resources.  In this example, the apix:Service URI has no representation (is a hash URI), but resolves to document containing a service instance registry.  Thus, it presents almost the same API as the proposed examples, with a slightly different mapping onto LDP.

I do not believe Fedora supports this pattern.  In particular, it looks like direct or indirect relations cannot be added to hash resources.  

Registry:

<pre>
<b>&lt;&gt; a</b> ldp:IndirectContainer, <b>apix:ServiceRegistry</b> ;
  ldp:membershipResource &lt;#registry&gt; ;
  ldp:hasMemberRelation apix:containsService ;
  ldp:insertedContentRelation ore:describes ;
  ldp:contains &lt;Thumbnail&gt; ;
  <b>apix:containsService &lt;Thumbnail&gt;</b> .
</pre>

Service:

<pre>
<b>&lt;Thumbnail&gt; a</b> ore:ResourceMap, ldp:DirectContainer, <b>apix:LdpServiceInstanceRegistry</b> ;
   ore:describes &lt;#service&gt; ;
   <b>apix:isServiceInstanceRegistryFor
        &lt;#service&gt;</b> ;
   ldp:contains &lt;Thumbnail/instance1&gt; .

<b>&lt;#service&gt; a apix:Service</b> ;
   rdfs:label “Image Manipulation Service ;
   <b>apix:hasServiceInstanceRegistry &lt;&gt; ;
   apix:canonical &lt;http://example.org/ThumbnailService&gt;
   apix:hasServiceInstance &lt;Thumbnail/instance1&gt;</b>
</pre>

Instance:

<pre>
<b>&lt;Thumbnail/instance1&gt; a apix:ServiceInstance ;
    apix:hasEndpoint &lt;http://backend01.cloud.local:1234/Thumbnail&gt;;
    apix:isServiceInstanceOf &lt;http://example.org/ThumbnailService&gt; .</b>;
</pre>

---

[0]: ./extension-definition-and-binding.md "Extension Definition & Binding"
[1]: ./uris-in-apix.md#external-exposed-services "External Exposed Services - URIs in API-X"
[2]: ./uris-in-apix.md#resource-scoped-services "Resource Scoped Services - URIs in API-X"
[3]: ./uris-in-apix.md#repository-scoped-services "Repository Scoped Services - URIs in API-X"
[4]: ./apix-design-overview.md#exposing "Exposing - API-X Design Overview"
[5]: ./uris-in-apix.md#api-x-exposed-service-uris "API-X exposed service URIs - URIs in API-X"
[6]: ./execution-and-routing.md#routing "Routing - API-X Execution & Routing"
[8]: ./extension-definition-and-binding.md#extension-definition "Extension Definition - Extension Definition & Binding"
[9]: ./apix-design-overview.md#intercepting "Intercepting - API-X Design Overview"