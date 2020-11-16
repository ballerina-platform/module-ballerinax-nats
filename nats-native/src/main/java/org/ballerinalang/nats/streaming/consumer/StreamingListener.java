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
package org.ballerinalang.nats.streaming.consumer;

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.nats.streaming.Message;
import io.nats.streaming.MessageHandler;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsObserverContext;

import java.util.HashMap;
import java.util.Map;

import static org.ballerinalang.nats.Constants.NATS_STREAMING_MESSAGE_OBJ_NAME;
import static org.ballerinalang.nats.Constants.ON_ERROR_METADATA;
import static org.ballerinalang.nats.Constants.ON_ERROR_RESOURCE;
import static org.ballerinalang.nats.Constants.ON_MESSAGE_METADATA;
import static org.ballerinalang.nats.Constants.ON_MESSAGE_RESOURCE;
import static org.ballerinalang.nats.Utils.getAttachedFunctionType;

/**
 * {@link MessageHandler} implementation to listen to Messages of the subscribed subject from NATS streaming server.
 */
public class StreamingListener implements MessageHandler {
    private BObject service;
    private Runtime runtime;
    private String connectedUrl;
    private boolean manualAck;
    private NatsMetricsReporter natsMetricsReporter;

    public StreamingListener(BObject service, boolean manualAck, Runtime runtime,
                             String connectedUrl, NatsMetricsReporter natsMetricsReporter) {
        this.service = service;
        this.runtime = runtime;
        this.manualAck = manualAck;
        this.natsMetricsReporter = natsMetricsReporter;
        this.connectedUrl = connectedUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message msg) {
        natsMetricsReporter.reportConsume(msg.getSubject(), msg.getData().length);
        BObject ballerinaNatsMessage = ValueCreator.createObjectValue(
                Constants.NATS_PACKAGE_ID, NATS_STREAMING_MESSAGE_OBJ_NAME, StringUtils.fromString(msg.getSubject()),
                ValueCreator.createArrayValue(msg.getData()), StringUtils.fromString(msg.getReplyTo()));
        ballerinaNatsMessage.addNativeData(Constants.NATS_STREAMING_MSG, msg);
        ballerinaNatsMessage.addNativeData(Constants.NATS_STREAMING_MANUAL_ACK.getValue(), manualAck);
        AttachedFunctionType onMessageResource = getAttachedFunctionType(service, "onMessage");
        Type[] parameterTypes = onMessageResource.getParameterTypes();
        if (parameterTypes.length == 1) {
            dispatch(ballerinaNatsMessage, msg.getSubject());
        } else {
            Type intendedTypeForData = parameterTypes[1];
            dispatch(ballerinaNatsMessage, intendedTypeForData, msg.getData(), msg.getSubject());
        }
    }

    private void dispatch(BObject ballerinaNatsMessage, String subject) {
        executeResource(subject, ballerinaNatsMessage);
    }

    private void dispatch(BObject ballerinaNatsMessage, Type intendedTypeForData, byte[] data, String subject) {
        try {
            Object typeBoundData = Utils.bindDataToIntendedType(data, intendedTypeForData);
            executeResource(subject, ballerinaNatsMessage, typeBoundData);
        } catch (NumberFormatException e) {
            BError dataBindError = Utils
                    .createNatsError("The received message is unsupported by the resource signature");
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            executeErrorResource(subject, ballerinaNatsMessage, dataBindError);
        } catch (BError e) {
            executeErrorResource(subject, ballerinaNatsMessage, e);
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
        }
    }

    private void executeResource(String subject, BObject ballerinaNatsMessage) {
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(NatsObservabilityConstants.CONTEXT_CONSUMER,
                                                                          connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, ON_MESSAGE_METADATA, new DispatcherCallback(subject, natsMetricsReporter),
                                      properties, ballerinaNatsMessage, true);
        } else {
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, ON_MESSAGE_METADATA, new DispatcherCallback(subject, natsMetricsReporter),
                                      null, ballerinaNatsMessage, true);
        }
    }

    private void executeResource(String subject, BObject ballerinaNatsMessage, Object typeBoundData) {
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(NatsObservabilityConstants.CONTEXT_CONSUMER,
                                                                          connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, ON_MESSAGE_METADATA, new DispatcherCallback(subject, natsMetricsReporter),
                                      properties, ballerinaNatsMessage, true, typeBoundData, true);
        } else {
            runtime.invokeMethodAsync(service, ON_MESSAGE_RESOURCE,
                                      null, ON_MESSAGE_METADATA, new DispatcherCallback(subject, natsMetricsReporter),
                                      null, ballerinaNatsMessage, true, typeBoundData, true);
        }
    }

    private void executeErrorResource(String subject, BObject ballerinaNatsMessage, BError error) {
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(NatsObservabilityConstants.CONTEXT_CONSUMER,
                                                                          connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            runtime.invokeMethodAsync(service, ON_ERROR_RESOURCE, null, ON_ERROR_METADATA,
                                      new DispatcherCallback(subject, natsMetricsReporter),
                                      properties, ballerinaNatsMessage, true, error, true);
        } else {
            runtime.invokeMethodAsync(service, ON_ERROR_RESOURCE, null, ON_ERROR_METADATA,
                                      new DispatcherCallback(subject, natsMetricsReporter),
                                      null, ballerinaNatsMessage, true, error, true);
        }
    }


    private static class DispatcherCallback implements Callback {

        private String subject;
        private NatsMetricsReporter natsMetricsReporter;

        public DispatcherCallback(String subject, NatsMetricsReporter natsMetricsReporter) {
            this.subject = subject;
            this.natsMetricsReporter = natsMetricsReporter;
        }

        @Override
        public void notifySuccess() {
            natsMetricsReporter.reportDelivery(subject);
        }

        @Override
        public void notifyFailure(io.ballerina.runtime.api.values.BError error) {
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            error.printStackTrace();
        }
    }
}
