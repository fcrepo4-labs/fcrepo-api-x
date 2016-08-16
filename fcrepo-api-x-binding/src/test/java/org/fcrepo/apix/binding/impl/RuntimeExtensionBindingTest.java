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

package org.fcrepo.apix.binding.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Ontology;
import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.OntologyService;

import org.junit.Ignore;
import org.junit.Test;

public class RuntimeExtensionBindingTest {

    // Basic verification that binding works as expected. ITs will exercise this more thoroughly.
    @Test
    @Ignore
    public void bindingTest() {

        final String class1 = "test:/class1";
        final String class2 = "test:/class2";
        final String class3 = "test:/class3";

        final String individual = "test:/individual";

        final RuntimeExtensionBinding toTest = new RuntimeExtensionBinding();

        final Extension E1 = mock(Extension.class);
        final WebResource E1r = mock(WebResource.class);
        when(E1.getResource()).thenReturn(E1r);
        when(E1.bindingClass()).thenReturn(URI.create(class1));
        when(E1r.toString()).thenReturn(String.format("%s=%s", individual, class1));

        final Extension E2 = mock(Extension.class);
        final WebResource E2r = mock(WebResource.class);
        when(E2.getResource()).thenReturn(E2r);
        when(E2.bindingClass()).thenReturn(URI.create(class2));
        when(E2r.toString()).thenReturn(String.format("%s=%s", individual, "test:/NOTHING"));

        final Extension E3 = mock(Extension.class);
        final WebResource E3r = mock(WebResource.class);
        when(E3.getResource()).thenReturn(E3r);
        when(E3.bindingClass()).thenReturn(URI.create(class3));
        when(E3r.toString()).thenReturn(String.format("%s=%s", individual, class3));

        final ExtensionRegistry extensionRegistry = mock(ExtensionRegistry.class);
        when(extensionRegistry.getExtensions()).thenReturn(Arrays.asList(E1, E2, E3));

        // We need to fake an ontology service with a valid merge function;
        // Our "ontology" is a string of equality statements "individualURI=classURI"
        // separated by semicolons. Merging "ontologies" means joining with a semicolon.
        final OntologyService ontologyService = new OntologyService() {

            @Override
            public Ontology merge(Ontology ontology1, Ontology ontology2) {
                return new Ont(String.join(";", ont(ontology1), ont(ontology2)));
            }

            @Override
            public Ontology parseOntology(WebResource ont) {
                return new Ont(ont.toString());
            }

            // Look for all equalities in our "ontology" that assert a class for our individual
            @Override
            public Set<URI> inferClasses(URI individual, WebResource resource, Ontology ontology) {
                final Set<URI> classes = new HashSet<>();

                for (final String spec : ont(ontology).split(";")) {
                    final String[] val = spec.split("=");

                    if (val[0].equals(individual.toString())) {
                        classes.add(URI.create(val[1]));
                    }
                }

                return classes;
            }

            @Override
            public Ontology getOntology(URI uri) {
                return null;
            }
        };

        toTest.setExtensionRegistry(extensionRegistry);
        toTest.setOntologyService(ontologyService);

        final WebResource individualResource = mock(WebResource.class);
        when(individualResource.uri()).thenReturn(URI.create(individual));

        // We expect extensions 1 and 3 to be bound

        final List<Extension> expectedExtensions = Arrays.asList(E1, E3);

        final Collection<Extension> boundExtensions = toTest.getExtensionsFor(individualResource);

        // Make sure expected and bound extensions have identical membership
        assertTrue(boundExtensions.containsAll(expectedExtensions));
        assertTrue(expectedExtensions.containsAll(boundExtensions));
    }

    private class Ont implements Ontology {

        public String content;

        public Ont(String content) {
            this.content = content;
        }
    }

    public String ont(Ontology o) {
        return ((Ont) o).content;
    }
}
