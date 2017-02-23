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

package org.fcrepo.apix.routing.impl;

import java.net.URI;

/**
 * @author apb@jhu.edu
 */
public class RoutingStub extends RoutingPrototype {

    private String scheme;

    private String host;

    private int port;

    /**
     * Set the API-X scheme.
     *
     * @param scheme typically http or https
     */
    public void setScheme(final String scheme) {
        this.scheme = scheme;
    }

    /**
     * Set the API-X host.
     *
     * @param host IP or dns name
     */
    public void setHost(final String host) {
        this.host = host;
    }

    /**
     * Set the API-X port.
     *
     * @param port port number.
     */
    public void setPort(final int port) {
        this.port = port;
    }

    URI exposedBaseURI() {
        if (port == 80) {
            return URI.create(String.format("%s://%s/", scheme, host));
        }
        return URI.create(String.format("%s://%s:%s/", scheme, host, port));
    }

}
