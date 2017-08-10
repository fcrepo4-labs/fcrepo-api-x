/*
 * Licensed to DuraSpace under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * DuraSpace licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fcrepo.apix.model;

/**
 * Class and property names from various ontologies
 *
 * @author apb@jhu.edu
 */
public interface Ontologies {

    String LDP_CONTAINS = "http://www.w3.org/ns/ldp#contains";

    String LDP_LDPR = "http://www.w3.org/ns/ldp#Resource";

    String ORE_DESCRIBES = "http://www.openarchives.org/ore/terms/describes";

    String ORE_AGGREGATES = "http://www.openarchives.org/ore/terms/aggregates";

    String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public interface Service {

        String NS = "http://fedora.info/definitions/v4/service#";

        String CLASS_SERVICE = NS + "Service";

        String CLASS_SERVICE_REGISTRY = NS + "ServiceRegistry";

        String CLASS_SERVICE_DOCUMENT = NS + "ServiceDocument";

        String CLASS_SERVICE_INSTANCE = NS + "ServiceInstance";

        String CLASS_SERVICE_INSTANCE_REGISTRY = NS + "ServiceInstanceRegistry";

        String CLASS_LDP_SERVICE_INSTANCE_REGISTRY = NS + "LdpServiceInstanceRegistry";

        String PROP_CONTAINS_SERVICE = NS + "containsService";

        String PROP_IS_FUNCTION_OF = NS + "isFunctionOf";

        String PROP_HAS_SERVICE_INSTANCE = NS + "hasServiceInstance";

        String PROP_IS_SERVICE_INSTANCE_OF = NS + "isServiceInstanceOf";

        String PROP_EXPOSES_SERVICE_INSTANCE = NS + "exposesServiceInstance";

        String PROP_SERVICE_INSTANCE_EXPOSED_BY = NS + "serviceInstanceExposedBy";

        String PROP_HAS_SERVICE_DOCUMENT = NS + "hasServiceDocument";

        String PROP_IS_SERVICE_DOCUMENT_FOR = NS + "isServiceDocumentFor";

        String PROP_HAS_ENDPOINT = NS + "hasEndpoint";

        String PROP_CANONICAL = NS + "canonical";

        String PROP_IS_SERVICE_INSTANCE_REGISTRY_FOR = NS + "isServiceInstanceRegistryFor";

        String PROP_HAS_SERVICE_INSTANCE_REGISTRY = NS + "hasServiceInstanceRegistry";
    }

    public interface Apix {

        String NS = "http://fedora.info/definitions/v4/api-extension#";

        String CLASS_EXTENSION = NS + "Extension";

        String CLASS_BINDING_FILTER = NS + "BindingFilter";

        String CLASS_INTERCEPTING_EXTENSION = NS + "InterceptingExtension";

        String CLASS_EXPOSING_EXTENSION = NS + "ServiceExposingExtension";

        String PROP_BINDS_TO = NS + "bindsTo";

        String PROP_IS_BOUND_TO = NS + "isBoundTo";

        String PROP_IS_FILTERED_BY = NS + "isFilteredBy";

        String PROP_EXPOSES_SERVICE = NS + "exposesService";

        String PROP_CONSUMES_SERVICE = NS + "consumesService";

        String PROP_EXPOSES_SERVICE_AT = NS + "exposesServiceAt";
    }
}
