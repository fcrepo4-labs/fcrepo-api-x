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

package org.fcrepo.apix.model.components;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Runs initialization tasks and provides barriers to control access while the task runs.
 *
 * @author apb@jhu.edu
 */
public interface Initializer {

    /**
     * Run an initialization task.
     * <p>
     * May run initialization task indefinitely or repeatedly until it succeeds, or is cancelled.
     * </p>
     *
     * @param task The task to run
     * @return initialization state
     */
    Initialization initialize(final Runnable task);

    /**
     * Represents initialization state.
     * <p>
     * Provides barriers that may be used to verify orr wait for initialization;
     * </p>
     *
     * @author apb@jhu.edu
     */
    public interface Initialization {

        /** No initialization. */
        static final Initialization NONE = new Initialization() {

            @Override
            public void await() {
                // Do nothing
            }

            @Override
            public void verify() {
                // Do nothing
            }

            @Override
            public void cancel() {
                // Do nothing
            }

            @Override
            public void await(final long time, final TimeUnit unit) throws TimeoutException {
                // Do nothing
            }

        };

        /**
         * Await for initialization to succeed, blocking as necessary.
         * <p>
         * This may be used as an initialization barrier, preventing other threads from progressing until
         * initialization is finished. Called from within the initialization routine, this will not block.
         * </p>
         *
         * @throws RuntimeException if initialization fails.
         */
        void await();

        /**
         * Await for the initialization to succeed, blocking, for the specified time limit *
         * <p>
         * This may be used as an initialization barrier, preventing other threads from progressing until
         * initialization is finished. Called from within the initialization routine, this will not block.
         * </p>
         *
         * @param time Time to wait.
         * @param unit time units.
         * @throws TimeoutException Thrown when time limit exceeded
         */
        public void await(final long time, final TimeUnit unit) throws TimeoutException;

        /**
         * Throw an exception if not initialized.
         * <p>
         * If called within the initialization routine, this will not throw an exception.
         * </p>
         *
         * @throws RuntimeException id not initialized.
         */
        void verify();

        /**
         * Cancel an initialization, if it's still running.
         */
        void cancel();
    }
}
