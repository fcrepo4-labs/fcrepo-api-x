
package org.fcrepo.apix.model;

import java.util.Collection;

public interface ExtensionBinding {

    public Collection<Extension> getExtensionsFor(WebResource resource);
}
