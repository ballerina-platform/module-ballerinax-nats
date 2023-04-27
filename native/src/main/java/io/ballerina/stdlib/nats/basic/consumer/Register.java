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

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;

/**
 * Creates a subscription with the NATS server.
 *
 * @since 0.995
 */
public class Register {
    private static final PrintStream console;

    public static Object attach(Environment env, BObject listenerObject, BObject service,
                                       Object annotationData) {
        Connection natsConnection =
                (Connection) listenerObject.getNativeData(Constants.NATS_CONNECTION);
        @SuppressWarnings("unchecked")
        List<BObject> serviceList =
                (List<BObject>) listenerObject.getNativeData(Constants.SERVICE_LIST);
        BMap<BString, Object> subscriptionConfig =
                Utils.getSubscriptionConfig(((AnnotatableType) TypeUtils.getType(service))
                        .getAnnotation(StringUtils.fromString(
                                Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                        Utils.getModule().getName() + VERSION_SEPARATOR +
                                        Utils.getModule().getVersion() + ":" +
                                        Constants.SUBSCRIPTION_CONFIG)));
        String queueName = null;
        String subject;

        Runtime runtime = env.getRuntime();
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) listenerObject.getNativeData(Constants.NATS_METRIC_UTIL);
        Dispatcher dispatcher = natsConnection.createDispatcher(new DefaultMessageHandler(
                service, runtime, natsConnection, natsMetricsReporter, listenerObject));

        // Add dispatcher. This is needed when closing the connection.
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Dispatcher> dispatcherList = (ConcurrentHashMap<String, Dispatcher>)
                listenerObject.getNativeData(Constants.DISPATCHER_LIST);
        dispatcherList.put(TypeUtils.getType(service).getName(), dispatcher);
        if (subscriptionConfig != null) {
            if (subscriptionConfig.containsKey(Constants.QUEUE_NAME)) {
                queueName = subscriptionConfig.getStringValue(Constants.QUEUE_NAME).getValue();
            }
            // If the service config is not null, get subject from the config
            subject = subscriptionConfig.getStringValue(Constants.SUBJECT).getValue();
            if (subscriptionConfig.getMapValue(Constants.PENDING_LIMITS) != null) {
                setPendingLimits(dispatcher, subscriptionConfig.getMapValue(Constants.PENDING_LIMITS));
            }
        } else if (TypeUtils.getType(annotationData).getTag() == TypeTags.STRING_TAG) {
            // Else get the service name as the subject
            subject = ((BString) annotationData).getValue();
            service.addNativeData(Constants.SERVICE_NAME, subject);
        } else {
            throw Utils.createNatsError("Subject name cannot be found");
        }

        if (queueName != null) {
            dispatcher.subscribe(subject, queueName);
        } else {
            dispatcher.subscribe(subject);
        }
        serviceList.add(service);
        @SuppressWarnings("unchecked")
        ArrayList<String> subscriptionsList =
                (ArrayList<String>) listenerObject
                        .getNativeData(Constants.BASIC_SUBSCRIPTION_LIST);
        subscriptionsList.add(subject);
        NatsMetricsReporter.reportSubscription(natsConnection.getConnectedUrl(), subject);
        return null;
    }

    // Set limits on the maximum number of messages, or maximum size of messages this consumer will
    // hold before it starts to drop new messages waiting for the resource functions to drain the queue.
    private static void setPendingLimits(Dispatcher dispatcher, BMap pendingLimits) {
        long maxMessages = pendingLimits.getIntValue(Constants.MAX_MESSAGES);
        long maxBytes = pendingLimits.getIntValue(Constants.MAX_BYTES);
        dispatcher.setPendingLimits(maxMessages, maxBytes);
    }

    static {
        console = System.out;
    }
}
