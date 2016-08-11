
package org.fcrepo.apix.jena.impl;

import org.fcrepo.apix.model.WebResource;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;

class Util {

    static Model parse(WebResource r) {
        final Model model = ModelFactory.createDefaultModel();

        final Lang lang = RDFLanguages.contentTypeToLang(r.contentType());
        RDFDataMgr.read(model, r.representation(), r.uri() != null ? r.uri().toString() : null, lang);
        return model;
    }
}
