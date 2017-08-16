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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.commons.io.input.NullInputStream;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests relating to streaming content proxied by API-X
 *
 * @author esm
 */
@RunWith(PaxExam.class)
public class StreamingIT implements KarafIT {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingIT.class);

    private static final URI APIX_BASE_URI = URI.create(apixBaseURI);

    private static final String INTERCEPT_ROUTE_ID = "execute-intercept";

    private static final String CONTEXT_NAME = "apix-core";

    private static final String CONTEXT_ROLE = "routing-context";

    private URI binaryContainer;

    private URI binaryResource;

    private String binaryResourceSha;

    private static MessageDigest sha1;

    @Rule
    public TestName name = new TestName();

    @Inject
    @Filter("(role=" + CONTEXT_ROLE + ")")
    private CamelContext ctx;

    @Override
    public String testClassName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String testMethodName() {
        return name.getMethodName();
    }

    @BeforeClass
    public static void initMessageDigest() throws NoSuchAlgorithmException {
        sha1 = MessageDigest.getInstance("SHA-1");
    }

    /**
     * Creates a container and a binary resource of 2MiB + 1 bytes long. Retrieves checksum of the resource.
     *
     * @throws FcrepoOperationFailedException if unexpected things go wrong
     * @throws IOException if unexpected things go wrong
     * @throws URISyntaxException if unexpected things go wrong
     */
    @Before
    public void initBinaryResources() throws FcrepoOperationFailedException, IOException, URISyntaxException {
        binaryContainer = URI.create(String.format("%s%s", fcrepoBaseURI, testClassName() + "/binaries/"));

        // Create container if it doesn't already exist
        if (!resourceExists(binaryContainer)) {
            try (FileInputStream body = new FileInputStream(
                    new File(testResources, "objects/binary_container.ttl"));
                    FcrepoResponse r = client.put(binaryContainer)
                            .body(body, "text/turtle")
                            .perform()) {
                assertEquals("Failed to create binary container '" + binaryContainer + "'",
                        201, r.getStatusCode());
            }
        }

        // Create the binary resource if it doesn't already exist
        final URI expectedBinaryResource = appendToPath(binaryContainer, "large-binary");
        if (!resourceExists(expectedBinaryResource)) {
            LOG.warn("Expected resource did not exist {}", expectedBinaryResource);
            try (InputStream body = new NullInputStream((2 * 1024 * 1024) + 1)) {
                binaryResource = postFromStream(
                        body, binaryContainer,
                        "application/octet-stream", "large-binary");
            } catch (final Exception e) {
                fail(String.format("Failed to create binary LDPR: %s", e.getMessage()));
            }
        } else {
            binaryResource = expectedBinaryResource;
        }

        // Retrieve the checksum calculated by Fedora
        binaryResourceSha = ModelFactory.createDefaultModel()
                .read(client
                        .get(appendToPath(binaryResource, "/fcr:metadata"))
                        .accept("application/rdf+xml")
                        .perform().getBody(),
                        null)
                .listObjectsOfProperty(
                        ResourceFactory
                                .createProperty("http://www.loc.gov/premis/rdf/v1#", "hasMessageDigest"))
                .mapWith((digestValue) -> digestValue.toString().substring("urn:sha1:".length()))
                .next();
        assertNotNull("Missing http://www.loc.gov/premis/rdf/v1#hasMessageDigest on " +
                appendToPath(binaryResource, "/fcr:metadata").toString(), binaryResourceSha);

    }

    /**
     * Smoke test insuring Karaf is doing what we think it is doing
     */
    @Before
    public void verifyContextAndRoute() {
        assertNotNull("No context", ctx);
        assertEquals("Unexpected context " + ctx.getName(), CONTEXT_NAME, ctx.getName());
        assertNotNull("No route (ctx name: " + ctx.getName() + ")", ctx.getRouteDefinition(INTERCEPT_ROUTE_ID));
    }

    /**
     * Verify the binary can be retrieved from Fedora. The request should <em>not</em> be intercepted.
     *
     * @throws Exception if unexpected things go wrong
     */
    @Test
    public void testRetrieveLargeBinaryFromFedora() throws Exception {

        // Record 'true' if the intercepting route is triggered
        final AtomicBoolean intercepted = new AtomicBoolean(false);
        ctx.getRouteDefinition(INTERCEPT_ROUTE_ID).adviceWith((ModelCamelContext) ctx, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveAddFirst().process((ex) -> intercepted.set(true));
            }
        });

        final long expectedSize = (2 * 1024 * 1024) + 1;
        final long actualSize;
        final String actualDigest;

        try (FcrepoResponse r = client.get(binaryResource).perform();
                DigestInputStream body = new DigestInputStream(r.getBody(), sha1)) {
            actualSize = drain(body);
            actualDigest = asHex(body.getMessageDigest().digest());
        }

        // The resource can be retrieved intact
        assertEquals(expectedSize, actualSize);
        assertEquals(binaryResourceSha, actualDigest);

        // And the request was not proxied by API-X
        assertFalse(String.format("Unexpected interception of a Fedora resource URI %s by route %s",
                binaryResource.toString(), INTERCEPT_ROUTE_ID), intercepted.get());
    }

    /**
     * Verify the binary can be retrieved through the API-X proxy. The request should be intercepted and proxied by
     * API-X.
     *
     * @throws Exception if unexpected things go wrong
     */
    @Test
    public void testRetrieveLargeBinaryFromApix() throws Exception {

        // Record 'true' if the intercepting route is triggered
        final AtomicBoolean intercepted = new AtomicBoolean(false);
        ctx.getRouteDefinition(INTERCEPT_ROUTE_ID).adviceWith((ModelCamelContext) ctx, new AdviceWithRouteBuilder() {

            @Override
            public void configure() throws Exception {
                weaveAddFirst().process((ex) -> intercepted.set(true));
            }
        });

        final long expectedSize = (2 * 1024 * 1024) + 1;
        final long actualSize;
        final String actualDigest;

        final URI proxiedResource = proxied(binaryResource);
        try (FcrepoResponse r = KarafIT.attempt(30, () -> client.get(proxiedResource).perform());
                DigestInputStream body = new DigestInputStream(r.getBody(), sha1)) {
            actualSize = drain(body);
            actualDigest = asHex(body.getMessageDigest().digest());
        }

        // The request _was_ proxied by API-X
        assertTrue(String.format("Expected the retrieval of %s to be proxied by API-X, route id %s",
                proxiedResource, INTERCEPT_ROUTE_ID), intercepted.get());

        // And resource can be retrieved intact
        assertEquals(expectedSize, actualSize);
        assertEquals(binaryResourceSha, actualDigest);
    }

    /**
     * Returns true if the URI exists (i.e. responds with a 200 to a HEAD request).
     *
     * @param resource some HTTP resource
     * @return true if the resource exists, false otherwise
     * @throws IOException if there is an error determining whether the resource exists
     */
    private boolean resourceExists(final URI resource) throws IOException {
        try (FcrepoResponse r = client.head(resource).perform()) {
            if (r.getStatusCode() == 200) {
                return true;
            }
        } catch (final FcrepoOperationFailedException e) {
            // Probably the resource doesn't exist.
            LOG.debug("Error retrieving resource '" + resource + "': " + e.getMessage(), e);
        }

        return false;
    }

    /**
     * Reads the input stream to exhaustion and returns the number of bytes read.
     *
     * @param in the stream to exhaust
     * @return the number of bytes read
     * @throws IOException
     */
    private static long drain(final InputStream in) throws IOException {
        final byte[] buf = new byte[1024 * 128];
        long size = 0;

        for (int i = in.read(buf, 0, buf.length); i > -1; i = in.read(buf, 0, i)) {
            size += i;
        }

        return size;
    }

    /**
     * Coverts the supplied byte array to a String hexadecimal representation, starting with the most significant bit.
     *
     * @param digest a byte array containing a message digest
     * @return a hexadecimal string representation of the message digest
     */
    private static String asHex(final byte[] digest) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            buf.append(
                    String.format("%02x", Byte.toUnsignedInt(digest[i])));
        }

        return buf.toString();
    }

    /**
     * Assuming the supplied URI targets the <em>Fedora repository</em> (i.e. <em>a URI that is not proxied by
     * API-X</em>), returns an equivalent URI that targets the same resource through the API-X proxy.
     *
     * @param toProxy a URI that targets an un-proxied Fedora resource
     * @return an equivalent URI targeting the proxied Fedora resource
     * @throws URISyntaxException
     */
    private static URI proxied(final URI toProxy) throws URISyntaxException {
        if (isProxied(toProxy)) {
            return toProxy;
        }
        return appendToPath(APIX_BASE_URI, toProxy.getPath());
    }

    /**
     * Appends the path to the URI. All other components of the URI are preserved.
     *
     * @param uri the URI with the path being appended to
     * @param toAppend the path to be appended to the URI
     * @return a new URI with a path component ending with {@code toAppend}
     * @throws URISyntaxException
     */
    private static URI appendToPath(final URI uri, final String toAppend) throws URISyntaxException {
        return new URI(uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath() + toAppend,
                uri.getRawQuery(),
                uri.getRawFragment());
    }

    /**
     * Returns true if the supplied URI will be proxied by API-X
     *
     * @param uri a candidate uri that might be proxied API-X
     * @return true if the supplied URI will be proxied by API-X, false otherwise
     */
    private static boolean isProxied(final URI uri) {
        return uri.getScheme().equals(APIX_BASE_URI.getScheme()) && uri.getHost().equals(APIX_BASE_URI.getHost()) &&
                uri.getPort() == APIX_BASE_URI.getPort();
    }

}
