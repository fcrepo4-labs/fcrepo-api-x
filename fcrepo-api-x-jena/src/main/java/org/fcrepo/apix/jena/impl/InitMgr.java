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

package org.fcrepo.apix.jena.impl;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fcrepo.apix.model.components.Initializer;

/**
 * Simple threaded initialization initializer.
 * <p>
 * Executes each initialization task in its own thread, unbounded.
 * </p>
 * TODO: Find a more appropriate home for this, it's not specifically Jena-related.
 *
 * @author apb@jhu.edu
 */
public class InitMgr implements Initializer {

    long retryWait = 1000;

    /**
     * Set delay between retries.
     *
     * @param millis Retry in ms.
     */
    public void setRetryWait(final long millis) {
        this.retryWait = millis;
    }

    final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public Initialization initialize(final Runnable task) {
        return new Initialization() {

            long id = 0;

            final Future<?> initProc = executor.submit(() -> {
                boolean success = false;
                id = Thread.currentThread().getId();
                while (!success) {
                    try {
                        task.run();
                        success = true;
                    } catch (final Exception e) {
                        try {
                            Thread.sleep(retryWait);
                        } catch (final InterruptedException i) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Initialization interrupted", i);
                        }
                    }
                }
            });

            @Override
            public void verify() {
                if (notInitThread()) {
                    try {
                        initProc.get(0, TimeUnit.SECONDS);
                    } catch (final TimeoutException t) {
                        throw new IllegalStateException("Not initialized");
                    } catch (final ExecutionException e) {
                        throw new IllegalStateException("Not initialized", e);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Initialization terminated", e);
                    }
                }
            }

            @Override
            public void await() {
                if (notInitThread()) {
                    try {
                        initProc.get();
                    } catch (final ExecutionException e) {
                        throw new RuntimeException("Operation terminated", e);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation terminated", e);
                    }
                }
            }

            @Override
            public void await(final long time, final TimeUnit unit) throws TimeoutException {
                if (notInitThread()) {
                    try {
                        initProc.get(time, unit);
                    } catch (final ExecutionException e) {
                        throw new RuntimeException("Operation terminated", e);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Operation terminated", e);
                    }
                }
            }

            @Override
            public void cancel() {
                initProc.cancel(true);
            }

            boolean notInitThread() {
                return Thread.currentThread().getId() != id;
            }
        };
    }

}
