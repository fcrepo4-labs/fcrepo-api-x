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

import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.fcrepo.apix.model.components.Initializer.Initialization;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifies functionality of Initializer impl.
 *
 * @author apb@jhu.edu
 */
public class InitMgrTest {

    InitMgr toTest = new InitMgr();

    static ExecutorService exe = Executors.newCachedThreadPool();

    @Before
    public void setUp() {
        toTest.setRetryWait(10);
    }

    // No exceptions or timeouts should be seen for a quick and easy initialization.
    @Test
    public void successfulInitTest() throws Exception {
        final Initialization init = toTest.initialize(() -> {
            // Immediate success
        });

        attempt(() -> {
            init.await();
        });

        init.verify();

        init.cancel();
    }

    // Make sure await waits, verify throws exception if init is unsuccessful;
    @Test
    public void unsuccessfulInitTest() throws Exception {
        final CountDownLatch working = new CountDownLatch(1);

        final Initialization init = toTest.initialize(() -> {
            working.countDown();
            throw new RuntimeException();
        });

        /* Make sure we're sure the initialization thread is running */
        working.await(1, TimeUnit.SECONDS);

        try {
            attempt(() -> {
                init.await();
            }, 10, TimeUnit.MILLISECONDS);
            fail("Should have timed out!");
        } catch (final TimeoutException e) {
            // Expected
        }

        try {
            init.verify();
            fail("Should have failed!");
        } catch (final Exception e) {
            // Expected
        }

        // Should not throw an exception
        init.cancel();
    }

    // Make sure cancel cancels
    @Test
    public void cancelTest() throws Exception {
        final CountDownLatch working = new CountDownLatch(1);

        final Initialization init = toTest.initialize(() -> {
            working.countDown();
            throw new RuntimeException();
        });

        /* Make sure we're sure the initialization thread is running */
        working.await(1, TimeUnit.SECONDS);

        init.cancel();

        try {
            attempt(() -> {
                init.await();
            }, 10, TimeUnit.MILLISECONDS);
            fail("Should not have succeeded!");
        } catch (final TimeoutException e) {
            fail("Should not have failed with timeout exception");
        } catch (final Exception e) {
        }

        try {
            init.verify();
            fail("Should have failed!");
        } catch (final Exception e) {
            // Expected
        }
    }

    // Make sure await timeout works
    @Test(expected = TimeoutException.class)
    public void awaitTimeoutTest() throws Exception {

        final CountDownLatch working = new CountDownLatch(1);

        final Initialization init = toTest.initialize(() -> {
            working.countDown();
            throw new RuntimeException();
        });

        /* Make sure we're sure the initialization thread is running */
        working.await(1, TimeUnit.SECONDS);

        init.await(10, TimeUnit.MILLISECONDS);

    }

    static void attempt(final Runnable task) throws Exception {
        attempt(task, 1, TimeUnit.SECONDS);
    }

    static void attempt(final Runnable task, final long time, final TimeUnit unit) throws Exception {
        exe.submit(task).get(time, unit);
    }
}
