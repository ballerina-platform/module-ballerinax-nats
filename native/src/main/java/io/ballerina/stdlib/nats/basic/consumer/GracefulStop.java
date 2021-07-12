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
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Extern function to gracefully stop the NATS subscriber.
 *
 * @since 1.0.0
 */
public class GracefulStop {

    public static Object gracefulStop(BObject listenerObject) {
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) listenerObject.getNativeData(Constants.NATS_METRIC_UTIL);
        Connection natsConnection =
                (Connection) listenerObject.getNativeData(Constants.NATS_CONNECTION);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Dispatcher> dispatcherList = (ConcurrentHashMap<String, Dispatcher>)
                listenerObject.getNativeData(Constants.DISPATCHER_LIST);
        Iterator dispatchers = dispatcherList.entrySet().iterator();
        while (dispatchers.hasNext()) {
            Map.Entry pair = (Map.Entry) dispatchers.next();
            natsConnection.closeDispatcher((Dispatcher) pair.getValue());
            dispatchers.remove(); // avoids a ConcurrentModificationException
        }
        @SuppressWarnings("unchecked")
        ArrayList<String> subscriptionsList =
                (ArrayList<String>) listenerObject
                        .getNativeData(Constants.BASIC_SUBSCRIPTION_LIST);
        natsMetricsReporter.reportBulkUnsubscription(subscriptionsList);

        try {
            // Wait for the drain to succeed, passed 0 to wait forever.
            natsConnection.drain(Duration.ZERO);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                                                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Listener interrupted on graceful stop.");
        } catch (TimeoutException e) {
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                                                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Timeout error occurred, on graceful stop.");
        } catch (IllegalStateException e) {
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                                                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Connection is already closed.");
        }
        return null;
    }

}
