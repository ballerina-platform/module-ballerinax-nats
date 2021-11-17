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
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
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
    private BObject serviceObject;
    private String connectedUrl;
    private Runtime runtime;
    private NatsMetricsReporter natsMetricsReporter;
    private Connection natsConnection;

    DefaultMessageHandler(BObject serviceObject, Runtime runtime, Connection natsConnection,
                          NatsMetricsReporter natsMetricsReporter) {
        this.serviceObject = serviceObject;
        this.runtime = runtime;
        this.connectedUrl = natsConnection.getConnectedUrl();
        this.natsMetricsReporter = natsMetricsReporter;
        this.natsConnection = natsConnection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message) {
        natsMetricsReporter.reportConsume(message.getSubject(), message.getData().length);
        BArray msgData = ValueCreator.createArrayValue(message.getData());
        BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Utils.getModule(),
                                                                      Constants.NATS_MESSAGE_OBJ_NAME);
        BMap<BString, Object> populatedRecord = ValueCreator.createRecordValue(msgRecord, msgData,
                                                                         StringUtils.fromString(message.getSubject()),
                                                                         StringUtils.fromString(message.getReplyTo()));
        String replyTo = message.getReplyTo();
        String subject = message.getSubject();
        try {
            if (replyTo != null && Utils.getAttachedFunctionType(serviceObject,
                    Constants.ON_REQUEST_RESOURCE) != null) {
                // If replyTo subject is there and the user has written the onRequest function implementation:
                dispatchOnRequest(populatedRecord, replyTo);
            } else {
                // Default onMessage behavior
                dispatchOnMessage(populatedRecord);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     *
     * @param msgObj Message object
     */
    private void dispatchOnRequest(BMap<BString, Object>  msgObj, String replyTo) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnRequestResource(msgObj, countDownLatch, replyTo);
        countDownLatch.await();
    }

    /**
     * Dispatch only the message to the onMessage resource.
     *
     * @param msgObj Message object
     */
    private void dispatchOnMessage(BMap<BString, Object>  msgObj) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnMessageResource(msgObj, countDownLatch);
        countDownLatch.await();
    }

    private void executeOnRequestResource(BMap<BString, Object>  msgObj, CountDownLatch countDownLatch,
                                          String replyTo) {
        String subject = msgObj.getStringValue(Constants.SUBJECT).getValue();
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), Constants.ON_REQUEST_RESOURCE);
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(Constants.ON_REQUEST_RESOURCE)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, Constants.ON_REQUEST_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                this.natsConnection), properties,
                        PredefinedTypes.TYPE_ANYDATA, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, Constants.ON_REQUEST_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                this.natsConnection), properties,
                        PredefinedTypes.TYPE_ANYDATA, msgObj, true);
            }
        } else {
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(Constants.ON_REQUEST_RESOURCE)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, Constants.ON_REQUEST_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                this.natsConnection), null, PredefinedTypes.TYPE_ANYDATA, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, Constants.ON_REQUEST_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                this.natsConnection), null, PredefinedTypes.TYPE_ANYDATA, msgObj, true);
            }
        }
    }

    private void executeOnMessageResource(BMap<BString, Object>  msgObj, CountDownLatch countDownLatch) {
        String subject = msgObj.getStringValue(Constants.SUBJECT).getValue();
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), Constants.ON_MESSAGE_RESOURCE);
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(Constants.ON_MESSAGE_RESOURCE)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, Constants.ON_MESSAGE_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter),
                        properties, PredefinedTypes.TYPE_NULL, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, Constants.ON_MESSAGE_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter),
                        properties, PredefinedTypes.TYPE_NULL, msgObj, true);
            }
        } else {
            if (serviceObject.getType().isIsolated() &&
                    serviceObject.getType().isIsolated(Constants.ON_MESSAGE_RESOURCE)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, Constants.ON_MESSAGE_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter), null,
                        PredefinedTypes.TYPE_NULL, msgObj, true);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, Constants.ON_MESSAGE_RESOURCE, null, metadata,
                        new ResponseCallback(countDownLatch, subject, natsMetricsReporter), null,
                        PredefinedTypes.TYPE_NULL, msgObj, true);
            }
        }
    }

    /**
     * Represents the callback which will be triggered upon submitting to resource.
     */
    public static class ResponseCallback implements Callback {
        private CountDownLatch countDownLatch;
        private String subject;
        private NatsMetricsReporter natsMetricsReporter;
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
            if (replyTo != null) {
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
