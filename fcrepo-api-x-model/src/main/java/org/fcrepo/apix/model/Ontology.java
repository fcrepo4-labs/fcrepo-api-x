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

package org.fcrepo.apix.model;

/**
 * Marker interface for ontologies.
 * <p>
 * Contains various namespaces and values of significant ontologies.
 * </p>
 */
public interface Ontology {

    public static final String APIX_NS = "http://example.org/apix#";

    public static final String LDP_NS = "http://www.w3.org/ns/ldp#";

    public static final String EXTENSION_BINDING_CLASS = APIX_NS + "bindsTo";

}
