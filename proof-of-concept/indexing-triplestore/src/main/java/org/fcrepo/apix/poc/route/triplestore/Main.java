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

package org.fcrepo.apix.poc.route.triplestore;

import org.apache.camel.spring.boot.CamelSpringBootApplicationController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

/**
 * Simple standalone spring boot indexing message consumer
 *
 * @author apb@jhu.edu
 */
@SpringBootApplication
@EnableAutoConfiguration
@Configuration
@ImportResource({ "classpath*:applicationContext.xml" })
public class Main {

    /**
     * Standaline app main method.
     *
     * @param args arguments.
     */
    public static void main(final String[] args) {

        SpringApplication.run(Main.class, args)
                .getBean(CamelSpringBootApplicationController.class).run();
    }
}
