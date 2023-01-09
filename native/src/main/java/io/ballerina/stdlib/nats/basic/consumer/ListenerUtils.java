/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.connection.ConnectionUtils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static io.ballerina.stdlib.nats.Constants.CONSTRAINT_VALIDATION;

/**
 * Contains listener stop functions.
 */
public class ListenerUtils {
    public static Object immediateStop(BObject listenerObject) {
        Connection natsConnection =
                (Connection) listenerObject.getNativeData(Constants.NATS_CONNECTION);
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) listenerObject.getNativeData(Constants.NATS_METRIC_UTIL);
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

        // Actual NATS connection is not used in any other clients. So we can close the actual connection.
        try {
            natsConnection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Listener interrupted while closing NATS connection", e);
        }
        return null;
    }


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
            return Utils.createNatsError("Listener interrupted on graceful stop.", e);
        } catch (TimeoutException e) {
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Timeout error occurred, on graceful stop.", e);
        } catch (IllegalStateException e) {
            natsMetricsReporter.reportConsumerError(NatsObservabilityConstants.UNKNOWN,
                    NatsObservabilityConstants.ERROR_TYPE_CLOSE);
            return Utils.createNatsError("Connection is already closed.", e);
        }
        return null;
    }

    public static Object consumerInit(BObject listenerObject, Object url, BMap connectionConfig) {
        Connection natsConnection;
        try {
            natsConnection = ConnectionUtils.getNatsConnection(url, connectionConfig);
        } catch (Exception e) {
            String errorMsg = "Error occurred while setting up the connection.";
            return Utils.createNatsError(errorMsg, e);
        }
        listenerObject.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(natsConnection));
        listenerObject.addNativeData(Constants.NATS_CONNECTION, natsConnection);
        listenerObject.addNativeData(CONSTRAINT_VALIDATION,
                connectionConfig.getBooleanValue(StringUtils.fromString(CONSTRAINT_VALIDATION)));
        ((NatsMetricsReporter) listenerObject.getNativeData(Constants.NATS_METRIC_UTIL)).reportNewClient();

        // Initialize dispatcher list to use in service register and listener close.
        ConcurrentHashMap<String, Dispatcher> dispatcherList = new ConcurrentHashMap<>();
        listenerObject.addNativeData(Constants.DISPATCHER_LIST, dispatcherList);
        ArrayList<String> subscriptionsList = new ArrayList<>();
        listenerObject.addNativeData(Constants.BASIC_SUBSCRIPTION_LIST, subscriptionsList);
        List<BObject> serviceList = Collections.synchronizedList(new ArrayList<>());
        listenerObject.addNativeData(Constants.SERVICE_LIST, serviceList);
        return null;
    }
}
