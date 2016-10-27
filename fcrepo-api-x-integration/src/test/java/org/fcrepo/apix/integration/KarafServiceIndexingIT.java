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

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.fcrepo.apix.model.components.Routing;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.MavenUrlReference;

/**
 * @author apb@jhu.edu
 */
@RunWith(PaxExam.class)
public class KarafServiceIndexingIT extends ServiceBasedTest {

    @Rule
    public TestName name = new TestName();

    @Inject
    public Routing routing;

    @Override
    public String testClassName() {
        return getClass().getSimpleName();
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
                deployFile("cfg/org.fcrepo.camel.service.activemq.cfg"),
                deployFile("cfg/org.fcrepo.apix.indexing.cfg"),
                features(apixRepo, "fcrepo-api-x-indexing"));
    }

    @Test
    public void smokeTest() throws Exception {
        onServiceRequest(ex -> {
            System.out.println("\n<===");
            ex.getIn().getHeaders().entrySet().forEach(System.out::println);
            System.out.println("====>");
        });

        final URI OBJECT = client.post(routing.interceptUriFor(objectContainer)).perform().getLocation();

        System.out.println("Object is " + OBJECT);

        Thread.sleep(5000);

    }

}
