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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Contains a representation of a web resource.
 *
 * @author apb@jhu.edu
 */
public interface WebResource extends AutoCloseable {

    /**
     * Return the MIME type of the representation of this resource.
     *
     * @return MIME type string
     */
    public String contentType();

    /**
     * URI of the resource.
     * <p>
     * May be a relative URI.
     * </p>
     *
     * @return the URI of the resource, or null if it does not have any.
     */
    public URI uri();

    /**
     * Size of the resource representation in bytes.
     *
     * @return length in bytes, may be null if unknown.
     */
    public Long length();

    /**
     * Retrieve a byte stream of the resource.
     * <p>
     * This can be called multiple times sequentially after closing the previous stream, but is not thread safe.
     * Consumers MUST close this when finished in order to prevent resource leaks.
     * </p>
     *
     * @return the resource's btyes.
     */
    public InputStream representation();

    /**
     * Create a new WebResource instance from bytes.
     * <p>
     * URI and length will be null if used this way.
     * </p>
     *
     * @param stream Resource bytes.
     * @param contentType MIME type of the underlying stream.
     * @return populated web resourceF.
     */
    public static WebResource of(final InputStream stream, final String contentType) {
        return of(stream, contentType, null, null);
    }

    /**
     * Create a new WebResource instance.
     *
     * @param stream Resource bytes.
     * @param contentType MIME type of the underlying stream.
     * @param uri URI if the resource
     * @param length size in byyes, or null if not known.
     * @return the newly created WebResource
     */
    public static WebResource of(final InputStream stream, final String contentType, final URI uri,
            final Long length) {
        return new WebResource() {

            @Override
            public void close() throws IOException {
                stream.close();
            }

            @Override
            public URI uri() {
                return uri;
            }

            @Override
            public InputStream representation() {
                return stream;
            }

            @Override
            public Long length() {
                return length;
            }

            @Override
            public String contentType() {
                return contentType;
            }
        };
    }
}
