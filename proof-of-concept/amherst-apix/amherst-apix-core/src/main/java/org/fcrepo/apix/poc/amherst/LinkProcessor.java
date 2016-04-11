/**
 * Copyright 2015 Amherst College
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
package org.fcrepo.apix.poc.amherst;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import static org.fcrepo.apix.poc.amherst.ApixHeaders.SERVICE_LIST;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import java.util.List;
import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;

/**
 * A Processor to reformat service names as Link headers.
 *
 * @author Aaron Coburn
 */
public class LinkProcessor implements Processor {

    /**
     * Process an exchange
     *
     * @param exchange The exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final String svcPrefix;
        final String registryPath;
        final String proxyPath;
        final String bindingPath;
        try {
            svcPrefix = exchange.getContext().resolvePropertyPlaceholders("{{apix.prefix}}");
            registryPath = exchange.getContext().resolvePropertyPlaceholders("{{rest.registry}}");
            proxyPath = exchange.getContext().resolvePropertyPlaceholders("{{rest.proxy}}");
            bindingPath = exchange.getContext().resolvePropertyPlaceholders("{{rest.binding}}");
        } catch (final Exception ex) {
            throw new RuntimeCamelException("Could not resolve property placeholders", ex);
        }

        final Message in = exchange.getIn();
        final String base = in.getHeader("CamelServletContextPath", String.class);
        final String identifier = in.getHeader(FCREPO_IDENTIFIER, String.class);
        final List<String> services = getServicesAsList(in.getHeader(SERVICE_LIST));
        final String prefix = getRegistryPrefix(registryPath, base)
                                .orElseGet(() -> getBindingPrefix(bindingPath, base)
                                    .orElseGet(() -> getProxyPrefix(proxyPath, base, identifier, svcPrefix)
                                        .orElse("/")));

        in.setHeader("Link", services.stream().map(x -> "<" + prefix + x + ">; rel=\"service\"").collect(toList()));
    }

    private static Optional<String> getRegistryPrefix(final String registryPath, final String base) {
        if (base.startsWith(registryPath)) {
            return Optional.of(registryPath + "/");
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> getBindingPrefix(final String bindingPath, final String base) {
        if (base.startsWith(bindingPath)) {
            return Optional.of("");
        } else {
            return Optional.empty();
        }
    }

    private static Optional<String> getProxyPrefix(final String proxyPath, final String base, final String identifier,
            final String svcPrefix) {
        final String partial = base + identifier;
        if (base.startsWith(proxyPath)) {
            return Optional.of(partial.endsWith("/") ? partial + svcPrefix : partial + "/" + svcPrefix);
        } else {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> getServicesAsList(final Object value) {
        if (List.class.isInstance(value)) {
            return List.class.cast(value);
        } else if (String.class.isInstance(value)) {
            return asList(String.class.cast(value));
        } else {
            return null;
        }
    }
}

