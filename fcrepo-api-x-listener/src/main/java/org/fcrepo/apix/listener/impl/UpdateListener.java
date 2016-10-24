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

import java.net.URI;
import java.util.List;

import org.fcrepo.apix.model.components.Updateable;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class UpdateListener extends RouteBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateListener.class);

    private List<Updateable> toUpdate;

    private String fcrepoBaseURI;

    private static final String RESOURCE_PATH = "org.fcrepo.jms.identifier";

    private static final String TYPE = "org.fcrepo.jms.resourceType";

    private static final String TYPE_RESOURCE = "http://fedora.info/definitions/v4/repository#Resource";

    /**
     * Set the list of services to update
     *
     * @param list List of {@link Updateable} services to update.
     */
    public void setToUpdate(final List<Updateable> list) {
        this.toUpdate = list;
    }

    /**
     * Set the Fedora baseURI.
     *
     * @param uri Fedora baseURI.
     */
    public void setFcrepoBaseURI(final String uri) {
        this.fcrepoBaseURI = uri.replaceFirst("/$", "");
    }

    @Override
    public void configure() throws Exception {

        from("{{input.uri}}").id("listener-update-apix")
                .choice().when(e -> e.getIn().getHeader(TYPE, String.class).contains(TYPE_RESOURCE))
                .process(e -> LOG.debug("UPDATING {}", e.getIn().getHeader(RESOURCE_PATH)))
                .process(e -> toUpdate.forEach(u -> {
                    try {
                        u.update(objectURI(e));
                    } catch (final Exception x) {
                        LOG.warn(String.format("Update to <%s> failed", objectURI(e)), x);
                    }
                }));
    }

    private URI objectURI(final Exchange e) {
        return URI.create(fcrepoBaseURI + e.getIn().getHeader(RESOURCE_PATH));
    }
}
