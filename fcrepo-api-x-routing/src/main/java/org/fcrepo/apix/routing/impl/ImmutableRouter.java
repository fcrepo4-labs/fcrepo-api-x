/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
 * Immutable version of {@code RoutingStub}; throws {@link UnsupportedOperationException} for mutating methods.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
class ImmutableRouter extends RoutingStub {

    ImmutableRouter(final String scheme, final String host, final int port, final RoutingPrototype delegate) {
        super.setScheme(scheme);
        super.setHost(host);
        super.setPort(port);
        super.setDiscoveryPath(delegate.getDiscoveryPath());
        super.setExposePath(delegate.getExposePath());
        super.setInterceptPath(delegate.getInterceptPath());
        super.setFcrepoBaseURI(delegate.getFcrepoBaseURI());
    }

    @Override
    public void setScheme(final String scheme) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setHost(final String host) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setPort(final int port) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setDiscoveryPath(final String path) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setInterceptPath(final String path) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setFcrepoBaseURI(final URI uri) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

    @Override
    public void setExposePath(final String path) {
        throw new UnsupportedOperationException("Instance is immutable.");
    }

}
