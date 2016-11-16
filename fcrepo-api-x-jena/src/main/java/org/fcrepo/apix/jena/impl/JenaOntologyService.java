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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.apix.model.components.OntologyService;

import org.apache.jena.ontology.Individual;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses Jena to parse and provide reasoning over ontologies.
 *
 * @author apb@jhu.edu
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JenaOntologyService implements OntologyService {

    private OntologyRegistry registry;

    private OntModelSpec defaultSpec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;

    static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    static final String OWL_ONTOLOGY = "http://www.w3.org/2002/07/owl#Ontology";

    static final String OWL_IMPORTS = "http://www.w3.org/2002/07/owl#imports";

    private static final Logger LOG = LoggerFactory.getLogger(JenaOntologyService.class);

    /**
     * Jena ontology model/reasoning specification.
     *
     * @param spec THe specification
     */
    public void setOntModelSpec(final OntModelSpec spec) {
        defaultSpec = new OntModelSpec(spec);
        defaultSpec.setImportModelGetter(new NullGetter());
    }

    /** Default constructor */
    public JenaOntologyService() {
        defaultSpec.setImportModelGetter(new NullGetter());
    }

    /**
     * Underlying ontology registry.
     *
     * @param registry The underlying ontology registry.
     */
    @Reference
    public void setRegistryDelegate(final OntologyRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Ont getOntology(final URI uri) {
        return new Ont(resolveImports(ModelFactory.createOntologyModel(defaultSpec, load(uri.toString()))));
    }

    @Override
    public Ont parseOntology(final WebResource ont) {
        try (WebResource ontology = ont) {
            return new Ont(resolveImports(ModelFactory.createOntologyModel(defaultSpec, parse(ontology))));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Resolve the closure of all owl:imports statements into a union model */
    private OntModel resolveImports(final OntModel model) {

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

        // Since we manually resolved all imports, remove all owl:imports statements
        out.removeAll(null, model.getProperty(OWL_IMPORTS), null);

        return out;
    }

    private Set<String> imports(final Model model, final Set<String> resolved) {
        return model.listObjectsOfProperty(model.getProperty(OWL_IMPORTS)).mapWith(RDFNode::asResource).mapWith(
                Resource::getURI).filterDrop(uri -> resolved.contains(uri)).toSet();
    }

    private Model load(final String uri) {
        return load(URI.create(uri));
    }

    private Model load(final URI uri) {
        try (WebResource wr = registry.get(uri)) {

            return parse(wr);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Make sure Jena doesn't resolve imported ontologies, we're doing that; */
    private class NullGetter implements ModelGetter {

        @Override
        public Model getModel(final String uri) {
            return null;
        }

        @Override
        public Model getModel(final String uri, final ModelReader reader) {
            return ModelFactory.createDefaultModel();
        }

    }

    @Override
    public Ont merge(final Ontology ontology1, final Ontology ontology2) {
        final OntModel model = ModelFactory.createOntologyModel(defaultSpec);

        model.add(ont(ontology1).getBaseModel());
        model.add(ont(ontology2).getBaseModel());

        return new Ont(model);
    }

    @Override
    public Set<URI> inferClasses(final URI uri, final WebResource resource, final Ontology ontology) {

        final OntModel model = resolveImports(ont(ontology));
        model.add(parse(resource));

        final Individual individual = model.getIndividual(uri.toString());

        if (individual != null) {
            return individual
                    .listRDFTypes(false)
                    .filterKeep(Resource::isURIResource)
                    .mapWith(Resource::getURI)
                    .mapWith(URI::create)
                    .toSet();
        } else {
            LOG.info("<{}> does not make any statements about itself, " +
                    "so we can't infer anything about it nor bind extensions to it", uri);
            return Collections.emptySet();
        }

    }

    OntModel ont(final Ontology o) {
        return ((Ont) o).model;
    }

    static class Ont implements Ontology {

        final OntModel model;

        private Ont(final OntModel model) {
            this.model = model;
        }
    }

}
