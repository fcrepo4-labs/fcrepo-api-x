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

package org.fcrepo.apix.jena.impl;

import static org.fcrepo.apix.jena.Util.parse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.Updateable;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFLanguages;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows lookup by ontologyURI as well as location.
 * <p>
 * For any ontology in underlying registry, will allow retriving that ontology by its location URI, or ontologyIRI
 * (which may be different). To do so, it maintains an in-memory map of ontology URIs to location. It is an error to
 * have the same ontology IRI in more than one entry in the registry.
 * </p>
 * <p>
 * This indexes all ontologies upon initialization, and maintains the index in response to {@link #put(WebResource)}
 * or {@link #put(WebResource, URI)}.
 * </p>
 * <p>
 * For ontology registries backed by an LDP container in the repository, this class may be used by an asynchronous
 * message consumer to update the ontology registry in response to ontologies added/removed manually by clients via
 * LDP interactions with the repository.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class LookupOntologyRegistry implements OntologyRegistry, Updateable {

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";

    private final Map<URI, URI> ontologyIRIsToLocation = new ConcurrentHashMap<>();

    private Registry registry;

    private static final Logger LOG = LoggerFactory.getLogger(LookupOntologyRegistry.class);

    /**
     * Set underlying registry containing ontology resources.
     *
     * @param delegate Registry containing ontology resources.
     */
    @Reference
    public void setRegistryDelegate(final Registry delegate) {
        this.registry = delegate;
    }

    @Override
    public WebResource get(final URI id) {
        return registry.get(ontologyIRIsToLocation.getOrDefault(id, id));
    }

    @Override
    public URI put(final WebResource ontologyResource) {
        return index(registry.put(ontologyResource));
    }

    @Override
    public URI put(final WebResource ontologyResource, final boolean asBinary) {
        return index(registry.put(ontologyResource, asBinary));
    }

    @Override
    public URI put(final WebResource ontologyResource, final URI ontologyIRI) {
        final Model model = parse(ontologyResource);
        model.add(model.getResource(ontologyIRI.toString()), model.getProperty(RDF_TYPE), model.getResource(
                OWL_ONTOLOGY));
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        model.write(out, RDFLanguages.contentTypeToLang(ontologyResource.contentType()).getName());

        return put(WebResource.of(new ByteArrayInputStream(out.toByteArray()), ontologyResource.contentType(),
                ontologyResource.uri(), ontologyResource.name()));
    }

    @Override
    public boolean canWrite() {
        return registry.canWrite();
    }

    @Override
    public Collection<URI> list() {
        return registry.list();
    }

    @Override
    public void delete(final URI uri) {
        registry.delete(uri);
        update();
    }

    /** Try infinitely to read contents of registry in order to index ontologyIRIs */
    public void init() {

        for (boolean indexed = false; !indexed;) {
            try {
                update();
                indexed = true;
            } catch (final Exception e) {
                LOG.warn("Indexing existing ontologies failed, retrying: ", e);
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException i) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private URI index(final URI ontologyLocation) {
        LOG.debug("Indexing ontology at {}", ontologyLocation);

        for (final URI ontologyIRI : ontologyURIs(load(ontologyLocation))) {
            if (ontologyIRIsToLocation.containsKey(ontologyIRI) && !ontologyLocation.equals(
                    ontologyIRIsToLocation.get(ontologyIRI))) {
                throw new RuntimeException(String.format(
                        "There is already a resource for ontology %s at %s, " +
                                "attempted to add a new one at %s",
                        ontologyIRI,
                        ontologyIRIsToLocation.get(ontologyIRI), ontologyLocation));
            }

            LOG.debug("Indexing ontology IRI {} which resolves to location {}", ontologyIRI, ontologyLocation);

            ontologyIRIsToLocation.put(ontologyIRI, ontologyLocation);
        }
        return ontologyLocation;
    }

    Set<URI> ontologyURIs(final Model ontology) {

        final Set<URI> ontologyIRIs = ontology
                .listSubjectsWithProperty(ontology.getProperty(RDF_TYPE), ontology.getResource(OWL_ONTOLOGY))
                .mapWith(Resource::getURI)
                .mapWith(URI::create).toSet();

        return ontologyIRIs;
    }

    private Model load(final URI uri) {
        try (WebResource wr = registry.get(uri)) {
            return parse(wr);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean contains(final URI id) {
        return ontologyIRIsToLocation.containsKey(id) || registry.contains(id);
    }

    @Override
    public boolean hasInDomain(final URI uri) {
        return registry.hasInDomain(uri);
    }

    @Override
    public void update() {
        final Map<URI, URI> iriMap = registry.list().stream()
                .flatMap(loc -> ontologyURIs(load(loc)).stream()
                        .map(uri -> new SimpleEntry<URI, URI>(uri, loc)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, CONFLICT));

        ontologyIRIsToLocation.putAll(iriMap);

        ontologyIRIsToLocation.keySet().removeIf(iri -> !iriMap.containsKey(iri));

        ontologyIRIsToLocation.entrySet().forEach(e -> LOG.debug(
                "Indexing ontology IRI {} which resolves to location {}", e.getKey(), e.getValue()));
    }

    private static BinaryOperator<URI> CONFLICT = (v1, v2) -> {
        throw new RuntimeException(
                String.format("Resources %s and %s both define the same ontology IRI", v1, v2));
    };

    @Override
    public void update(final URI inResponseTo) {
        if (registry.hasInDomain(inResponseTo)) {
            update();
        }
    }
}
