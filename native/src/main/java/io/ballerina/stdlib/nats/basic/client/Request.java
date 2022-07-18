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

package io.ballerina.stdlib.nats.basic.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.types.Field;
import io.ballerina.runtime.api.types.RecordType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.runtime.api.values.BTypedesc;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.ballerina.stdlib.nats.observability.NatsObservabilityConstants;
import io.ballerina.stdlib.nats.observability.NatsTracingUtil;
import io.nats.client.Connection;
import io.nats.client.Message;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.ballerina.stdlib.nats.Constants.CONSTRAINT_VALIDATION;
import static io.ballerina.stdlib.nats.Utils.convertDataIntoByteArray;
import static io.ballerina.stdlib.nats.Utils.getElementTypeDescFromArrayTypeDesc;
import static io.ballerina.stdlib.nats.Utils.validateConstraints;

/**
 * Extern function to publish message to a given subject.
 *
 * @since 0.995
 */
public class Request {
    private static final BigDecimal MILLISECOND_MULTIPLIER = new BigDecimal(1000);
    
    @SuppressWarnings("unused")
    public static Object requestMessage(Environment environment, BObject clientObj, BMap<BString, Object> message,
                                       Object duration, BTypedesc bTypedesc) {
        String subject = message.getStringValue(StringUtils.fromString(Constants.MESSAGE_SUBJECT)).getValue();
        Object data = message.get(StringUtils.fromString(Constants.MESSAGE_CONTENT));
        NatsTracingUtil.traceResourceInvocation(environment, clientObj, subject);
        Connection natsConnection = (Connection) clientObj.getNativeData(Constants.NATS_CONNECTION);
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) clientObj.getNativeData(Constants.NATS_METRIC_UTIL);
        byte[] byteContent = convertDataIntoByteArray(data, TypeUtils.getType(data));
        try {
            Message reply;
            Future<Message> incoming = natsConnection.request(subject, byteContent);
            natsMetricsReporter.reportRequest(subject, byteContent.length);
            if (TypeUtils.getType(duration).getTag() == TypeTags.DECIMAL_TAG) {
                BigDecimal valueInSeconds = ((BDecimal) duration).decimalValue();
                int valueInMilliSeconds = (valueInSeconds).multiply(MILLISECOND_MULTIPLIER).intValue();
                reply = incoming.get(valueInMilliSeconds, TimeUnit.MILLISECONDS);
            } else {
                reply = incoming.get();
            }
            RecordType recordType = Utils.getRecordType(bTypedesc);

            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(recordType);
            Map<String, Field> fieldMap = recordType.getFields();
            Type contentType = TypeUtils.getReferredType(fieldMap.get(Constants.MESSAGE_CONTENT).getFieldType());

            BMap<BString, Object> populatedRecord = ValueCreator.createRecordValue(msgRecord,
                    Utils.getValueWithIntendedType(contentType, reply.getData()),
                    StringUtils.fromString(reply.getSubject()),
                    StringUtils.fromString(reply.getReplyTo()));
            boolean constraintValidation = (boolean) clientObj.getNativeData(CONSTRAINT_VALIDATION);
            validateConstraints(populatedRecord, getElementTypeDescFromArrayTypeDesc(bTypedesc), constraintValidation);
            natsMetricsReporter.reportResponse(subject);
            return populatedRecord;
        } catch (TimeoutException ex) {
            natsMetricsReporter.reportProducerError(subject, NatsObservabilityConstants.ERROR_TYPE_REQUEST);
            return Utils.createNatsError("Request to subject " + subject +
                                                 " timed out while waiting for a reply");
        } catch (IllegalArgumentException | IllegalStateException | ExecutionException | InterruptedException ex) {
            natsMetricsReporter.reportProducerError(subject, NatsObservabilityConstants.ERROR_TYPE_REQUEST);
            return Utils.createNatsError("Error while requesting message to " +
                                                 "subject " + subject + ". " + ex.getMessage());
        } catch (BError bError) {
            natsMetricsReporter.reportProducerError(subject, NatsObservabilityConstants.ERROR_TYPE_REQUEST);
            return bError;
        }
    }
}
