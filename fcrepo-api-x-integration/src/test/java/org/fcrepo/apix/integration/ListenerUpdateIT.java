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

import static org.fcrepo.apix.integration.KarafIT.attempt;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.net.URI;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.fcrepo.apix.model.components.Updateable;

import org.apache.camel.CamelContext;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.BundleContext;

/**
 * Verifies that the listener updates updateable services.
 *
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class ListenerUpdateIT implements KarafIT {

    @Inject
    public BundleContext cxt;

    @Rule
    public TestName name;

    // Make sure the test doesn't start before the listener context has started.
    @Inject
    @Filter("(role=apix-listener)")
    CamelContext camelContext;

    @Override
    public String testClassName() {
        return ListenerUpdateIT.class.getSimpleName();
    }

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void init() throws Exception {
        KarafIT.createContainers();
    }

    @Override
    public List<Option> additionalKarafConfig() {
        final MavenUrlReference apixRepo =
                maven().groupId("org.fcrepo.apix")
                        .artifactId("fcrepo-api-x-karaf").versionAsInProject()
                        .classifier("features").type("xml");

        return Arrays.asList(
                features(apixRepo, "fcrepo-api-x-listener"),
                deployFile("cfg/org.fcrepo.camel.service.activemq.cfg"));
    }

    // Verify that we can add an Updateable service,
    // and that it updates itself when a new object is deposited into the repo
    @Test
    public void addUpdateableTest() throws Exception {

        final BlockingQueue<URI> objects = new LinkedBlockingQueue<>();

        cxt.registerService(Updateable.class, new Updateable() {

            @Override
            public void update(final URI uri) {
                objects.add(uri);
            }

            @Override
            public void update() {
                fail("Should have called specific URI-based update");
            }
        }, new Hashtable<>());

        assertTrue(attempt(3, () -> {

            final URI OBJECT = client.post(objectContainer).perform().getLocation();

            URI uri;
            while ((uri = objects.poll(30, TimeUnit.SECONDS)) != null) {

                // Skip over objects in the queue we don't care about
                if (OBJECT.equals(uri)) {
                    break;
                }
            }
            return (OBJECT.equals(uri));
        }));

    }

}
