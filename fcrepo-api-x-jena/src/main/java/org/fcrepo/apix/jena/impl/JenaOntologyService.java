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

import static org.fcrepo.apix.jena.impl.Util.parse;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.OntologyService;
import org.fcrepo.apix.model.Registry;
import org.fcrepo.apix.model.WebResource;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelGetter;
import org.apache.jena.rdf.model.ModelReader;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JenaOntologyService implements OntologyService {

    private final Map<URI, URI> registeredOntoloies = new ConcurrentHashMap<>();

    private Registry registry;

    private OntModelSpec defaultSpec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";

    static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";

    public void setOntModelSpec(OntModelSpec spec) {
        defaultSpec = new OntModelSpec(spec);
        spec.setImportModelGetter(new NullGetter());
    }

    public void init() {
        index();
    }

    private void index() {
        for (final URI registeredOntologyResource : registry.list()) {
            index(registeredOntologyResource);
        }
    }

    private void index(URI registeredOntologyResource) {
        System.out.println("Indexing " + registeredOntologyResource);
        for (final URI ontologyURI : ontologyURIs(load(registeredOntologyResource))) {
            if (registeredOntoloies.containsKey(ontologyURI) && !registeredOntologyResource.equals(
                    registeredOntoloies.get(ontologyURI))) {
                throw new RuntimeException(String.format(
                        "There is already a resource for ontology %s at %s, attempted to add a new one at %s",
                        ontologyURI, registeredOntoloies.get(ontologyURI), registeredOntologyResource));
            }
            registeredOntoloies.put(ontologyURI, registeredOntologyResource);
        }
    }

    @Reference
    public void setRegistryDelegate(Registry registry) {
        this.registry = registry;
    }

    @Override
    public Ont getOntology(final URI uri) {

        URI toLoad = uri;
        System.out.println("Registered ontologoes " + registeredOntoloies);
        if (registeredOntoloies.containsKey(uri)) {
            System.out.println("get this ontologu " + uri);
            toLoad = registeredOntoloies.get(uri);
            System.out.println(toLoad);
        } else {
            System.out.println("NO ontology " + uri);
        }

        return new Ont(resolveImports(ModelFactory.createOntologyModel(defaultSpec, load(toLoad.toString()))));
    }

    @Override
    public Ont loadOntology(WebResource ont) {
        try (WebResource ontology = ont) {
            return new Ont(resolveImports(ModelFactory.createOntologyModel(defaultSpec, parse(ontology))));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OntModel resolveImports(OntModel model) {

        // if no imports, nothing to resolve
        if (model.listObjectsOfProperty(model.getProperty(OWL_IMPORTS)).toSet().isEmpty()) {
            return model;
        }

        final OntModel out = ModelFactory.createOntologyModel(defaultSpec, model.getBaseModel());

        final Set<String> resolvedImports = new HashSet<>();
        Set<String> unresolvedImports = imports(model, resolvedImports);

        while (!unresolvedImports.isEmpty()) {

            for (final String unresolved : unresolvedImports) {
                model.add(load(unresolved));
                resolvedImports.add(unresolved);

            }
            unresolvedImports = imports(model, resolvedImports);
        }

        out.removeAll(null, model.getProperty(OWL_IMPORTS), null);

        return out;
    }

    private Set<String> imports(Model model, Set<String> resolved) {
        return model.listObjectsOfProperty(model.getProperty(OWL_IMPORTS)).mapWith(RDFNode::asResource).mapWith(
                Resource::getURI).filterDrop(uri -> resolved.contains(uri)).toSet();
    }

    private Model load(String uri) {
        return load(URI.create(uri));
    }

    private Set<URI> ontologyURIs(Model ontology) {
        System.out.println("Getting ontologies from ");
        ontology.write(System.out);
        return ontology.listSubjectsWithProperty(ontology.getProperty(RDF_TYPE), OWL_ONTOLOGY)
                .mapWith(Resource::getURI)
                .mapWith(URI::create).toSet();
    }

    private Model load(URI uri) {
        try (WebResource wr = registry.get(uri)) {

            System.out.println("LOADING FROM URI " + uri + "; " + wr);
            return parse(wr);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Make sure Jena doesn't resolve imported ontologies, we're doing that;
    private class NullGetter implements ModelGetter {

        @Override
        public Model getModel(String uri) {
            return null;
        }

        @Override
        public Model getModel(String uri, ModelReader reader) {
            return ModelFactory.createDefaultModel();
        }

    }

    @Override
    public Ont merge(Ontology ontology1, Ontology ontology2) {
        final OntModel model = ModelFactory.createOntologyModel(defaultSpec);

        model.add(ont(ontology1).getBaseModel());
        model.add(ont(ontology2).getBaseModel());

        return new Ont(model);
    }

    @Override
    public Set<URI> inferClasses(URI individual, WebResource resource, Ontology ontology) {

        final OntModel model = resolveImports(ont(ontology));
        model.add(parse(resource));

        return model.getIndividual(individual.toString())
                .listRDFTypes(false)
                .mapWith(Resource::getURI)
                .mapWith(URI::create)
                .toSet();

    }

    OntModel ont(Ontology o) {
        return ((Ont) o).model;
    }

    static class Ont implements Ontology {

        final OntModel model;

        private Ont(OntModel model) {
            this.model = model;
        }
    }

    @Override
    public WebResource get(URI id) {
        return registry.get(id);
    }

    @Override
    public URI put(WebResource ontologyResource) {
        System.out.println("putting " + ontologyResource.uri());
        final URI uri = registry.put(ontologyResource);
        System.out.println("OK, put " + uri);

        index(uri);

        return uri;
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
    public void delete(URI uri) {
        registry.delete(uri);
    }
}
