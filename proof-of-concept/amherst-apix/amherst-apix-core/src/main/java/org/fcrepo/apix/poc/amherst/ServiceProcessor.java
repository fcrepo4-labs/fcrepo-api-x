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

import static org.apache.camel.Exchange.HTTP_PATH;
import static org.fcrepo.apix.poc.amherst.ApixHeaders.SERVICE_NAME;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_IDENTIFIER;

import java.util.Optional;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;

/**
 * A Processor to extract the API-X Service name.
 *
 * @author Aaron Coburn
 */
public class ServiceProcessor implements Processor {

    /**
     * Process an exchange
     *
     * @param exchange The exchange
     */
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        final String svcPrefix;
        try {
            svcPrefix = exchange.getContext().resolvePropertyPlaceholders("{{apix.prefix}}");
        } catch (final Exception ex) {
            throw new RuntimeCamelException("Could not resolve property placeholders", ex);
        }

        final String path = in.getHeader(HTTP_PATH, String.class);
        final Optional<String> svcName = getServiceName(path, svcPrefix);

        svcName.ifPresent(x -> in.setHeader(SERVICE_NAME, x));

        in.setHeader(FCREPO_IDENTIFIER, path.replaceAll("/" + svcPrefix + ".*$", ""));
    }

    private Optional<String> getServiceName(final String path, final String prefix) {
        final int idx1 = path.lastIndexOf("/" + prefix);
        final int idx2 = path.lastIndexOf("/");
        if (idx1 >= 0 && idx1 == idx2) {
            final String svcName = path.substring(path.lastIndexOf("/") + prefix.length() + 1);
            return Optional.of(svcName);
        }
        return Optional.empty();
    }
}
