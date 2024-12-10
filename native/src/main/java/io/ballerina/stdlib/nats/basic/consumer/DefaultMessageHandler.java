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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.PredefinedTypes;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
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
import io.ballerina.stdlib.nats.Handler;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.ballerina.stdlib.nats.observability.NatsObserverContext;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.MessageHandler;
import org.ballerinalang.langlib.value.CloneReadOnly;

import java.util.HashMap;
import java.util.Map;

import static io.ballerina.runtime.api.types.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.types.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.runtime.api.utils.TypeUtils.getReferredType;
import static io.ballerina.stdlib.nats.Constants.CONSTRAINT_VALIDATION;
import static io.ballerina.stdlib.nats.Constants.IS_ANYDATA_MESSAGE;
import static io.ballerina.stdlib.nats.Constants.MESSAGE_CONTENT;
import static io.ballerina.stdlib.nats.Constants.PARAM_ANNOTATION_PREFIX;
import static io.ballerina.stdlib.nats.Constants.PARAM_PAYLOAD_ANNOTATION_NAME;
import static io.ballerina.stdlib.nats.Constants.TYPE_CHECKER_OBJECT_NAME;
import static io.ballerina.stdlib.nats.Utils.createPayloadBindingError;
import static io.ballerina.stdlib.nats.Utils.getElementTypeDescFromArrayTypeDesc;
import static io.ballerina.stdlib.nats.Utils.getModule;
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
        try {
            Object[] arguments = getResourceArguments(data, replyTo, subject, methodType);
            executeOnRequestResource(subject, replyTo, arguments);
        } catch (BError bError) {
            if (getAttachedFunctionType(serviceObject, Constants.ON_ERROR_RESOURCE) != null) {
                executeOnErrorResource(subject, replyTo, data, bError);
            }
        }
    }

    /**
     * Dispatch only the message to the onMessage resource.
     */
    private void dispatchOnMessage(String subject, String replyTo, byte[] data) throws InterruptedException {
        MethodType methodType = getAttachedFunctionType(this.serviceObject, Constants.ON_MESSAGE_RESOURCE);
        try {
            Object[] arguments = getResourceArguments(data, replyTo, subject, methodType);
            executeOnMessageResource(subject, replyTo, arguments);
        } catch (BError bError) {
            if (getAttachedFunctionType(serviceObject, Constants.ON_ERROR_RESOURCE) != null) {
                executeOnErrorResource(subject, replyTo, data, bError);
            }
        }
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

    private void executeOnRequestResource(String subject, String replyTo,
                                          Object... args) {
        executeResource(Constants.ON_REQUEST_RESOURCE, new ResponseHandler(subject,
                        natsMetricsReporter, replyTo, this.natsConnection), PredefinedTypes.TYPE_ANYDATA,
                subject, args);
    }

    private void executeOnErrorResource(String subject, String replyTo, byte[] data,
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
        ResponseHandler handler = new ResponseHandler(subject, natsMetricsReporter);
        try {
            Object result = runtime.callMethod(serviceObject, Constants.ON_ERROR_RESOURCE, null,
                    msgObj, bError);
            handler.notifySuccess(result);
        } catch (BError bError1) {
            handler.notifyFailure(bError1);
        }
    }

    private void executeOnMessageResource(String subject,
                                          String replyTo, Object... args) {
        executeResource(Constants.ON_MESSAGE_RESOURCE, new ResponseHandler(subject,
                        natsMetricsReporter), PredefinedTypes.TYPE_NULL,
                replyTo, args);
    }

    private void executeResource(String function, Handler callback, Type returnType, String subject, Object... args) {
        ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(serviceObject));
        Thread.startVirtualThread(() -> {
            Map<String, Object> properties = Utils.getProperties(function);
            if (ObserveUtils.isTracingEnabled()) {
                NatsObserverContext observerContext = new NatsObserverContext(
                        NatsObservabilityConstants.CONTEXT_CONSUMER, connectedUrl, subject);
                properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
            }
            boolean isConcurrentSafe = objectType.isIsolated() && objectType.isIsolated(function);
            try {
                Object result = runtime.callMethod(serviceObject, function, new StrandMetadata(isConcurrentSafe,
                        properties), args);
                callback.notifySuccess(result);
            } catch (BError bError) {
                callback.notifyFailure(bError);
            }
        });
    }

    private Object[] getResourceArguments(byte[] message, String replyTo, String subject, MethodType remoteFunction) {
        Parameter[] parameters = remoteFunction.getParameters();
        boolean messageExists = false;
        boolean payloadExists = false;
        boolean constraintValidation = (boolean) listenerObj.getNativeData(CONSTRAINT_VALIDATION);
        Object[] arguments = new Object[parameters.length];
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
                        Object record = createAndPopulateMessageRecord(message, replyTo, subject, referredType);
                        validateConstraints(record, getElementTypeDescFromArrayTypeDesc(ValueCreator
                                .createTypedescValue(parameter.type)), constraintValidation);
                        arguments[index++] = record;
                        break;
                    }
                    /*-fallthrough*/
                default:
                    if (payloadExists) {
                        throw Utils.createNatsError("Invalid remote function signature");
                    }
                    payloadExists = true;
                    Object value = createPayload(message, referredType);
                    validateConstraints(value, getElementTypeDescFromArrayTypeDesc(ValueCreator
                            .createTypedescValue(parameter.type)), constraintValidation);
                    arguments[index++] = value;
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
        NatsTypeCheckHandler messageTypeCheckCallback = new NatsTypeCheckHandler();
        try {
            Object result = runtime.callMethod(client, IS_ANYDATA_MESSAGE, null,
                    ValueCreator.createTypedescValue(paramType));
            messageTypeCheckCallback.notifySuccess(result);
        } catch (BError bError) {
            messageTypeCheckCallback.notifyFailure(bError);
        }
        return messageTypeCheckCallback.getIsMessageType();
    }

    private static BMap<BString, Object> createAndPopulateMessageRecord(byte[] message, String replyTo, String subject,
                                                                        Type messageType) {
        RecordType recordType = getRecordType(messageType);
        Type intendedType = TypeUtils.getReferredType(recordType.getFields().get(MESSAGE_CONTENT).getFieldType());
        BMap<BString, Object> messageRecord = ValueCreator.createRecordValue(recordType);
        Object messageContent = Utils.getValueWithIntendedType(intendedType, message);
        if (messageContent instanceof BError) {
            throw createPayloadBindingError(String.format("Data binding failed: %s", ((BError) messageContent)
                    .getMessage()), (BError) messageContent);
        }
        messageRecord.put(StringUtils.fromString(MESSAGE_CONTENT), messageContent);
        messageRecord.put(StringUtils.fromString(Constants.MESSAGE_SUBJECT), StringUtils.fromString(subject));
        if (replyTo != null) {
            messageRecord.put(StringUtils.fromString(Constants.MESSAGE_REPLY_TO), StringUtils.fromString(replyTo));
        }
        if (messageType.getTag() == TypeTags.INTERSECTION_TAG) {
            messageRecord.freezeDirect();
        }
        return messageRecord;
    }

    private static Object createPayload(byte[] message, Type payloadType) {
        Object messageContent = Utils.getValueWithIntendedType(getPayloadType(payloadType), message);
        if (messageContent instanceof BError) {
            throw createPayloadBindingError(String.format("Data binding failed: %s", ((BError) messageContent)
                    .getMessage()), (BError) messageContent);
        }
        if (payloadType.isReadOnly()) {
            return CloneReadOnly.cloneReadOnly(messageContent);
        }
        return messageContent;
    }

    private static RecordType getRecordType(Type type) {
        if (type.getTag() == TypeTags.INTERSECTION_TAG) {
            return (RecordType) TypeUtils.getReferredType(((IntersectionType) (type)).getConstituentTypes().get(0));
        }
        return (RecordType) type;
    }

    private static Type getPayloadType(Type definedType) {
        if (definedType.getTag() == INTERSECTION_TAG) {
            return TypeUtils.getReferredType(definedType);
        }
        return definedType;
    }

    /**
     * Represents the callback which will be triggered upon submitting to resource.
     */
    public static class ResponseHandler implements Handler {
        private final String subject;
        private final NatsMetricsReporter natsMetricsReporter;
        private String replyTo;
        private Connection natsConnection;

        ResponseHandler(String subject, NatsMetricsReporter natsMetricsReporter) {
            this.subject = subject;
            this.natsMetricsReporter = natsMetricsReporter;
        }

        ResponseHandler(String subject, NatsMetricsReporter natsMetricsReporter,
                         String replyTo, Connection natsConnection) {
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
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifyFailure(BError error) {
            error.printStackTrace();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
            // Service level `panic` is captured in this method.
            // Since, `panic` is due to a critical application bug or resource exhaustion
            // we need to exit the application.
            // Please refer: https://github.com/ballerina-platform/ballerina-standard-library/issues/2714
        }
    }

    /**
     * {@code NatsTypeCheckCallback} provides ability to check whether a given type is a subtype of
     * nats:AnydataMessage.
     */
    public static class NatsTypeCheckHandler implements Handler {
        private Boolean isMessageType = false;

        @Override
        public void notifySuccess(Object obj) {
            isMessageType = (Boolean) obj;
        }

        @Override
        public void notifyFailure(BError error) {
            error.printStackTrace();
            // Service level `panic` is captured in this method.
            // Since, `panic` is due to a critical application bug or resource exhaustion we need
            // to exit the application.
            // Please refer: https://github.com/ballerina-platform/ballerina-standard-library/issues/2714
        }

        public boolean getIsMessageType() {
            return isMessageType;
        }
    }
}
