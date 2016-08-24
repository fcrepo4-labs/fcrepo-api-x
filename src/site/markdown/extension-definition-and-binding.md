<h1><a id="top" href="#top" class="anchor">Extension Definition and Binding</a></h1>

* [Introduction](#introduction)
* [Extension Definition](#extension-definition)
    * [Extension Definition Ontology](#extension-definition-ontology)
        * [apix:exposesService](#apixexposesservice)
            * [apix:exposesServiceAtURI](#apixexposesserviceaturi)
        * [apix:consumesService](#apixconsumesservice)
        * [apix:bindsTo](#apixbindsto)
        * [apix:isFilteredBy](#apixisfilteredby)
* [Extension Registry and API](#extension-registry-and-api)
    * [Ontology registry](#ontology-registry)
* [Extension Binding](#extension-binding)
    * [OWL Reasoning](#owl-reasoning)
        * [Minimal graph for reasoning](#minimal-graph-for-reasoning)
        * [Implementations](#implementations)
    * [Filtering](#filtering)
* [Examples](#examples)
    * [Example #1](#example-1)
    * [ResourceMap example #2](#resourcemap-example-2)
    * [Filtering example #3](#filtering-example-3)
    * [Large and growing data example #4](#large-and-growing-data-example-4)
        * [Background](#background)

<h1><a id="introduction" href="#introduction" class="anchor">Introduction</a></h1>

The purpose of an API-X extension is to relate services to objects in a Fedora repository in a specific way, based on intent.  As mentioned in the [design overview][0], an extension may _expose_ a service on an object as a web resource.  This exposed service may possibly be the result of invoking a _consumed_ service (say, an image re-sizer) to object content, and providing some derivative (say, a resized image) at the exposed URI.   To the consumer of such a resource, the exposed service may resemble static content, or it may present an API with methods (POST, GET, etc), parameters, exchange of data, etc.  Likewise, an extension may consume a service in order to filter a request or response to a repository resource (e.g. invoke a validation on write requests to repository resources, where the extension may return an error HTTP code when a request would result in invalid data according to some model).  

As API-X extensions are an expression of intent, it is necessary for an extension to define the set of objects that it is intended to operate on.  

At a minimum, therefore, the definition of an API-X extension, needs to:

* Provide a description and/or name of each service it exposes, if the extension exposes a service
    * Services are exposed at URIs, provide that URI.
* Provide a description and/or name of each service it consumes
* Provide a description of objects the extension is bound to

<h1><a id="extension-definition" href="#extension-definition" class="anchor">Extension Definition</a></h1>

Extension definitions are documents that contain descriptive information about the extension, its modality, and the resources the extension binds to.  Technically speaking, an extension definition is a rdf resource containing statements from an extension definition ontology, and possibly OWL axioms or import statements (in support of [owl reasoning](#owl-reasoning) as a mechanism of extension binding).  Therefore, extension definitions can be considered to be OWL2 documents containing a mixture of axioms and instance data.

<h2><a id="extension-definition-ontology" href="#extension-definition-ontology" class="anchor">Extension Definition Ontology</a></h2>

The API-X extension definition ontology is expected to evolve during API-X development.  A terse Turtle representation of the current proposed API-X extension definition is as follows:

    @prefix apix:<http://example.org/apix#> .
    @prefix owl:<http://www.w3.org/2002/07/owl#> .
    @prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> .
    
    apix:Extension a owl:Class .
    
    apix:InterceptingExtension a owl:Class ; 
        owl:subClassOf apix:Extension .
    
    apix:ServiceExposingExtension a owl:Class ;
        owl:subClassOf apix:Extension .
    
    apix:Service a owl:Class .
    
    apix:MatchingObjectClass a owl:Class .
    
    apix:BindingFilter a owl:Class .
    
    apix:bindsTo a owl:ObjectProperty;
        rdfs:domain apix:Extension;
        rdfs:range apix:MatchingObjectClass .
    
    apix:isBoundTo a owl:ObjectProperty;
        rdfs:domain apix:Extension .
    
    apix:isFilteredBy a owl:ObjectProperty;
        rdfs:domain apix:Extension;
        rdfs:range apix:BindingFilter .
    
    apix:exposesService a owl:ObjectProperty;
        rdfs:domain apix:ServiceExposingExtension;
        rdfs:range apix:Service .
    
    apix:consumesService a owl:ObjectProperty;
        rdfs:domain apix:Extension;
        rdfs:range apix:Service .
    
    apix:exposesServiceAtURI a owl:DatatypeProperty;
        rdfs:domain apix:ServiceExposingExtension ;
        rdfs:range xsd:anyURI .

<h3><a id="apixexposesservice" href="#apixexposesservice" class="anchor">apix:exposesService</a></h3>

This property is present if the extension exposes a service (e.g. is an `apix:ServiceExposingExtension`).   `apix:exposesService` links to a service (described in a [service registry][2]).  If this property is not present, the extension is presumed to be an [intercepting][1] extension, where the extension filters the request or response to a repository resource.

<h4><a id="apixexposesserviceaturi" href="#apixexposesserviceaturi" class="anchor">apix:exposesServiceAtURI</a></h4>

If the extension exposes a service, this property contains a _string_ containing a relative or absolute URI path, or an absolute URI indicating where a service should be exposed.  This value will be used to generate the URI to the exposed service. (see [exposed service URIs][3]).   The semantics of this value are as follows:

* Relative paths are for [resource scoped services][4], where a service is a function of a given repository object  (e.g. '`svc:service`' in  <code>http://archive.example.org/services/path/to/object/<b>svc:service</b></code>)
* Absolute paths are for [repository scoped services][5] considered to be global to a given API-X domain, and not a function of a particular object 
  (e.g. '`/svc:globalService`' in <code>http://archive.example.org/services<b>/svc:globalService</b></code>)
* Full URI are for [external exposed services][6] intended to be used literally without modification, and may or may not be in the API-X domain.

<h3><a id="apixconsumesservice" href="#apixconsumesservice" class="anchor">apix:consumesService</a></h3>

This property must always be present in an extension definition, and links to a service description. This _names_ a ‘backend’ service that is invoked by an extension implementation (see [invoking services][7]).  This does not link to a service _instance_.  The Service Discovery & Binding component is responsible for mapping between services by name, and service instances (of which there may be many, and may come and go)

<h3><a id="apixbindsto" href="#apixbindsto" class="anchor">apix:bindsTo</a></h3>

This property contains an owl:Class.  Instances of repository resources must be members of this class (either inferred or directly stated, see [extension binding][8]) in order for an extension to be considered bound to a repository resource.

<h3><a id="apixisfilteredby" href="#apixisfilteredby" class="anchor">apix:isFilteredBy</a></h3>

Optional.  This points to a ‘filter’ that can be used to refine extension binding by expressing constraints outside the scope of owl reasoning.  Examples may include  a SPARQL query (an ASK query that returns a boolean for matching objects), SHACL query, or more.

<h1><a id="extension-registry-and-api" href="#extension-registry-and-api" class="anchor">Extension Registry and API</a></h1>

The Fedora _repository_ is sufficient as a  registry for extension definitions, with LDP serving as a CRUD API for extensions.   In this scenario, the API-X framework would need to know the identity of all extension definitions in the repository, and likely maintain an index or other internal state.  The existence of extensions, and awareness of CRUD operations on extension definitions would need to be discoverable by API-X via the following:

* LDP containers that contain extension definitions.  API-X may be configured with the identity of a container whose children are extension definitions.  An alternative to this is to presume by default the presence of a well-known container (e.g. `/.well-known/apix-extension-registry`).
* API-X messaging client for detecting CRUD operations on extensions.  This would allow API-X to asynchronously react to added, removed, or updated extensions.

<h2><a id="ontology-registry" href="#ontology-registry" class="anchor">Ontology registry</a></h2>

In order to provide durability for ontologies and optimize their accessibility, API-X may cache repositories, or persist ontologies in the repository.  

The current Fedora community implementation places several restrictions on object content, most notably:

1. The subject of all triples must be the URI of the repository resource that contains it, or a fragment URI based on the repository resource URI
2. Blank nodes are skolemized, and blank node content is stored in separate repository resources.

Since extension definitions are OWL2 documents potentially parseable by OWL reasoners, these restrictions may pose some challenges.  (2) is a potential problem because OWL reasoners by default do not dereference every URI encountered in an OWL document; they dereference external documents in response to `owl:imports`.   An owl reasoner would not necessarily know that it needs to dereference the skolem URI.  Blank nodes are commonly encountered in OWL documents as existential quantifiers.  (1) is potential problem because it would disallow any statements about owl classes or properties whose URI is outside the repository domain.  The technique used in the [example](#bookmark-1) for defining PCDM and ORE concepts in terms of OWL semantics (their ontologies are RDFS) would not be possible under this restriction.

The topic of persisting ontologies in the repository needs further investigation.  At present, API-X persists ontologies as binary resources (NonRDSource).  If community best practices emerge for representing ontologies as RDFSources, API-X may adopt those practices in the future.

<h1><a id="extension-binding" href="#extension-binding" class="anchor">Extension Binding</a></h1>

Extension binding is the process by which extensions are matched to repository resources.  The API-X extension definition ontology contains an `apix:bindsTo` property that indicates that the extension binds to repository resources that are members of the given class.  The resources membership in a class may either be directly stated and therefore trivially satisfied (e.g. the resource or its description<a href="#sup1"><sup>1</sup></a> contains `rdf:type` with a matching class), or inferred via OWL reasoning.

<h2><a id="owl-reasoning" href="#owl-reasoning" class="anchor">OWL Reasoning</a></h2>

 If an extension definition contains OWL axioms or `owl:imports` properties that import existing ontologies, then API-X may perform reasoning over the axioms and instance data present in the extension definition, referenced ontologies, and repository resource contents in order to determine if a given repository resource is bound to an extension.
 
<h3><a id="minimal-graph-for-reasoning" href="#minimal-graph-for-reasoning" class="anchor">Minimal graph for reasoning</a></h3>

In order to perform reasoning to determine if a resource is a member of a class bound to an extension, at minimum three pieces of information are required:

* Instance data from the extension definition, as per the extension definition ontology.  In particular, the class of resource the extension binds to.
* OWL axioms present in the extension definition document and referenced ontologies
* Instance data from the resource itself.

For a given object then, OWL reasoning for the purpose of extension binding shall occur over the union of the resource graph, the extension definition graph and the closure of all ontologies included by  `owl:import`.  Because OWL2 uses location semantics for `owl:imports`, any URI used as the object of an `owl:imports` property will be dereferenced and incorporated into the knowledge base for the sake of reasoning.  

<h3><a id="implementations" href="#implementations" class="anchor">Implementations</a></h3>

The choice of reasoner is intended to be an implementation/deployment decision.  Such reasoner SHOULD be able to reason over OWL2 RL.  Extension definitions SHOULD conform syntactically to the OWL2 RL profile.  Otherwise, a reasoner MAY ignore all statements outside the OWL2 RL profile.

It is anticipated that there will be many implementations of reasoning engines for API-X that present different sets of tradeoffs.  For example,
 
* Some implementations may perform reasoning at transaction-time, others may pre-compute binding reasoning and store the results in an index or cache
* Implementations may use different reasoners (e.g. Pellet, Fact++, Jena), each offering their own tradeoffs of completeness and performance
* Some implementations will dereference repository resources to perform reasoning, others may require a triple store
* Some implementations (particularly those with a triple store) may expand the reasoning graph beyond the resource to be bound, e.g. to support reasoning based on characteristics of LDP descendant or ancestor objects.

The initial implementation of API-X includes a simple Jena-based reasoner<a href="#sup2"><sup>2</sup></a> that dereferences resources (i.e. does not require a triple store), and performs reasoning at transaction time.  As our understanding performance characteristics and requirements and use cases of the community become more mature, additional implementations will emerge.

<h2><a id="filtering" href="#filtering" class="anchor">Filtering</a></h2>

Pure OWL2 reasoning may not be sufficient for matching objects with the desired (or required) characteristics.  To address this, API-X could include the concept of a ‘filter’.  The optional property `apix:isFilteredBy` points to a document that contains additional filtering criteria that must be satisfied in order for an object to be bound to an extension.  This filtering is in addition a match of binding class.  At the moment, API-X may consider SPARQL and [SHACL](https://w3c.github.io/data-shapes/shacl/), but has not yet defined the ontology for such filter descriptions. 

Filtering must occur at least over the closure of all owl:imports graphs. Filtering implementations may expand this.  For example, if all content of the repository is indexed in a triple store, it’s conceivable that a filtering implementation may apply a sparql query to the triple store, rather than a local graph constructed from a handful of objects.

<h1><a id="examples" href="#examples" class="anchor">Examples</a></h1>

<h2><a id="example-1" href="#example-1" class="anchor">Example #1</a></h2>

Let’s use the Portland Common Data Model ([PCDM](https://github.com/duraspace/pcdm/wiki)) as a running example for demonstrating extension binding and reasoning.  Suppose that we have a service that produces JSON document that enumerates the members of a PCDM collection as a list, for all pcdm:Collection that have an order defined via proxies (see pcdm [ordering extension](https://github.com/duraspace/pcdm/wiki#ordering-extension)).  Let’s also suppose that repository resources (containers) are modelled as PCDM Collections.

A PCDM collection object in the repository may look something like:

    @prefix ore:<http://www.openarchives.org/ore/terms/> .
    @prefix pcdm:<http://pcdm.org/models#> .
    @prefix iana:<http://www.iana.org/assignments/relation/> .
    
    <> a pcdm:Collection;
        pcdm:hasMember </A>;
        pcdm:hasMember </B>;
        pcdm:hasMember </C>;
        iana:first <#proxyA>;
        iana:last <#proxyC> .
    
    <#proxyA> a ore:Proxy;
        ore:proxyIn <#collection>;
        ore:proxyFor </A>;
        iana:next <#proxyB> .
    
    <#proxyB> a ore:Proxy;
        ore:proxyIn <#collection>;
        ore:proxyFor </B>;
        iana:previous <#proxyA>;
        iana:next <#proxyC> .
    
    <#proxyC> a ore:Proxy;
        ore:proxyIn <#collection>;
        ore:proxyFor </C>;
        iana:previous <#proxyB> 

Now, we want to create an extension definition that binds only to those collections that have a defined order.  According to the PCDM ontology, it is reasonable for this to be something like “All PCDM collections that have an `iana:first` relation that points to an `ore:Proxy`”.  To make things a little more interesting, let’s suppose that order-listing service can operate over any `ore:Aggregation`, not just a `pcdm:Collection`.  

The PCDM ontology does not define any such “collection/aggregation with an order” class, so we need to define such a class and associated axioms in the extension definition (which, recall, is considered an OWL2  document.   Let’s call this class an `OrderedAggregation`, and let’s say that this class describes “all `ore:Aggregation`s that have an `iana:first` and `iana:last` relations to `ore:Proxy`”

So let’s define our extension as follows:

    <#Extension> a apix:Extension;
        apix:exposesService <http://example.org/registry/OreList>;
        apix:exposesServiceAt "svc:List";
        apix:bindsTo <#OrderedAggregation>;
        apix:consumesService <http://example.org/registry/OreListImpl#service> .
    
    <#OrderedAggregation> a owl:Class ;
        owl:intersectionOf (
            ore:Aggregation
            [ a owl:Restriction;
                owl:onProperty iana:last;
                owl:someValuesFrom ore:Proxy
            ]
            [ a owl:Restriction;
                owl:onProperty iana:first;
                owl:someValuesFrom ore:Proxy
            ]
        ) .
 
<a id="bookmark-1" href="#bookmark-1">However</a>, we are not done yet.  The problem is that there there are no subclass/subproperty axioms for pcdm (i.e. the reasoner has no way of knowing that pcdm:Collection is a subclass of ore:Aggregation, or that pcdm:hasMember is a subproperty of ore:aggregates).  We could include the ORE and PCDM ontologies via `owl:imports` ore and pcdm, PCDM is not an OWL ontology, and lacks the axioms needed for such reasoning.  To solve this problem, we place the axioms we need directly into the extension definition<a  href="#sup3"><sup>3</sup></a>:

    ore:Proxy a rdfs:Class;
    
    ore:Aggregation a rdfs:Class;
    
    ore:aggregates a owl:ObjectProperty;
        rdfs:domain ore:Aggregation;
    
    pcdm:Collection a rdfs:Class;
        rdfs:subClassOf ore:Aggregation .
    
    pcdm:hasMember a owl:ObjectProperty;
        rdfs:domain ore:Aggregation;
        rdfs:range ore:Aggregation;
        rdfs:subPropertyOf ore:aggregates .
    
    … and so on

When a reasoner operates over the extension definition and instance object, it will properly infer that `<>` (which has an `rdf:type` of  `pcdm:Collection` in the instance document) is also an `#OrderedAggregation`.  

<h2><a id="esourcemap-example-2" href="#esourcemap-example-2" class="anchor">ResourceMap example #2</a></h2>

Now let’s change our example slightly.  Let’s suppose that we wanted our repository resources to be modelled as ORE Resource Maps that _describe_ a pcdm Collection.  The key difference is illustrated as follows:

<table border="0">
  <tr>
    <td valign="top">
        <pre>
&lt;&gt; a pcdm:Collection ;
    pcdm:hasMember &lt;/A&gt;;
    pcdm:hasMember &lt;/B&gt;;
    pcdm:hasMember &lt;/C&gt; .
    ...
        </pre>
    </td>
    <td valign="top">
        <pre>
&lt;&gt; a ore:ResourceMap ;
  ore:describes &lt;#collection&gt;

&lt;#collection&gt; a pcdm:Collection ;
    pcdm:hasMember &lt;/A&gt;;
    pcdm:hasMember &lt;/B&gt;;
    pcdm:hasMember &lt;/C&gt; .
    ...
        </pre>
    </td>
  </tr>
</table>

Remember that the semantics of `apix:bindsTo` is such that it names a class of _repository resources_.  If we want our ‘listing service’ extension  to bind to repository resources that are ReMs that describe a `pcdm:Collection` with its membership order determined by `ore:Proxy`, then we need to include axioms in the Extension definition that make it so:

<table border="0">
  <tr>
    <th>Repository resource is a <code>pdcm:Collection</code></th>
    <th>Repository resource is ReM</th>
  </tr>
  <tr>
    <td valign="top">
        <pre>
&lt;#Extension&gt; a apix:Extension;
    apix:bindsTo &lt;#OrderedAggregation&gt;;
    ...

&lt;#OrderedAggregation&gt; a owl:Class;
    owl:intersectionOf (
        ore:Aggregation
        [ a owl:Restriction;
            owl:onProperty iana:last;
            owl:someValuesFrom ore:Proxy
        ]
        [ a owl:Restriction;
            owl:onProperty iana:first;
            owl:someValuesFrom ore:Proxy
        ]
    ) .
        </pre>
    </td>
    <td valign="top">
        <pre>
&lt;#Extension&gt; a apix:Extension;
    apix:bindsTo &lt;#ReMDescribingOrderedAggregation&gt;;
  ...

&lt;#ReMDescribingOrderedAggregation&gt;
   a owl:Class
   owl:intersectionOf (
        ore:ResourceMap
        [ a owl:Restriction;
            owl:onProperty ore:describes;
            owl:someValuesFrom 
                ex:OrderedAggregation;
        ]
    ) .

&lt;#OrderedAggregation&gt; a owl:Class;
    owl:intersectionOf (
        ore:Aggregation
        [ a owl:Restriction;
            owl:onProperty iana:last;
            owl:someValuesFrom ore:Proxy
        ]
        [ a owl:Restriction;
            owl:onProperty iana:first;
            owl:someValuesFrom ore:Proxy
        ]
    ) .
        </pre>
    </td>
  </tr>
</table>
 
<h2><a id="filtering-example-3" href="#filtering-example-3" class="anchor">Filtering example #3</a></h2>

Filtering may be used to represent constraints objects bound to extensions in situations where OWL reasoning is not possible or not desirable.  For example, imagine an extension definition as follows:

    <> a apix:Extension;
        apix:bindsTo fedora:Container;
        apix:isFilteredBy <#filter> ;
        ...
    
    <#filter> a apix:SparqlFilter ;
       apix:sparqlQuery ”””
    PREFIX iana:    <http://www.iana.org/assignments/relation/> 
    PREDIX ore:     <http://www.openarchives.org/ore/terms/>
                            
    ASK { 
          ?object a ore:Aggregation ;
          ?object iana:first ?proxy1 .
          ?object iana:last ?proxy2 .
          ?proxy1 a ore:Proxy;
          ?proxy2 a ore:Proxy;  
    }
      ”””@en .



.. or even: 

    @prefix sh: <http://www.w3.org/ns/shacl#> .
    <> a apix:Extension;
        apix:bindsTo fedora:Container;
        apix:isFilteredBy <#filter> ;
        ...
    
    <#filter> a apix:SparqlFilter ;
       apix:shape [ a sh:Shape ;
           sh:scopeClass ore:Aggregation ;
           sh:property [
           sh:predicate iana:first;
           sh:minCount 1 
      ] .

Characteristics and implications of using filters may be:

* Filters would operate over the graph formed by a repository resource, extension definition, the closure of all owl:imports resources, and possibly the set of triples entailed by a reasoner (e.g. RDFS or OWL entailment).
* by choosing a fairly non-specific `apix:bindsTo` (e.g. to `owl:Thing`, or `fedora:Resource`), the logic of extension binding can mostly be placed in the filter (e.g. SPARQL or SHACL). 
* …

<h2><a id="large-and-growing-data-example-4" href="#large-and-growing-data-example-4" class="anchor">Large and growing data example #4</a></h2>

<h3><a id="background" href="#background" class="anchor">Background</a></h3>

It is very common in the sciences for a “data set” to be both large and growing.  By large I mean that an individual collection may contain several million or more individual objects (granules in NASA/NOAA speak) each of which may itself be many GB in size.  By growing I mean that over time the number of objects increases as new data is acquired by the instrument and automatically processed using a very standard algorithm and added to the collection.  Using the MODIS Terra 500m Level 2 (MOD10L2) snow product as a concrete example, this data set grows by one granule every 5 minutes and has ever since the year 2000.  At this point there are roughly 1.7 million granules.

As used here the term data set will be assumed to be equivalent to a Data Conservancy (DCS)  collection and the term granule will be assumed to be equivalent to a DCS data item.  

Given that Fedora performance is currently known to be slow when an object is related to very many other objects as would be the case here where each granule is explicitly a part of the overall MOD10L2 data collection; API-X services could be used as a cache that stores metadata about each granule and its associated collection in the Fedora repository so that other technologies (e.g., RDBMS, SOLR, etc.) can be used to provide faster response times.  In this case, the full suite of CRUD capabilities will need to be handled through the API-X caching service; if we presume that the underlying URL structure is a simple RESTful /collection/granule (e.g., here it might be MOD10_L2/MOD10_L2.A2000058.0230.006.2016058064802), the entire CRUD suite can be handled by Get, Post, Put, and Delete to the regular object URL even though a lot of non-baseline Fedora 4 stuff needs to be handled under the hood by the extension.  In addition, there might be several other non-CRUD operations that would be useful to implement as part of such an extension.  A few possibilities are mentioned in the draft extension definition below for which only the 2nd and 3rd services have been defined.  The other services would be defined similarly.


    <#fastCache> a apix:Extension;
        apix:exposesService </Collection/Granule>;
        apix:exposesService <#queryCollections>;
        apix:exposesService <#queryGranulesForCollection>;
        apix:exposesService <#getCollectionInfo>;
        apix:exposesService <#getGranuleInfo> .
    
    <#queryCollections> a apix:serviceExposingExtension;
        apix:bindsTo <#Collections>;
        apix:exposesServiceAtURI “svc:queryCollections”;
        apix:consumesService <http://example.org/registry/Collections#query>.
    
    <#Collections> a owl:Class;
        Rdf:type dcs:collection .
    
    <#queryCollections> a apix:serviceExposingExtension;
       apix:bindsTo <#DataItems>;
       apix:exposesServiceAtURI “svc:queryGranulesForCollection”;
       apix:consumesService <http://example.org/registry/Granules#query>.
    
    <#DataItems> a owl:Class;
       Rdf:type dcs:dataItem .


Describing the under the hood operation of each of the CRUD operations follows: 

Create - Adding a new granule to the collection

Writes to the repository via the extension would populate the cache in conjunction, perhaps with asynchronous messaging, with the repository.  It can be assumed that the cache would store not only identifiers and locators for the objects stored in the Fedora repository; but also a set of metadata suitable for querying those types of objects using the metadata fields common within the community being served.

Read - Accessing a granule in the collection or its associated metadata 

Reads would return the cached values unless there’s reason to believe the cache is invalid or the request cannot be satisfied by the cache.  Access to the files that comprise the content of the granule would be passed through to the repository.

Update - Updating an existing granule

There are two cases here:

1. Only the metadata about the granule is being updated, in which case this happens without updating the version of the granule itself.  Here again both the cache and the repository are updated (perhaps asynchronously) and a record of the update in the granule’s provenance trail is made.
1. One or more of the files (binaries in Fedora 4 speak) are replaced, perhaps in conjunction with other metadata changes.  The metadata is handled as per 1 above. Fedora 4 services are used to ingest the new binaries into the repository along with a record of the update in that granule’s provenance trail.  Repository policies would determine the fate of the replaced binaries (i.e., maintained as old versions or deleted for example).

Delete - Deleting an existing granule

The content of the cache would be removed in conjunction with the repository (perhaps asynchronously).  Whether the delete actually removed the data from the repository or merely hides it would be based on repository policies.


---

<a id="sup1" href="#sup1"><sup>1</sup></a> For binary resources, its description is used for the purpose of reasoning

<a id="sup2" href="#sup2"><sup>2</sup></a> Jena provides simple but relatively fast reasoners that implement only a subset of OWL2 RL

<a id="sup3" href="#sup3"><sup>3</sup></a> Note: This essentially allows us to build a local ontology where terms have different semantics from those published in an authoritative ontology.  While this works well from a pragmatic sense and is very simple to implement, there may be consequences that would motivate a different solution.  API-X should seek out best practice in this situation.


[0]: ./apix-design-overview.md "API-X Design Overview"
[1]: ./apix-design-overview.md#intercepting "Intercepting Extension - API-X Design Overview"
[2]: ./service-discovery-and-binding.md#service-registry "Service Registry - API-X Service Discovery & Binding"
[3]: ./uris-in-apix.md#api-x-exposed-service-uris "API-X exposed service URIs - URIs in API-X"
[4]: ./uris-in-apix.md#resource-scoped-services "Resource Scoped Services - URIs in API-X"
[5]: ./uris-in-apix.md#repository-scoped-services "Repository Scoped Services - URIs in API-X"
[6]: ./uris-in-apix.md#external-exposed-services "External Exposed Services - URIs in API-X"
[7]: ./service-discovery-and-binding.md "API-X Service Discovery & Binding"
[8]: ./extension-definition-and-binding.md#extension-binding "Extension Binding - API-X Extension Definition & Binding"