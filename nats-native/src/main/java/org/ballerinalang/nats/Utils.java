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

package org.ballerinalang.nats;

import io.ballerina.runtime.JSONParser;
import io.ballerina.runtime.JSONUtils;
import io.ballerina.runtime.TypeChecker;
import io.ballerina.runtime.XMLFactory;
import io.ballerina.runtime.api.ErrorCreator;
import io.ballerina.runtime.api.StringUtils;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.ValueCreator;
import io.ballerina.runtime.api.types.AttachedFunctionType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.types.BRecordType;
import io.ballerina.runtime.util.exceptions.BallerinaException;
import io.ballerina.runtime.values.ArrayValue;
import io.ballerina.runtime.values.ArrayValueImpl;
import io.ballerina.runtime.values.DecimalValue;
import io.nats.client.Message;

import java.nio.charset.StandardCharsets;

/**
 * Utilities for producing and consuming via NATS sever.
 */
public class Utils {

    public static BError createNatsError(String detailedErrorMessage) {
        return ErrorCreator.createDistinctError(Constants.NATS_ERROR, Constants.NATS_PACKAGE_ID,
                                                   StringUtils.fromString(detailedErrorMessage));
    }

    public static Object bindDataToIntendedType(byte[] data, Type intendedType) {
        int dataParamTypeTag = intendedType.getTag();
        Object dispatchedData;
        switch (dataParamTypeTag) {
            case TypeTags.STRING_TAG:
                dispatchedData = StringUtils.fromString(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.JSON_TAG:
                try {
                    Object json = JSONParser.parse(new String(data, StandardCharsets.UTF_8));
                    dispatchedData = json instanceof String ? StringUtils.fromString((String) json) : json;
                } catch (BallerinaException e) {
                    throw createNatsError("Error occurred in converting message content to json: " +
                            e.getMessage());
                }
                break;
            case TypeTags.INT_TAG:
                dispatchedData = Integer.valueOf(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.BOOLEAN_TAG:
                dispatchedData = Boolean.valueOf(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.FLOAT_TAG:
                dispatchedData = Double.valueOf(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.DECIMAL_TAG:
                dispatchedData = new DecimalValue(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.ARRAY_TAG:
                dispatchedData = new ArrayValueImpl(data);
                break;
            case TypeTags.XML_TAG:
                dispatchedData = XMLFactory.parse(new String(data, StandardCharsets.UTF_8));
                break;
            case TypeTags.RECORD_TYPE_TAG:
                dispatchedData = JSONUtils.convertJSONToRecord(JSONParser.parse(new String(data,
                        StandardCharsets.UTF_8)), (BRecordType) intendedType);
                break;
            default:
                throw Utils.createNatsError("Unable to find a supported data type to bind the message data");
        }
        return dispatchedData;
    }

    public static BObject getMessageObject(Message message) {
        BObject msgObj;
        if (message != null) {
            ArrayValue msgData = new ArrayValueImpl(message.getData());
            msgObj = ValueCreator.createObjectValue(Constants.NATS_PACKAGE_ID,
                                                       Constants.NATS_MESSAGE_OBJ_NAME,
                                                       StringUtils.fromString(message.getSubject()), msgData,
                                                       StringUtils.fromString(message.getReplyTo()));
        } else {
            ArrayValue msgData = new ArrayValueImpl(new byte[0]);
            msgObj = ValueCreator.createObjectValue(Constants.NATS_PACKAGE_ID,
                    Constants.NATS_MESSAGE_OBJ_NAME, "", msgData, "");
        }
        return msgObj;
    }

    public static byte[] convertDataIntoByteArray(Object data) {
        Type dataType = TypeChecker.getType(data);
        int typeTag = dataType.getTag();
        if (typeTag == org.wso2.ballerinalang.compiler.util.TypeTags.STRING) {
            return ((BString) data).getValue().getBytes(StandardCharsets.UTF_8);
        } else {
            return ((ArrayValue) data).getBytes();
        }
    }

    public static AttachedFunctionType getAttachedFunctionType(BObject serviceObject, String functionName) {
        AttachedFunctionType function = null;
        AttachedFunctionType[] resourceFunctions = serviceObject.getType().getAttachedFunctions();
        for (AttachedFunctionType resourceFunction : resourceFunctions) {
            if (functionName.equals(resourceFunction.getName())) {
                function = resourceFunction;
                break;
            }
        }
        return function;
    }

    @SuppressWarnings("unchecked")
    public static BMap<BString, Object> getSubscriptionConfig(Object annotationData) {
        BMap annotationRecord = null;
        if (TypeChecker.getType(annotationData).getTag() == TypeTags.RECORD_TYPE_TAG) {
            annotationRecord = (BMap) annotationData;
        }
        return annotationRecord;
    }

    public static String getCommaSeparatedUrl(BObject connectionObject) {
        return String.join(", ", connectionObject.getArrayValue(Constants.URL).getStringArray());
    }
}
