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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.concurrent.StrandMetadata;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.ObjectType;
import io.ballerina.runtime.api.types.Parameter;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.observability.ObservabilityConstants;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.ballerina.stdlib.nats.observability.NatsObserverContext;
import io.nats.client.Message;
import io.nats.client.MessageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles incoming message for a given subscription.
 */
public class StreamMessageHandler implements MessageHandler {

    private final BObject service;
    private final Runtime runtime;
    private final String connectedUrl;
    private final boolean autoAck;

    public StreamMessageHandler(BObject service, Runtime runtime, String connectedUrl, boolean autoAck) {
        this.service = service;
        this.runtime = runtime;
        this.autoAck = autoAck;
        this.connectedUrl = connectedUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMessage(Message msg) {
        BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Utils.getModule(), Constants.STREAM_MESSAGE);
        Object[] msgRecordValues = new Object[2];

        msgRecordValues[0] = StringUtils.fromString(msg.getSubject());
        msgRecordValues[1] = ValueCreator.createArrayValue(msg.getData());

        BMap<BString, Object> populatedMsgRecord = ValueCreator.createRecordValue(msgRecord, msgRecordValues);

        BObject callerObj = ValueCreator.createObjectValue(Utils.getModule(), Constants.STREAM_CALLER);
        callerObj.addNativeData(Constants.JET_STREAM_MESSAGE, msg);
        callerObj.addNativeData(Constants.JET_STREAM_AUTO_ACK, autoAck);

        MethodType onMessageResource = getAttachedFunctionType(service, "onMessage");
        Type returnType = onMessageResource.getReturnType();
        Parameter[] parameters = onMessageResource.getParameters();
        if (parameters.length == 1) {
            Object[] args1 = new Object[1];
            if (parameters[0].type.getTag() == TypeTags.INTERSECTION_TAG) {
                args1[0] = getReadonlyMessage(msg);
            } else {
                args1[0] = populatedMsgRecord;
            }
            dispatch(args1, msg.getSubject(), returnType);
        } else if (parameters.length == 2) {
            Object[] args2 = new Object[2];
            if (parameters[0].type.getTag() == TypeTags.INTERSECTION_TAG) {
                args2[0] = getReadonlyMessage(msg);
            } else {
                args2[0] = populatedMsgRecord;
            }
            args2[1] = callerObj;
            dispatch(args2, msg.getSubject(), returnType);
        } else {
            throw Utils.createNatsError("Invalid remote function signature.");
        }
    }

    private BMap<BString, Object> getReadonlyMessage(Message msg) {
        Map<String, Object> valueMap = new HashMap<>();
        valueMap.put(Constants.MESSAGE_CONTENT, ValueCreator.createArrayValue(msg.getData()));
        valueMap.put(Constants.MESSAGE_SUBJECT, StringUtils.fromString(msg.getSubject()));
        return ValueCreator.createReadonlyRecordValue(Utils.getModule(), Constants.STREAM_MESSAGE, valueMap);
    }

    private void dispatch(Object[] args, String subject, Type returnType) {
        executeResource(subject, args, returnType);
    }

    private void executeResource(String subject, Object[] args, Type returnType) {
        ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(service));
        Map<String, Object> properties = null;
        if (ObserveUtils.isTracingEnabled()) {
            properties = new HashMap<>();
            NatsObserverContext observerContext = new NatsObserverContext(NatsObservabilityConstants.CONTEXT_CONSUMER,
                    connectedUrl, subject);
            properties.put(ObservabilityConstants.KEY_OBSERVER_CONTEXT, observerContext);
        }
        try {
            boolean isConcurrentSafe = objectType.isIsolated() &&
                    objectType.isIsolated(Constants.ON_MESSAGE_RESOURCE);
            Object result = runtime.callMethod(service, Constants.ON_MESSAGE_RESOURCE,
                    new StrandMetadata(isConcurrentSafe, properties), args);
            if (result instanceof BError) {
                ((BError) result).printStackTrace();
            }
        } catch (BError bError) {
            bError.printStackTrace();
            // Service level `panic` is captured in this method.
            // Since, `panic` is due to a critical application bug or resource exhaustion
            // we need to exit the application.
            // Please refer: https://github.com/ballerina-platform/ballerina-standard-library/issues/2714
        }
    }

    private static MethodType getAttachedFunctionType(BObject serviceObject, String functionName) {
        MethodType function = null;
        ObjectType objectType = (ObjectType) TypeUtils.getReferredType(TypeUtils.getType(serviceObject));
        MethodType[] remoteFunctions = objectType.getMethods();
        for (MethodType resourceFunction : remoteFunctions) {
            if (functionName.equals(resourceFunction.getName())) {
                function = resourceFunction;
                break;
            }
        }
        return function;
    }
}
