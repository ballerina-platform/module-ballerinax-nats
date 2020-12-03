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
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MemberFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
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

import static org.ballerinalang.nats.Constants.ON_MESSAGE_METADATA;
import static org.ballerinalang.nats.Constants.ON_MESSAGE_RESOURCE;
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

    DefaultMessageHandler(BObject serviceObject, Runtime runtime, String connectedUrl,
                          NatsMetricsReporter natsMetricsReporter) {
        this.serviceObject = serviceObject;
        this.runtime = runtime;
        this.connectedUrl = connectedUrl;
        this.natsMetricsReporter = natsMetricsReporter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message) {
        natsMetricsReporter.reportConsume(message.getSubject(), message.getData().length);
        BArray msgData = ValueCreator.createArrayValue(message.getData());
        BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Constants.NATS_PACKAGE_ID,
                                                                      Constants.NATS_MESSAGE_OBJ_NAME);
        BMap<BString, Object> populatedRecord = ValueCreator.createRecordValue(msgRecord, msgData,
                                                                         StringUtils.fromString(message.getSubject()),
                                                                         StringUtils.fromString(message.getReplyTo()));
        MemberFunctionType onMessage = getAttachedFunctionType(serviceObject, ON_MESSAGE_RESOURCE);
        Type[] parameterTypes = onMessage.getParameterTypes();
        if (parameterTypes.length == 1) {
            dispatch(populatedRecord);
        } else {
            throw Utils.createNatsError("Invalid remote function signature");
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     *
     * @param msgObj Message object
     */
    private void dispatch(BMap<BString, Object>  msgObj) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        executeResource(msgObj, countDownLatch);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            natsMetricsReporter.reportConsumerError(msgObj.getStringValue(Constants.SUBJECT).getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
        }
    }

    private void executeResource(BMap<BString, Object>  msgObj, CountDownLatch countDownLatch) {
        String subject = msgObj.getStringValue(Constants.SUBJECT).getValue();
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl,
                    msgObj.getStringValue(Constants.SUBJECT).getValue());
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(serviceObject, ON_MESSAGE_RESOURCE, null, ON_MESSAGE_METADATA,
                                      new ResponseCallback(countDownLatch, subject, natsMetricsReporter), properties,
                                      msgObj, true);
        } else {
            runtime.invokeMethodAsync(serviceObject, ON_MESSAGE_RESOURCE, null, ON_MESSAGE_METADATA,
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

        ResponseCallback(CountDownLatch countDownLatch, String subject, NatsMetricsReporter natsMetricsReporter) {
            this.countDownLatch = countDownLatch;
            this.subject = subject;
            this.natsMetricsReporter = natsMetricsReporter;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifySuccess(Object obj) {
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
