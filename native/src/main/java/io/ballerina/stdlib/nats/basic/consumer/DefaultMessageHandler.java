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

import io.ballerina.runtime.api.PredefinedTypes;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.ballerina.stdlib.nats.observability.NatsObserverContext;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Handles incoming message for a given subscription.
 *
 * @since 1.0.0
 */
public class DefaultMessageHandler implements MessageHandler {

    // Resource which the message should be dispatched.
    private final BObject serviceObject;
    private final String connectedUrl;
    private final Runtime runtime;
    private final NatsMetricsReporter natsMetricsReporter;
    private final Connection natsConnection;
    private static final PrintStream console;

    DefaultMessageHandler(BObject serviceObject, Runtime runtime, Connection natsConnection,
                          NatsMetricsReporter natsMetricsReporter) {
        this.serviceObject = serviceObject;
        this.runtime = runtime;
        this.connectedUrl = natsConnection.getConnectedUrl();
        this.natsMetricsReporter = natsMetricsReporter;
        this.natsConnection = natsConnection;
    }

    static {
        console = System.out;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message) {
        natsMetricsReporter.reportConsume(message.getSubject(), message.getData().length);
        String replyTo = message.getReplyTo();
        String subject = message.getSubject();
        String response = new String(message.getData(), StandardCharsets.UTF_8);
        console.println("Message received java: " + response);
        try {
            if (replyTo != null && getAttachedFunctionType(serviceObject,
                    Constants.ON_REQUEST_RESOURCE) != null) {
                // If replyTo subject is there and the user has written the onRequest function implementation:
                dispatchOnRequest(subject, replyTo, message.getData());
            } else {
                // Default onMessage behavior
                dispatchOnMessage(subject, replyTo, message.getData());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     */
    private void dispatchOnRequest(String subject, String replyTo, byte[] data) throws InterruptedException {
        MethodType methodType = getAttachedFunctionType(this.serviceObject, Constants.ON_REQUEST_RESOURCE);
        Parameter[] parameters = methodType.getParameters();
        int messageType = parameters[0].type.getTag();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnRequestResource(countDownLatch, messageType, subject, replyTo, data);
        countDownLatch.await();
    }

    /**
     * Dispatch only the message to the onMessage resource.
     */
    private void dispatchOnMessage(String subject, String replyTo, byte[] data) throws InterruptedException {
        MethodType methodType = getAttachedFunctionType(this.serviceObject, Constants.ON_MESSAGE_RESOURCE);
        Parameter[] parameters = methodType.getParameters();
        int messageType = parameters[0].type.getTag();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnMessageResource(countDownLatch, messageType, subject, replyTo, data);
        countDownLatch.await();
    }

    public static MethodType getAttachedFunctionType(BObject serviceObject, String functionName) {
        MethodType function = null;
        MethodType[] remoteFunctions = serviceObject.getType().getMethods();
        for (MethodType remoteFunction : remoteFunctions) {
            if (functionName.equals(remoteFunction.getName())) {
                function = remoteFunction;
                break;
            }
        }
        return function;
    }

    private void executeOnRequestResource(CountDownLatch countDownLatch, int tag,
                                          String subject, String replyTo, byte[] data) {
        BMap<BString, Object> msgObj = null;
        BArray msgData = ValueCreator.createArrayValue(data);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.MESSAGE_CONTENT, msgData);
        valueMap.put(Constants.MESSAGE_SUBJECT, StringUtils.fromString(subject));
        valueMap.put(Constants.MESSAGE_REPLY_TO, StringUtils.fromString(replyTo));

        if (tag == TypeTags.INTERSECTION_TAG) {
            msgObj = ValueCreator.createReadonlyRecordValue(Utils.getModule(),
                        Constants.NATS_MESSAGE_OBJ_NAME, valueMap);
        } else {
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Utils.getModule(),
                    Constants.NATS_MESSAGE_OBJ_NAME);
            msgObj = ValueCreator.createRecordValue(msgRecord, msgData, StringUtils.fromString(subject),
                    StringUtils.fromString(replyTo));
        }
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), Constants.ON_REQUEST_RESOURCE);
        executeResource(msgObj, Constants.ON_REQUEST_RESOURCE, new ResponseCallback(countDownLatch, subject,
                natsMetricsReporter, replyTo, this.natsConnection), metadata, PredefinedTypes.TYPE_ANYDATA);
    }

    private void executeOnMessageResource(CountDownLatch countDownLatch, int tag,
                                          String subject, String replyTo, byte[] data) {
        BMap<BString, Object> msgObj;
        BArray msgData = ValueCreator.createArrayValue(data);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.MESSAGE_CONTENT, msgData);
        valueMap.put(Constants.MESSAGE_SUBJECT, StringUtils.fromString(subject));
        if (replyTo != null) {
            valueMap.put(Constants.MESSAGE_REPLY_TO, StringUtils.fromString(replyTo));
        }

        if (tag == TypeTags.INTERSECTION_TAG) {
            msgObj = ValueCreator.createReadonlyRecordValue(Utils.getModule(),
                    Constants.NATS_MESSAGE_OBJ_NAME, valueMap);
        } else {
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Utils.getModule(),
                    Constants.NATS_MESSAGE_OBJ_NAME);
            msgObj = ValueCreator.createRecordValue(msgRecord, msgData, StringUtils.fromString(subject),
                    StringUtils.fromString(replyTo));
        }
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), Constants.ON_MESSAGE_RESOURCE);
        executeResource(msgObj, Constants.ON_MESSAGE_RESOURCE, new ResponseCallback(countDownLatch, subject,
                        natsMetricsReporter), metadata, PredefinedTypes.TYPE_NULL);
    }

    private void executeResource(BMap<BString, Object>  msgObj, String function, Callback callback,
                                 StrandMetadata metadata, Type returnType) {
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(function)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, function, null, metadata,
                        callback, properties, returnType, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, function, null, metadata,
                        callback, properties, returnType, msgObj, true);
            }
        } else {
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(function)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, function, null, metadata,
                        callback, null, returnType, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, function, null, metadata,
                        callback, null, returnType, msgObj, true);
            }
        }
    }

    /**
     * Represents the callback which will be triggered upon submitting to resource.
     */
    public static class ResponseCallback implements Callback {
        private final CountDownLatch countDownLatch;
        private final String subject;
        private final NatsMetricsReporter natsMetricsReporter;
        private String replyTo;
        private Connection natsConnection;

        ResponseCallback(CountDownLatch countDownLatch, String subject, NatsMetricsReporter natsMetricsReporter) {
            this.countDownLatch = countDownLatch;
            this.subject = subject;
            this.natsMetricsReporter = natsMetricsReporter;
        }

        ResponseCallback(CountDownLatch countDownLatch, String subject, NatsMetricsReporter natsMetricsReporter,
                         String replyTo, Connection natsConnection) {
            this.countDownLatch = countDownLatch;
            this.subject = subject;
            this.natsMetricsReporter = natsMetricsReporter;
            this.replyTo = replyTo;
            this.natsConnection = natsConnection;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifySuccess(Object obj) {
            if (obj instanceof BError) {
                ((BError) obj).printStackTrace();
            } else if (replyTo != null) {
                natsConnection.publish(replyTo, Utils.convertDataIntoByteArray(obj));
            }
            natsMetricsReporter.reportDelivery(subject);
            countDownLatch.countDown();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifyFailure(io.ballerina.runtime.api.values.BError error) {
            error.printStackTrace();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            countDownLatch.countDown();
        }
    }
}
