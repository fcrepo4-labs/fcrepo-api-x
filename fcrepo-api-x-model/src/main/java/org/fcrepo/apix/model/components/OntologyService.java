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
import java.util.Set;

import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.WebResource;

/**
 * Serializes and deserializes ontologies, and performs reasoning.
 * <p>
 * Instances of {@link Ontology} should only be used with the same {@link OntologyService}.
 * </p>
 *
 * @author apb@jhu.edu
 */
public interface OntologyService {

    /**
     * Parse resource into an ontology object.
     * <p>
     * The ontology object is a black box, containing some underlying notion of a parsed ontology suitable for
     * inference.
     * </p>
     *
     * @param ont Serialization of an ontology.
     * @return The parsed ontology.
     */
    public Ontology parseOntology(WebResource ont);

    /**
     * Retrieve an ontology from an underlying registry.
     * <p>
     * If the ontology service has been configured with an an ontology registry, the registry will be used to retrieve
     * the ontology at the given URI.
     * </p>
     *
     * @param uri location or Ontology IRI.
     * @return The parsed ontology. If none exist, a runtime exception will be thrown.
     */
    public Ontology getOntology(URI uri);

    /**
     * Merge two ontologies.
     *
     * @param ontology1 Ontology to merge.
     * @param ontology2 Ontology to merge.
     * @return The union of both ontologies.
     */
    public Ontology merge(Ontology ontology1, Ontology ontology2);

    /**
     * Infer the classes of a given individual.
     * <p>
     * Performs A-box reasoning of an individual with respect to a given ontology.
     * </p>
     *
     * @param individual URI of the individual, as mat be found in the provided resource.
     * @param resource Resource containing serialized RDF.
     * @param ontology Ontology that shall be used for reasoning.
     * @return A set of all matching classes, or empty if none.
     */
    public Set<URI> inferClasses(URI individual, WebResource resource, Ontology ontology);

}
