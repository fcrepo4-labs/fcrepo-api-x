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

package org.fcrepo.apix.model.components;

import java.net.URI;

import org.fcrepo.apix.model.WebResource;

/**
 * Registry which enables lookup of OWL ontologies by location or Ontology IRI.
 *
 * @author apb@jhu.edu
 */
public interface OntologyRegistry extends Registry {

    /**
     * Get an ontology by Ontology IRI or location
     *
     * @param id URI representing an Ontology IRI or location.
     * @return A resource containing a serialized ontology.
     */
    @Override
    public WebResource get(URI id);

    /**
     * Persist an ontology in the registry.
     * <p>
     * If the ontology contains a <code>&lt;ontologyIRI&gt; a owl:Ontology</code>, the ontology registry shall index
     * <code>ontologyIRI</code> such that {@link #get(URI)} with argument <code>ontologyIRI</code>.
     * </p>
     *
     * @param ontology Serialized ontology.
     * @return URI containing the location of the persisted ontology.
     */
    @Override
    public URI put(WebResource ontology);

    /**
     * Persist an ontology with a specified ontology IRI.
     * <p>
     * If the underlying ontology does not have an OWL ontology IRI declared within it, such statements will be added.
     * </p>
     *
     * @param ontology Serialized ontology.
     * @param ontologyIRI Desired ontology IRI
     * @return URI containing the location of the persisted ontology.
     */
    public URI put(WebResource ontology, URI ontologyIRI);

    /**
     * Determine if the registry contains the given location or ontology IRI. {@inheritDoc}
     *
     * @param id Ontology location or Ontology IRI.
     */
    @Override
    public boolean contains(URI id);

}
