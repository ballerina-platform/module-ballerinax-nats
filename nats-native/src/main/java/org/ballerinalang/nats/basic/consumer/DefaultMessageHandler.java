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

package org.ballerinalang.nats.basic.consumer;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsObserverContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.nats.Constants.ON_MESSAGE_RESOURCE;
import static org.ballerinalang.nats.Constants.ON_REQUEST_RESOURCE;
import static org.ballerinalang.nats.Utils.convertDataIntoByteArray;
import static org.ballerinalang.nats.Utils.getAttachedFunctionType;

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
        if (replyTo != null && getAttachedFunctionType(serviceObject, ON_REQUEST_RESOURCE) != null) {
            // If replyTo subject is there and the user has written the onRequest function implementation:
            MethodType onRequest = getAttachedFunctionType(serviceObject, ON_REQUEST_RESOURCE);
            Type[] parameterTypes = onRequest.getParameterTypes();
            if (parameterTypes.length == 1) {
                dispatchOnRequest(populatedRecord, replyTo);
            } else {
                throw Utils.createNatsError("invalid onRequest remote function signature");
            }
        } else {
            // Default onMessage behavior
            MethodType onMessage = getAttachedFunctionType(serviceObject, ON_MESSAGE_RESOURCE);
            Type[] parameterTypes = onMessage.getParameterTypes();
            if (parameterTypes.length == 1) {
                dispatchOnMessage(populatedRecord);
            } else {
                throw Utils.createNatsError("invalid onMessage remote function signature");
            }
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     *
     * @param msgObj Message object
     */
    private void dispatchOnRequest(BMap<BString, Object>  msgObj, String replyTo) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnRequestResource(msgObj, countDownLatch, replyTo);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(msgObj.getStringValue(Constants.SUBJECT).getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     *
     * @param msgObj Message object
     */
    private void dispatchOnMessage(BMap<BString, Object>  msgObj) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeOnMessageResource(msgObj, countDownLatch);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(msgObj.getStringValue(Constants.SUBJECT).getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
        }
    }

    private void executeOnRequestResource(BMap<BString, Object>  msgObj, CountDownLatch countDownLatch,
                                          String replyTo) {
        String subject = msgObj.getStringValue(Constants.SUBJECT).getValue();
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), ON_REQUEST_RESOURCE);
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(serviceObject, ON_REQUEST_RESOURCE, null, metadata,
                                      new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                                           this.natsConnection), properties, msgObj, true);
        } else {
            runtime.invokeMethodAsync(serviceObject, ON_REQUEST_RESOURCE, null, metadata,
                                      new ResponseCallback(countDownLatch, subject, natsMetricsReporter, replyTo,
                                                           this.natsConnection), msgObj, true);
        }
    }

    private void executeOnMessageResource(BMap<BString, Object>  msgObj, CountDownLatch countDownLatch) {
        String subject = msgObj.getStringValue(Constants.SUBJECT).getValue();
        StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(), Utils.getModule().getName(),
                                                     Utils.getModule().getVersion(), ON_MESSAGE_RESOURCE);
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(serviceObject, ON_MESSAGE_RESOURCE, null, metadata,
                                      new ResponseCallback(countDownLatch, subject, natsMetricsReporter),
                                      properties, msgObj, true);
        } else {
            runtime.invokeMethodAsync(serviceObject, ON_MESSAGE_RESOURCE, null, metadata,
                                      new ResponseCallback(countDownLatch, subject, natsMetricsReporter), msgObj, true);
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
                natsConnection.publish(replyTo, convertDataIntoByteArray(obj));
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
