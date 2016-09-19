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

import static org.fcrepo.apix.routing.impl.RoutingStub.segment;
import static org.fcrepo.apix.routing.impl.RoutingStub.terminal;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.fcrepo.apix.model.Extension;
import org.fcrepo.apix.model.Extension.Scope;
import org.fcrepo.apix.model.Extension.ServiceExposureSpec;
import org.fcrepo.apix.model.components.ExtensionRegistry;
import org.fcrepo.apix.routing.impl.ExposedServiceUriAnalyzer.ServiceExposingBinding;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author apb@jhu.edu
 */
@RunWith(MockitoJUnitRunner.class)
public class ExposedServiceUriAnalyzerTest {

    @Mock
    ExtensionRegistry extensisons;

    @Mock
    Extension extension1;

    @Mock
    Extension extension2;

    @Mock
    ServiceExposureSpec extension1Spec;

    @Mock
    ServiceExposureSpec extension2Spec;

    private static final URI fcrepoBaseURI = URI.create("http://example.org/fcrepo/rest");

    private static final URI exposureBaseURI = URI.create("http://example.org/services");

    private static final URI extension1URI = URI.create("http://example.org/extensions/1");

    private static final URI extension1ExposedAt = URI.create("extension1()");

    private static final URI extension2URI = URI.create("http://example.org/extensions/2");

    private static final URI extension2ExposedAt = URI.create("extension2()");

    private static final Set<URI> extensionURIs = new HashSet<>();

    ExposedServiceUriAnalyzer toTest;

    @Before
    public void setUp() {
        extensionURIs.clear();

        toTest = new ExposedServiceUriAnalyzer();
        toTest.setExtensionRegistry(extensisons);
        toTest.setExposeBaseURI(exposureBaseURI);
        toTest.setFcrepoBaseURI(fcrepoBaseURI);

        when(extensisons.getExtension(extension1URI)).thenReturn(extension1);
        when(extension1.isExposing()).thenReturn(true);
        when(extension1.exposed()).thenReturn(extension1Spec);
        when(extension1Spec.exposedAt()).thenReturn(extension1ExposedAt);
        when(extension1Spec.scope()).thenReturn(Scope.RESOURCE);

        extensionURIs.add(extension1URI);
        when(extensisons.list()).thenReturn(extensionURIs);

        toTest.update();
    }

    // Verifies simple matching based on URI
    @Test
    public void extensionMatchTest() {
        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
    }

    // Verifies what happens if the URI doesn't match any extension.
    @Test
    public void extensionNoMatchTest() throws Exception {
        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, URI.create("doesNotMatch()")));

        assertNull(binding);
    }

    // Verifies that the root repository resource (/) corner case works
    @Test
    public void rootResourceTest() {
        final String path = "/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
        assertTrue(binding.repositoryResourceURI.toString().startsWith(fcrepoBaseURI.toString()));
        assertTrue(binding.repositoryResourceURI.toString().endsWith(path));
    }

    // Verifies that repository resources with trailing slashes are reflected correctly in the matching resource URI
    @Test
    public void trailingSlashTest() {
        final String path = "path/ends/with/slash/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
        assertTrue(binding.repositoryResourceURI.toString().startsWith(fcrepoBaseURI.toString()));
        assertTrue(binding.repositoryResourceURI.toString().endsWith(path));
    }

    // Verifies that repository resources with no trailing slashes are reflected correctly in matching resource URI
    @Test
    public void noTrailingSlashTest() {
        final String path = "path/not/end/with/slash";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
        assertTrue(binding.repositoryResourceURI.toString().startsWith(fcrepoBaseURI.toString()));
        assertTrue(binding.repositoryResourceURI.toString().endsWith(path));
    }

    // Verifies that additional path elements after exposedAt key are OK.
    @Test
    public void additionalPathTest() {
        final String path = "some/path";

        final ServiceExposingBinding binding = toTest.match(exposureURIPlus(path, extension1ExposedAt,
                "additional/path"));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
        assertTrue(binding.repositoryResourceURI.toString().startsWith(fcrepoBaseURI.toString()));
        assertTrue(binding.repositoryResourceURI.toString().endsWith(path));
    }

    // Verifies that update() pulls in registry updates.
    @Test
    public void updateTest() {
        when(extensisons.getExtension(extension2URI)).thenReturn(extension2);
        when(extension2.isExposing()).thenReturn(true);
        when(extension2.exposed()).thenReturn(extension2Spec);
        when(extension2Spec.exposedAt()).thenReturn(extension2ExposedAt);
        when(extension2Spec.scope()).thenReturn(Scope.RESOURCE);

        extensionURIs.add(extension2URI);

        toTest.update();

        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension2ExposedAt));

        assertNotNull(binding);
        assertEquals(extension2, binding.extension);
    }

    // Verifies that update(URI) pulls in registry updates.
    @Test
    public void updateURITest() {
        when(extensisons.getExtension(extension2URI)).thenReturn(extension2);
        when(extension2.isExposing()).thenReturn(true);
        when(extension2.exposed()).thenReturn(extension2Spec);
        when(extension2Spec.exposedAt()).thenReturn(extension2ExposedAt);
        when(extension2Spec.scope()).thenReturn(Scope.RESOURCE);

        extensionURIs.add(extension2URI);

        toTest.update(extension2URI);

        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension2ExposedAt));

        assertNotNull(binding);
        assertEquals(extension2, binding.extension);
    }

    // Verify that the presence of non-matching exposure specs is irrelevant
    @Test
    public void multipleExtensionsTest() {
        when(extensisons.getExtension(extension2URI)).thenReturn(extension2);
        when(extension2.isExposing()).thenReturn(true);
        when(extension2.exposed()).thenReturn(extension2Spec);
        when(extension2Spec.exposedAt()).thenReturn(extension2ExposedAt);
        when(extension2Spec.scope()).thenReturn(Scope.RESOURCE);

        extensionURIs.add(extension2URI);

        toTest.update();

        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
    }

    @Test
    public void conflictingExtensionsTest() {
        when(extensisons.getExtension(extension2URI)).thenReturn(extension2);
        when(extension2.isExposing()).thenReturn(true);

        when(extension2.exposed()).thenReturn(extension1Spec);

        extensionURIs.add(extension2URI);

        try {

            toTest.update();
            fail("Should have thrown an exception upon update");
        } catch (final Exception e) {
            // Expected
        }

        // Now, this conflict should not affect entries already indexed.

        final String path = "some/path/";

        final ServiceExposingBinding binding = toTest.match(exposureURI(path, extension1ExposedAt));

        assertNotNull(binding);
        assertEquals(extension1, binding.extension);
    }

    private static URI exposureURI(final String path, final URI exposedAt) {
        return URI.create(
                String.format("%s/%s/%s",
                        segment(exposureBaseURI.toString()),
                        path,
                        terminal(exposedAt.getPath())));
    }

    private static URI exposureURIPlus(final String path, final URI exposedAt, final String additionalPath) {
        return URI.create(
                String.format("%s/%s/%s/%s",
                        segment(exposureBaseURI.toString()),
                        path,
                        segment(exposedAt.getPath()),
                        terminal(additionalPath)));
    }

}
