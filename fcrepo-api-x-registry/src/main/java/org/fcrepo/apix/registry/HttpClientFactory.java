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

package org.fcrepo.apix.registry;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates configured instances of HttpClients.
 * <p>
 * Useful to containers like blueprint or sping for creating HttpClients for wiring.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class HttpClientFactory {

    private int connectTimeout = 1000;

    private int socketTimeout = 1000;

    private Map<String, String> props = new HashMap<>();

    static final Pattern pattern = Pattern.compile("^auth\\.(https?)\\.(\\d+)\\.(.+$)");

    private static final Logger LOG = LoggerFactory.getLogger(HttpClientFactory.class);

    /**
     * Timeout in miliseconds.
     *
     * @param timeout milliseconds.
     */
    public void setConnectTimeout(final int timeout) {
        this.connectTimeout = timeout;
    }

    /**
     * Socket timeout in milliseconds.
     *
     * @param timeout milliseconds.
     */
    public void setSocketTimeout(final int timeout) {
        this.socketTimeout = timeout;
    }

    /**
     * Configuration properties.
     *
     * @param conf map pf config properties.
     */
    public void setProperties(final Map<String, String> conf) {
        this.props = conf;
    }

    /**
     * Construct a new HttpClient.
     *
     * @return HttpClient impl.
     */
    public CloseableHttpClient getClient() {

        final RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout).build();

        final CredentialsProvider provider = new BasicCredentialsProvider();

        for (final AuthSpec authSpec : getAuthSpecs()) {
            LOG.debug("Using basic auth to {}://{}:{} with client",
                    authSpec.scheme, authSpec.host, authSpec.port);
            final HttpHost host = new HttpHost(authSpec.host,
                    authSpec.port, authSpec.scheme);

            provider.setCredentials(new AuthScope(host, AuthScope.ANY_REALM,
                    authSpec.scheme), new UsernamePasswordCredentials(authSpec.username(), authSpec.passwd()));
        }

        return HttpClientBuilder.create().setDefaultRequestConfig(config)
                .addInterceptorLast(new HttpRequestInterceptor() {

                    @Override
                    public void process(final HttpRequest req, final HttpContext cxt) throws HttpException,
                            IOException {
                        if (!req.containsHeader(HttpHeaders.AUTHORIZATION)) {
                            final String[] hostInfo = req.getFirstHeader(HttpHeaders.HOST).getValue().split(":");
                            final Credentials creds = provider.getCredentials(
                                    new AuthScope(new HttpHost(hostInfo[0],
                                            hostInfo.length > 1
                                                    ? Integer.valueOf(hostInfo[1])
                                                    : 80), AuthScope.ANY_REALM, "http"));

                            if (creds != null) {
                                req.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
                                        .encodeToString(String
                                                .format("%s:%s", creds
                                                        .getUserPrincipal()
                                                        .getName(), creds.getPassword()).getBytes()));
                                LOG.debug("Added auth header");
                            }
                        }
                    }
                })
                .setDefaultCredentialsProvider(
                        provider)
                .build();
    }

    List<AuthSpec> getAuthSpecs() {
        return props.keySet().stream()
                .filter(k -> k.startsWith("auth.http"))
                .filter(k -> k.endsWith(".username"))
                .map(k -> k.replaceFirst("\\.username$", ""))
                .map(spec -> new AuthSpec(spec)).collect(Collectors.toList());

    }

    class AuthSpec {

        final String spec;

        final String scheme;

        final int port;

        final String host;

        AuthSpec(final String spec) {
            this.spec = spec;
            final Matcher matcher = pattern.matcher(spec);

            if (!matcher.matches()) {
                throw new RuntimeException("Property " + spec + " does not match regex" + pattern.toString());
            }

            this.scheme = matcher.group(1);
            this.port = Integer.valueOf(matcher.group(2));
            this.host = matcher.group(3);

        }

        String passwd() {
            return props.get(spec + ".password");
        }

        String username() {
            return props.get(spec + ".username");
        }
    }
}
