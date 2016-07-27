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

package org.fcrepo.apix.impl.registry;

import java.net.URI;
import java.util.Set;

import org.fcrepo.apix.model.OntologyService;
import org.fcrepo.apix.model.Registry;
import org.fcrepo.apix.model.WebResource;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ModelGetter;
import org.apache.jena.rdf.model.ModelReader;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
public class JenaOntologyService implements OntologyService<OntModel> {

    private Registry registry;

    private OntModelSpec defaultSpec = OntModelSpec.OWL_MEM_MICRO_RULE_INF;

    public void setOntModelSpec(OntModelSpec spec) {
        defaultSpec = spec;
    }

    @Reference
    public void setOntologyRegistry(Registry registry) {
        this.registry = registry;
    }

    @Override
    public OntModel getOntology(URI uri) {
        final OntModelSpec spec = new OntModelSpec(defaultSpec);
        spec.setImportModelGetter(new RegistryGetter());

        final OntModel model = ModelFactory.createOntologyModel(spec);
        model.add(load(uri.toString()));

        return model;

    }

    private Model load(String uri) {
        try (WebResource wr = registry.get(URI.create(uri))) {

            return parse(wr);

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Model parse(WebResource r) {
        final Model model = ModelFactory.createDefaultModel();

        final Lang lang = RDFLanguages.contentTypeToLang(r.contentType());
        RDFDataMgr.read(model, r.representation(), r.uri().toString(), lang);
        return model;
    }

    private class RegistryGetter implements ModelGetter {

        @Override
        public Model getModel(String uri) {
            return load(uri);
        }

        @Override
        public Model getModel(String uri, ModelReader reader) {
            // We don't use the reader
            return load(uri);
        }

    }

    @Override
    public OntModel merge(OntModel ontology1, OntModel ontology2) {
        final OntModelSpec spec = new OntModelSpec(defaultSpec);
        final OntModel model = ModelFactory.createOntologyModel(spec);

        model.add(ontology1);
        model.add(ontology2);

        return model;
    }

    @Override
    public Set<URI> inferClasses(URI individual, WebResource resource, OntModel ontology) {
        final OntModelSpec spec = new OntModelSpec(defaultSpec);
        final OntModel model = ModelFactory.createOntologyModel(spec);

        model.add(ontology);
        model.add(parse(resource));

        return model.getIndividual(individual.toString())
                .listRDFTypes(false)
                .mapWith(Resource::getURI)
                .mapWith(URI::create)
                .toSet();

    }

}
