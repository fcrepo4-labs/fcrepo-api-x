
package org.fcrepo.apix.binding.impl;

import static org.mockito.Mockito.mock;

import org.fcrepo.apix.model.OntologyService;

import org.junit.Test;

public class RuntimeExtensionBindingTest {

    @Test
    public void bindingTest() {
        final RuntimeExtensionBinding toTest = new RuntimeExtensionBinding();

        final OntologyService<String> os = mock(OntologyService.class);
    }
}
