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

package org.fcrepo.apix.integration;

import static org.fcrepo.apix.jena.Util.parse;
import static org.fcrepo.apix.model.Ontologies.RDF_TYPE;
import static org.fcrepo.apix.model.Ontologies.Service.CLASS_SERVICE_INSTANCE;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_HAS_ENDPOINT;
import static org.fcrepo.apix.model.Ontologies.Service.PROP_IS_SERVICE_DOCUMENT_FOR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import javax.inject.Inject;

import org.fcrepo.apix.model.WebResource;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.model.components.Registry;
import org.fcrepo.apix.model.components.Routing;
import org.fcrepo.apix.model.components.ServiceDiscovery;

import org.apache.jena.rdf.model.Model;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;

/**
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class ServiceDocumentIT implements KarafIT {

    @Inject
    public ServiceDiscovery discovery;

    @Inject
    @Filter("(org.fcrepo.apix.registry.role=default)")
    Registry repository;

    @Inject
    ExtensionRegistry extensionRegistry;

    @Inject
    Routing routing;

    @Rule
    public TestName name = new TestName();

    @Override
    public String testClassName() {
        return ServiceDocumentIT.class.getSimpleName();
    }

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    // Verifies that an object with no services or extensions produces an empty document
    @Test
    @Ignore
    public void emptyServiceDocumentTest() throws Exception {
        try (WebResource resource = discovery.getServiceDocumentFor(serviceContainer,
                "text/turtle")) {
            final Model doc = parse(resource);

            assertTrue(doc.contains(
                    null,
                    doc.getProperty(PROP_IS_SERVICE_DOCUMENT_FOR),
                    doc.getResource(serviceContainer.toString())));

            assertFalse(doc.contains(null, doc.getProperty(RDF_TYPE), doc.getResource(CLASS_SERVICE_INSTANCE)));
        }
    }

    @Test
    @Ignore
    public void exposedExternalServiceTest() throws Exception {
        // Add an extension that binds to our object
        // Now put in an extension that binds to a class from that ontology
        extensionRegistry.put(testResource(
                "objects/extension_serviceDocumentIT.ttl"));

        final URI object = postFromTestResource("objects/object_serviceDocumentIT.ttl", objectContainer);

        try (WebResource resource = discovery.getServiceDocumentFor(object, "text/turtle")) {
            final Model doc = parse(resource);

            assertTrue(doc.contains(
                    null,
                    doc.getProperty(PROP_HAS_ENDPOINT),
                    doc.getResource("http://example.org/externalService/endpoint")));
        }
    }
}
