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

package io.ballerina.stdlib.nats;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Module;
import io.ballerina.runtime.api.creators.ErrorCreator;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.ArrayType;
import io.ballerina.runtime.api.types.IntersectionType;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.types.UnionType;
import io.ballerina.runtime.api.utils.JsonUtils;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.utils.ValueUtils;
import io.ballerina.runtime.api.utils.XmlUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.constraint.Constraints;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static io.ballerina.runtime.api.types.TypeTags.ANYDATA_TAG;
import static io.ballerina.runtime.api.types.TypeTags.ARRAY_TAG;
import static io.ballerina.runtime.api.types.TypeTags.BYTE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.INTERSECTION_TAG;
import static io.ballerina.runtime.api.types.TypeTags.RECORD_TYPE_TAG;
import static io.ballerina.runtime.api.types.TypeTags.STRING_TAG;
import static io.ballerina.runtime.api.types.TypeTags.UNION_TAG;
import static io.ballerina.runtime.api.types.TypeTags.XML_TAG;
import static io.ballerina.runtime.api.utils.TypeUtils.getReferredType;
import static io.ballerina.stdlib.nats.Constants.NATS_ERROR;
import static io.ballerina.stdlib.nats.Constants.PAYLOAD_BINDING_ERROR;
import static io.ballerina.stdlib.nats.Constants.PAYLOAD_VALIDATION_ERROR;

/**
 * Utilities for producing and consuming via NATS sever.
 */
public class Utils {

    private static Module natsModule = null;

    private Utils() {
    }

    public static void setModule(Environment env) {
        natsModule = env.getCurrentModule();
    }

    public static Module getModule() {
        return natsModule;
    }

    public static BError createNatsError(String detailedErrorMessage) {
        return ErrorCreator.createError(getModule(), NATS_ERROR, StringUtils.fromString(detailedErrorMessage),
                null, null);
    }

    public static BError createNatsError(String detailedErrorMessage, Exception ex) {
        String errorMsg = detailedErrorMessage + " " + (ex.getMessage() != null ? ex.getMessage() : "");
        return ErrorCreator.createError(getModule(), NATS_ERROR, StringUtils.fromString(errorMsg),
                null, null);
    }

    public static BError createPayloadValidationError(String message, Object results) {
        return ErrorCreator.createError(getModule(), PAYLOAD_VALIDATION_ERROR, StringUtils.fromString(message),
                ErrorCreator.createError(StringUtils.fromString(results.toString())), null);
    }

    public static BError createPayloadBindingError(String message, BError cause) {
        return ErrorCreator.createError(getModule(), PAYLOAD_BINDING_ERROR, StringUtils.fromString(message),
                cause, null);
    }

    public static byte[] convertDataIntoByteArray(Object data, Type dataType) {
        int typeTag = dataType.getTag();
        if (typeTag == ARRAY_TAG) {
            return convertDataIntoByteArray(data, getReferredType(((BArray) data).getElementType()));
        } else if (typeTag == STRING_TAG) {
            return ((BString) data).getValue().getBytes(StandardCharsets.UTF_8);
        } else if (typeTag == TypeTags.XML_ELEMENT_TAG || typeTag == XML_TAG || typeTag == TypeTags.MAP_TAG ||
                typeTag == TypeTags.JSON_TAG || typeTag == TypeTags.TABLE_TAG || typeTag == RECORD_TYPE_TAG ||
                typeTag == TypeTags.INT_TAG || typeTag == TypeTags.DECIMAL_TAG || typeTag == TypeTags.FLOAT_TAG ||
                typeTag == TypeTags.BOOLEAN_TAG) {
            return data.toString().getBytes(StandardCharsets.UTF_8);
        } else {
            return ((BArray) data).getBytes();
        }
    }

    @SuppressWarnings("unchecked")
    public static BMap<BString, Object> getSubscriptionConfig(Object annotationData) {
        BMap annotationRecord = null;
        if (TypeUtils.getType(annotationData).getTag() == TypeTags.RECORD_TYPE_TAG) {
            annotationRecord = (BMap) annotationData;
        }
        return annotationRecord;
    }

    public static RecordType getRecordType(BTypedesc bTypedesc) {
        RecordType recordType;
        if (bTypedesc.getDescribingType().isReadOnly()) {
            recordType = (RecordType)
                    ((IntersectionType) (bTypedesc.getDescribingType())).getConstituentTypes().get(0);
        } else {
            recordType = (RecordType) bTypedesc.getDescribingType();
        }
        return recordType;
    }

    public static Object getValueWithIntendedType(Type type, byte[] value) throws BError {
        String strValue = new String(value, StandardCharsets.UTF_8);
        try {
            switch (type.getTag()) {
                case STRING_TAG:
                    return StringUtils.fromString(strValue);
                case XML_TAG:
                    return XmlUtils.parse(strValue);
                case ANYDATA_TAG:
                    return ValueCreator.createArrayValue(value);
                case RECORD_TYPE_TAG:
                    return ValueUtils.convert(JsonUtils.parse(strValue), type);
                case UNION_TAG:
                    if (hasStringType((UnionType) type)) {
                        return StringUtils.fromString(strValue);
                    }
                    return getValueFromJson(type, strValue);
                case ARRAY_TAG:
                    if (getReferredType(((ArrayType) type).getElementType()).getTag() == BYTE_TAG) {
                        return ValueCreator.createArrayValue(value);
                    }
                    /*-fallthrough*/
                default:
                    return getValueFromJson(type, strValue);
            }
        } catch (BError bError) {
            throw createNatsError(String.format("Data binding failed: %s", bError.getMessage()), bError);
        }
    }

    private static boolean hasStringType(UnionType type) {
        return type.getMemberTypes().stream().anyMatch(memberType -> {
            if (memberType.getTag() == STRING_TAG) {
                return true;
            }
            return false;
        });
    }

    private static Object getValueFromJson(Type type, String stringValue) {
        return ValueUtils.convert(JsonUtils.parse(stringValue), type);
    }

    public static Object validateConstraints(Object value, BTypedesc bTypedesc, boolean constraintValidation) {
        if (constraintValidation) {
            Object validationResult = Constraints.validate(value, bTypedesc);
            if (validationResult instanceof BError) {
                throw createPayloadValidationError(((BError) validationResult).getMessage(), value);
            }
        }
        return value;
    }

    public static BTypedesc getElementTypeDescFromArrayTypeDesc(BTypedesc bTypeDesc) {
        if (bTypeDesc.getDescribingType().getTag() == INTERSECTION_TAG) {
            return ValueCreator.createTypedescValue((((IntersectionType) bTypeDesc.getDescribingType())
                    .getConstituentTypes().get(0)));
        }
        return ValueCreator.createTypedescValue((bTypeDesc.getDescribingType()));
    }

    public static Map<String, Object> getProperties(String resourceName) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("moduleOrg", getModule().getOrg());
        properties.put("moduleName", getModule().getName());
        properties.put("moduleVersion", getModule().getMajorVersion());
        properties.put("parentFunctionName", resourceName);
        return properties;
    }
}
