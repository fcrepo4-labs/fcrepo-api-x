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

import java.net.URI;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyRegistry;
import org.fcrepo.apix.model.components.OntologyService;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.Updateable;

import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persists ontologies if not present in a registry.
 *
 * @author apb@jhu.edu
 */
public class PersistingOntologyRegistry extends WrappingRegistry implements OntologyRegistry, Updateable {

    static final Logger LOG = LoggerFactory.getLogger(PersistingOntologyRegistry.class);

    private boolean doPersist = true;

    private OntologyRegistry delegate;

    private OntologyService ontologyService;

    private ExtensionRegistry extensionRegistry;

    private Registry world;

    /**
     * Determine whether ontologies shall be persisted to registry when imported.
     *
     * @param doPersist Will import ontologies if true.
     */
    public void setDoPersist(final boolean doPersist) {
        this.doPersist = doPersist;
    }

    /**
     * Underlying ontology registry delegate.
     *
     * @param registry Ontology registry.
     */
    @Reference
    public void setOntologyRegistry(final OntologyRegistry registry) {
        this.delegate = registry;
        setRegistryDelegate(registry);
    }

    /**
     * Underlying ontology service.
     *
     * @param svc The ontology service.
     */
    @Reference
    public void setOntologyService(final OntologyService svc) {
        this.ontologyService = svc;
    }

    /**
     * Underlying extension registry.
     *
     * @param registry The registry
     */
    @Reference
    public void setExtensionRegistry(final ExtensionRegistry registry) {
        this.extensionRegistry = registry;
    }

    /**
     * Underlying general registry for resolving ontologies.
     *
     * @param registry The registry
     */
    @Reference(target = "(org.fcrepo.apix.registry.role=default)")
    public void setGeneralRegistry(final Registry registry) {
        this.world = registry;
    }

    @Override
    public URI put(final WebResource ontology, final URI ontologyIRI) {
        return delegate.put(ontology, ontologyIRI);
    }

    @Override
    public WebResource get(final URI uri) {

        if (doPersist && !delegate.contains(uri)) {
            try (WebResource ontology = world.get(uri)) {
                LOG.info("Persisting ontology <{}> as <{}>", ontology.uri(), uri);
                delegate.put(ontology, uri);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        return delegate.get(uri);
    }

    @Override
    public void update() {
        extensionRegistry.list().forEach(this::update);
    }

    @Override
    public void update(final URI inResponseTo) {
        if (doPersist && extensionRegistry.hasInDomain(inResponseTo)) {
            try (WebResource extension = extensionRegistry.get(inResponseTo)) {
                LOG.debug("Loading ontologies from <{}>", inResponseTo);
                ontologyService.parseOntology(extension);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
