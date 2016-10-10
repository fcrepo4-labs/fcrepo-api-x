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

package org.fcrepo.apix.routing;

import java.net.URI;
import java.util.Arrays;

/**
 * URI parsiing and manipulation utilities.
 *
 * @author apb@jhu.edu
 */
public abstract class Util {

    /**
     * Normalize a URI path segment to remove forward and trailing slashes, if present.
     *
     * @param path path segment
     * @return Normalized segment
     */
    public static String segment(final String path) {
        return path.replaceFirst("^/", "").replaceFirst("/$", "");
    }

    /**
     * Normalize a terminall URI path segment by removing forward slashes, if present.
     *
     * @param path path segment
     * @return Normalized segmen
     */
    public static String terminal(final String path) {
        return path.equals("") ? "/" : path.replaceFirst("^/", "");
    }

    /**
     * Append URI path segments together.
     *
     * @param segments URI path segment
     * @return URI composed of appended segments.
     */
    public static URI append(final Object... segments) {
        return URI.create(
                Arrays.stream(segments)
                        .reduce((a, b) -> String.join("/", segment(a.toString()), terminal(b.toString())))
                        .get().toString());
    }
}
