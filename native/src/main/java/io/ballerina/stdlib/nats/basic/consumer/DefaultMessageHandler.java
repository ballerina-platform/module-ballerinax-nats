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
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import static io.ballerina.runtime.api.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.runtime.api.utils.TypeUtils.getReferredType;
import static io.ballerina.stdlib.nats.Constants.CONSTRAINT_VALIDATION;
import static io.ballerina.stdlib.nats.Constants.IS_ANYDATA_MESSAGE;
import static io.ballerina.stdlib.nats.Constants.NATS;
import static io.ballerina.stdlib.nats.Constants.ORG_NAME;
import static io.ballerina.stdlib.nats.Constants.PARAM_ANNOTATION_PREFIX;
import static io.ballerina.stdlib.nats.Constants.PARAM_PAYLOAD_ANNOTATION_NAME;
import static io.ballerina.stdlib.nats.Constants.TYPE_CHECKER_OBJECT_NAME;
import static io.ballerina.stdlib.nats.Utils.getElementTypeDescFromArrayTypeDesc;
import static io.ballerina.stdlib.nats.Utils.getModule;
import static io.ballerina.stdlib.nats.Utils.getRecordType;
import static io.ballerina.stdlib.nats.Utils.validateConstraints;

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
    private final BObject listenerObj;

    DefaultMessageHandler(BObject serviceObject, Runtime runtime, Connection natsConnection,
                          NatsMetricsReporter natsMetricsReporter, BObject listenerObj) {
        this.serviceObject = serviceObject;
        this.runtime = runtime;
        this.connectedUrl = natsConnection.getConnectedUrl();
        this.natsMetricsReporter = natsMetricsReporter;
        this.natsConnection = natsConnection;
        this.listenerObj = listenerObj;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message message) {
        natsMetricsReporter.reportConsume(message.getSubject(), message.getData().length);
        String replyTo = message.getReplyTo();
        String subject = message.getSubject();
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
            throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR, e);
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     */
    private void dispatchOnRequest(String subject, String replyTo, byte[] data)
            throws InterruptedException {
        MethodType methodType = getAttachedFunctionType(this.serviceObject, Constants.ON_REQUEST_RESOURCE);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Object[] arguments = getResourceArguments(data, replyTo, subject, methodType);
            executeOnRequestResource(countDownLatch, subject, replyTo, arguments);
            countDownLatch.await();
        } catch (BError bError) {
            if (getAttachedFunctionType(serviceObject, Constants.ON_ERROR_RESOURCE) != null) {
                executeOnErrorResource(countDownLatch, subject, replyTo, data, bError);
            }
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     */
    private void dispatchOnMessage(String subject, String replyTo, byte[] data) throws InterruptedException {
        MethodType methodType = getAttachedFunctionType(this.serviceObject, Constants.ON_MESSAGE_RESOURCE);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            Object[] arguments = getResourceArguments(data, replyTo, subject, methodType);
            executeOnMessageResource(countDownLatch, subject, replyTo, arguments);
            countDownLatch.await();
        } catch (BError bError) {
            if (getAttachedFunctionType(serviceObject, Constants.ON_ERROR_RESOURCE) != null) {
                executeOnErrorResource(countDownLatch, subject, replyTo, data, bError);
            }
        }
    }

    private static Object createAndPopulateMessageRecord(byte[] message, String replyTo, String subject,
                                                         Parameter parameter) {
        BMap<BString, Object> msgObj;
        BArray msgData = ValueCreator.createArrayValue(message);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.MESSAGE_CONTENT, msgData);
        valueMap.put(Constants.MESSAGE_SUBJECT, StringUtils.fromString(subject));
        if (replyTo != null) {
            valueMap.put(Constants.MESSAGE_REPLY_TO, StringUtils.fromString(replyTo));
        }
        Type referredType = getReferredType(parameter.type);
        if (referredType.getTag() == TypeTags.INTERSECTION_TAG) {
            msgObj = ValueCreator.createReadonlyRecordValue(getModule(),
                    Constants.NATS_MESSAGE_OBJ_NAME, valueMap);
        } else {
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue((RecordType) referredType);
            Map<String, Field> fieldMap = ((RecordType) referredType).getFields();
            Type contentType = getReferredType(fieldMap.get(Constants.MESSAGE_CONTENT).getFieldType());
            Object msg = Utils.getValueWithIntendedType(contentType, message);
            msgObj = ValueCreator.createRecordValue(msgRecord, msg, StringUtils.fromString(subject),
                    StringUtils.fromString(replyTo));
        }
        return msgObj;
    }

    private static MethodType getAttachedFunctionType(BObject serviceObject, String functionName) {
        MethodType function = null;
        ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(serviceObject));
        MethodType[] remoteFunctions = objectType.getMethods();
        for (MethodType remoteFunction : remoteFunctions) {
            if (functionName.equals(remoteFunction.getName())) {
                function = remoteFunction;
                break;
            }
        }
        return function;
    }

    private void executeOnRequestResource(CountDownLatch countDownLatch, String subject,
                                          String replyTo, Object... args) {
        StrandMetadata metadata = new StrandMetadata(getModule().getOrg(), getModule().getName(),
                getModule().getVersion(), Constants.ON_REQUEST_RESOURCE);
        executeResource(Constants.ON_REQUEST_RESOURCE, new ResponseCallback(countDownLatch, subject,
                natsMetricsReporter, replyTo, this.natsConnection), metadata, PredefinedTypes.TYPE_ANYDATA,
                subject, args);
    }

    private void executeOnErrorResource(CountDownLatch countDownLatch, String subject, String replyTo, byte[] data,
                                        BError bError) {
        BMap<BString, Object> msgObj;
        BArray msgData = ValueCreator.createArrayValue(data);
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.MESSAGE_CONTENT, msgData);
        valueMap.put(Constants.MESSAGE_SUBJECT, StringUtils.fromString(subject));
        if (replyTo != null) {
            valueMap.put(Constants.MESSAGE_REPLY_TO, StringUtils.fromString(replyTo));
        }
        msgObj = ValueCreator.createReadonlyRecordValue(getModule(),
                Constants.NATS_MESSAGE_OBJ_NAME, valueMap);
        StrandMetadata metadata = new StrandMetadata(getModule().getOrg(), getModule().getName(),
                getModule().getVersion(), Constants.ON_ERROR_RESOURCE);
        runtime.invokeMethodAsyncSequentially(serviceObject, Constants.ON_ERROR_RESOURCE, null, metadata,
                new ResponseCallback(countDownLatch, subject, natsMetricsReporter), null,
                PredefinedTypes.TYPE_NULL, msgObj, true, bError, true);
    }

    private void executeOnMessageResource(CountDownLatch countDownLatch, String subject,
                                          String replyTo, Object... args) {
        StrandMetadata metadata = new StrandMetadata(getModule().getOrg(), getModule().getName(),
                getModule().getVersion(), Constants.ON_MESSAGE_RESOURCE);
        executeResource(Constants.ON_MESSAGE_RESOURCE, new ResponseCallback(countDownLatch, subject,
                natsMetricsReporter), metadata, PredefinedTypes.TYPE_NULL,
                replyTo, args);
    }

    private void executeResource(String function, Callback callback,
                                 StrandMetadata metadata, Type returnType, String subject, Object... args) {
        ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(serviceObject));
        if (ObserveUtils.isTracingEnabled()) {
            Map<String, Object> properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(
                    NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            if (objectType.isIsolated() &&
                    objectType.isIsolated(function)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, function, null, metadata,
                        callback, properties, returnType, args);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, function, null, metadata,
                        callback, properties, returnType, args);
            }
        } else {
            if (objectType.isIsolated() &&
                    objectType.isIsolated(function)) {
                runtime.invokeMethodAsyncConcurrently(serviceObject, function, null, metadata,
                        callback, null, returnType, args);
            } else {
                runtime.invokeMethodAsyncSequentially(serviceObject, function, null, metadata,
                        callback, null, returnType, args);
            }
        }
    }

    private Object[] getResourceArguments(byte[] message, String replyTo, String subject, MethodType remoteFunction) {
        Parameter[] parameters = remoteFunction.getParameters();
        boolean messageExists = false;
        boolean payloadExists = false;
        boolean constraintValidation = (boolean) listenerObj.getNativeData(CONSTRAINT_VALIDATION);
        Object[] arguments = new Object[parameters.length * 2];
        int index = 0;
        for (Parameter parameter : parameters) {
            Type referredType = getReferredType(parameter.type);
            switch (referredType.getTag()) {
                case INTERSECTION_TAG:
                case RECORD_TYPE_TAG:
                    if (isMessageType(parameter, remoteFunction.getAnnotations())) {
                        if (messageExists) {
                            throw Utils.createNatsError("Invalid remote function signature");
                        }
                        messageExists = true;
                        Object record = createAndPopulateMessageRecord(message, replyTo, subject, parameter);
                        arguments[index++] = validateConstraints(record, getElementTypeDescFromArrayTypeDesc(
                                ValueCreator.createTypedescValue(referredType)), constraintValidation);
                        arguments[index++] = true;
                        break;
                    }
                    /*-fallthrough*/
                default:
                    if (payloadExists) {
                        throw Utils.createNatsError("Invalid remote function signature");
                    }
                    payloadExists = true;
                    Object value = Utils.getValueWithIntendedType(getPayloadType(referredType), message);
                    arguments[index++] = validateConstraints(value, getElementTypeDescFromArrayTypeDesc(
                            ValueCreator.createTypedescValue(parameter.type)), constraintValidation);
                    arguments[index++] = true;
                    break;
            }
        }
        return arguments;
    }

    private boolean isMessageType(Parameter parameter, BMap<BString, Object> annotations) {
        if (annotations.containsKey(StringUtils.fromString(PARAM_ANNOTATION_PREFIX + parameter.name))) {
            BMap paramAnnotationMap = annotations.getMapValue(StringUtils.fromString(
                    PARAM_ANNOTATION_PREFIX + parameter.name));
            if (paramAnnotationMap.containsKey(PARAM_PAYLOAD_ANNOTATION_NAME)) {
                return false;
            }
        }
        return invokeIsAnydataMessageTypeMethod(getRecordType(getReferredType(parameter.type)));
    }

    private boolean invokeIsAnydataMessageTypeMethod(Type paramType) {
        BObject client = ValueCreator.createObjectValue(getModule(), TYPE_CHECKER_OBJECT_NAME);
        Semaphore sem = new Semaphore(0);
        NatsTypeCheckCallback messageTypeCheckCallback = new NatsTypeCheckCallback(sem);
        StrandMetadata metadata = new StrandMetadata(ORG_NAME, NATS,
                getModule().getVersion(), IS_ANYDATA_MESSAGE);
        runtime.invokeMethodAsyncSequentially(client, IS_ANYDATA_MESSAGE, null, metadata,
                messageTypeCheckCallback, null, PredefinedTypes.TYPE_BOOLEAN,
                ValueCreator.createTypedescValue(paramType), true);
        try {
            sem.acquire();
        } catch (InterruptedException e) {
            throw Utils.createNatsError(e.getMessage());
        }
        return messageTypeCheckCallback.getIsMessageType();
    }

    private static Type getPayloadType(Type definedType) {
        if (definedType.getTag() == INTERSECTION_TAG) {
            return  ((IntersectionType) definedType).getConstituentTypes().get(0);
        }
        return definedType;
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
                natsConnection.publish(replyTo, Utils.convertDataIntoByteArray(obj, TypeUtils.getType(obj)));
            }
            natsMetricsReporter.reportDelivery(subject);
            countDownLatch.countDown();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifyFailure(BError error) {
            error.printStackTrace();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            countDownLatch.countDown();
            // Service level `panic` is captured in this method.
            // Since, `panic` is due to a critical application bug or resource exhaustion
            // we need to exit the application.
            // Please refer: https://github.com/ballerina-platform/ballerina-standard-library/issues/2714
            System.exit(1);
        }
    }

    /**
     * {@code NatsTypeCheckCallback} provides ability to check whether a given type is a subtype of
     * nats:AnydataMessage.
     */
    public static class NatsTypeCheckCallback implements Callback {

        private final Semaphore semaphore;
        private Boolean isMessageType = false;

        NatsTypeCheckCallback(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void notifySuccess(Object obj) {
            isMessageType = (Boolean) obj;
            semaphore.release();
        }

        @Override
        public void notifyFailure(BError error) {
            semaphore.release();
            error.printStackTrace();
            // Service level `panic` is captured in this method.
            // Since, `panic` is due to a critical application bug or resource exhaustion we need
            // to exit the application.
            // Please refer: https://github.com/ballerina-platform/ballerina-standard-library/issues/2714
            System.exit(1);
        }

        public boolean getIsMessageType() {
            return isMessageType;
        }
    }
}
