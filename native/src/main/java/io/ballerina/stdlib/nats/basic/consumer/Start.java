/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.nats.basic.consumer;

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.nats.Constants;

import java.util.concurrent.CountDownLatch;

/**
 * Extern function to start the NATS subscriber.
 *
 * @since 0.995
 */
public class Start {
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void basicStart(BObject listenerObject) {
        listenerObject.addNativeData(Constants.COUNTDOWN_LATCH, countDownLatch);
        // It is essential to keep a non-daemon thread running in order to avoid the java program or the
        // Ballerina service from exiting
        new Thread(() -> {
            try {
                countDownLatch.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
