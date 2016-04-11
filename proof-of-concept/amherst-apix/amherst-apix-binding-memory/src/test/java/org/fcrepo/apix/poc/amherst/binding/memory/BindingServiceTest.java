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
package org.fcrepo.apix.poc.amherst.binding.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import org.fcrepo.apix.poc.amherst.binding.api.BindingService;

/**
 * @author acoburn
 * @since 9/14/15
 */
public class BindingServiceTest {

    @Test
    public void testBinding() {
        final BindingService binder = new BindingServiceImpl();

        assertTrue(binder.bind("foo", "http://example.org"));
        assertEquals(1, binder.list("foo").size());
        assertEquals("http://example.org", binder.list("foo").get(0));
        assertEquals("http://example.org", binder.findAny("foo"));
        assertNull(binder.findAny("bar"));

        assertFalse(binder.bind("foo", "http://example.org"));
        assertEquals(1, binder.list("foo").size());

        binder.bind("foo", "http://example.com");
        assertEquals(2, binder.list("foo").size());
    }
}
