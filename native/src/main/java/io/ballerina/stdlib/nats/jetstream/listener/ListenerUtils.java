/*
 * Copyright (c) 2022, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package io.ballerina.stdlib.nats.jetstream.listener;

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
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static io.ballerina.runtime.api.constants.RuntimeConstants.ORG_NAME_SEPARATOR;
import static io.ballerina.runtime.api.constants.RuntimeConstants.VERSION_SEPARATOR;

/**
 * Extern functions of the APIs provided by the JetStreamListener.
 */
public class ListenerUtils {
    private static final CountDownLatch countDownLatch = new CountDownLatch(1);

    public static void basicStreamStart(BObject listenerObject) {
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

    public static Object attach(Environment env, BObject listenerObject, BObject service,
                                Object annotationData) {
        Connection natsConnection =
                (Connection) listenerObject.getNativeData(Constants.NATS_CONNECTION);
        JetStream jetStream = (JetStream) listenerObject.getNativeData(Constants.JET_STREAM);
        @SuppressWarnings("unchecked")
        List<BObject> serviceList =
                (List<BObject>) listenerObject.getNativeData(Constants.SERVICE_LIST);
        BMap<BString, Object> subscriptionConfig =
                Utils.getSubscriptionConfig(((AnnotatableType) service.getType())
                        .getAnnotation(StringUtils.fromString(
                                Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                        Utils.getModule().getName() + VERSION_SEPARATOR +
                                        Utils.getModule().getVersion() + ":" +
                                        Constants.STREAM_SUBSCRIPTION_CONFIG)));
        String queueName = null;
        String subject;

        Runtime runtime = env.getRuntime();

        boolean autoAck = true;
        Dispatcher dispatcher = natsConnection.createDispatcher();

        // Add dispatcher. This is needed when closing the connection.
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Dispatcher> dispatcherList = (ConcurrentHashMap<String, Dispatcher>)
                listenerObject.getNativeData(Constants.DISPATCHER_LIST);
        dispatcherList.put(service.getType().getName(), dispatcher);
        if (subscriptionConfig != null) {
            autoAck = subscriptionConfig.getBooleanValue(Constants.AUTO_ACK);
            if (subscriptionConfig.containsKey(Constants.QUEUE_NAME)) {
                queueName = subscriptionConfig.getStringValue(Constants.QUEUE_NAME).getValue();
            }
            // If the service config is not null, get subject from the config
            subject = subscriptionConfig.getStringValue(Constants.SUBJECT).getValue();
        } else if (TypeUtils.getType(annotationData).getTag() == TypeTags.STRING_TAG) {
            // Else get the service name as the subject
            subject = ((BString) annotationData).getValue();
            service.addNativeData(Constants.SERVICE_NAME, subject);
        } else {
            throw Utils.createNatsError("Subject name cannot be found.");
        }

        JetStreamSubscription streamSubscription;
        try {
            StreamMessageHandler streamMessageHandler =
                    new StreamMessageHandler(service, runtime, natsConnection.getConnectedUrl(), autoAck);
            if (queueName != null) {
                // todo: autoAck to user, keep this in a map?
                streamSubscription = jetStream.subscribe(subject, queueName, dispatcher, streamMessageHandler, false,
                        null);
            } else {
                streamSubscription = jetStream.subscribe(subject, dispatcher, streamMessageHandler, false);
            }
        } catch (IOException | JetStreamApiException e) {
            throw Utils.createNatsError("Error occurred while creating the JetStream subscription.");
        }
        serviceList.add(service);
        @SuppressWarnings("unchecked")
        ArrayList<JetStreamSubscription> subscriptionsList =
                (ArrayList<JetStreamSubscription>) listenerObject
                        .getNativeData(Constants.BASIC_SUBSCRIPTION_LIST);
        subscriptionsList.add(streamSubscription);
        return null;
    }

    public static Object detach(BObject listener, BObject service) {
        @SuppressWarnings("unchecked")
        List<BObject> serviceList =
                (List<BObject>) listener.getNativeData(Constants.SERVICE_LIST);
        BMap<BString, Object> subscriptionConfig = Utils.getSubscriptionConfig(((AnnotatableType) service.getType())
                        .getAnnotation(StringUtils.fromString(Utils.getModule().getOrg() + ORG_NAME_SEPARATOR +
                                        Utils.getModule().getName() + VERSION_SEPARATOR +
                                        Utils.getModule().getVersion() + ":" +
                                        Constants.STREAM_SUBSCRIPTION_CONFIG)));
        String serviceName = (String) service.getNativeData(Constants.SERVICE_NAME);
        String subject;
        if (subscriptionConfig == null) {
            if (serviceName == null) {
                return Utils.createNatsError(
                        "Error occurred while un-subscribing, Cannot find subscription configuration");
            } else {
                subject = serviceName;
            }
        } else {
            subject = subscriptionConfig.getStringValue(Constants.SUBJECT).getValue();
        }
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, Dispatcher> dispatcherList = (ConcurrentHashMap<String, Dispatcher>)
                listener.getNativeData(Constants.DISPATCHER_LIST);
        Dispatcher dispatcher = dispatcherList.get(service.getType().getName());
        try {
            dispatcher.unsubscribe(subject);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Utils.createNatsError("Error occurred while un-subscribing " + ex.getMessage());
        }
        serviceList.remove(service);
        dispatcherList.remove(service.getType().getName());
        return null;
    }

    public static Object immediateStop(BObject listenerObject) {
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

        // Actual NATS connection is not used in any other clients. So we can close the actual connection.
        try {
            natsConnection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Utils.createNatsError("Listener interrupted while closing NATS connection");
        }
        return null;
    }

    public static Object gracefulStop(BObject listenerObject) {
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

        try {
            // Wait for the drain to succeed, passed 0 to wait forever.
            natsConnection.drain(Duration.ZERO);
        } catch (InterruptedException | TimeoutException | IllegalStateException e) {
            Thread.currentThread().interrupt();
            return Utils.createNatsError("Error occurred while stopping the listener.");
        }
        return null;
    }
}
