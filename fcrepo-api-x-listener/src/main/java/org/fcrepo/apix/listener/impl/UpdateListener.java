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

package org.fcrepo.apix.listener.impl;

import static org.apache.camel.builder.PredicateBuilder.not;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_RESOURCE_TYPE;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.net.URI;
import java.util.List;

import org.fcrepo.apix.model.components.RoutingFactory;
import org.fcrepo.apix.model.components.Updateable;
import org.fcrepo.camel.FcrepoHeaders;
import org.fcrepo.camel.processor.EventProcessor;

import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens for updates to repository resources and notifies {@link Updateable}s
 *
 * @author apb@jhu.edu
 */
public class UpdateListener extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateListener.class);

    private List<Updateable> toUpdate;

    private static final String TYPE_REPOSITORY_RESOURCE = "http://fedora.info/definitions/v4/repository#Resource";

    private RoutingFactory routing;

    /**
     * Set the list of services to update
     *
     * @param list List of {@link Updateable} services to update.
     */
    public void setToUpdate(final List<Updateable> list) {
        this.toUpdate = list;
    }

    /**
     * Set the routing service.
     *
     * @param routing Routing impl.
     */
    public void setRouting(final RoutingFactory routing) {
        this.routing = routing;
    }

    @Override
    public void configure() throws Exception {

        from("{{input.uri}}").id("listener-update-apix")
                .process(new EventProcessor())

                // At the moment, this seems to be the only way to filter out messages for "hash resources"
                .filter(not(header(FCREPO_URI).contains("#")))
                .filter(header(FCREPO_RESOURCE_TYPE).contains(TYPE_REPOSITORY_RESOURCE))

                .process(USE_FCREPO_URIS)
                .process(e -> toUpdate.forEach(u -> {
                    try {
                        u.update(URI.create(e.getIn().getHeader(FCREPO_URI, String.class)));
                    } catch (final Exception x) {
                        LOG.warn(String.format("Update to <%s> failed", e.getIn().getHeader(FCREPO_URI)), x);
                    }
                }));
    }

    private final Processor USE_FCREPO_URIS = ex -> {
        final URI fcrepoUri = URI.create(ex.getIn().getHeader(FcrepoHeaders.FCREPO_URI, String.class));
        ex.getIn().setHeader(FcrepoHeaders.FCREPO_URI, routing.of(fcrepoUri).nonProxyURIFor(fcrepoUri));
    };
}
