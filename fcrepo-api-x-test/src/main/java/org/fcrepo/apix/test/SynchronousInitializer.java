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

package org.fcrepo.apix.test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fcrepo.apix.model.components.Initializer;

/**
 * Simple synchronous initializer for tests.
 *
 * @author apb@jhu.edu
 */
public class SynchronousInitializer implements Initializer {

    /**
     * {@inheritDoc}
     */
    @Override
    public Initialization initialize(final Runnable task) {
        return new Initialization() {

            Exception error = null;

            boolean initialized;
            {
                try {
                    task.run();
                    initialized = true;
                } catch (final Exception e) {
                    error = e;
                }
            }

            @Override
            public void verify() {
                if (!initialized) {
                    throw new IllegalStateException("Not initialized");
                }

                if (error != null) {
                    throw new RuntimeException(error);
                }
            }

            @Override
            public void cancel() {
                initialized = false;
            }

            @Override
            public void await(final long time, final TimeUnit unit) throws TimeoutException {
                verify();
            }

            @Override
            public void await() {
                verify();
            }
        };
    }

}
