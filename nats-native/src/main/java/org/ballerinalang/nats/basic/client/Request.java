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

package org.ballerinalang.nats.basic.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.Message;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsTracingUtil;

import java.math.BigDecimal;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.ballerinalang.nats.Utils.convertDataIntoByteArray;

/**
 * Extern function to publish message to a given subject.
 *
 * @since 0.995
 */
public class Request {
    private static final BigDecimal MILLISECOND_MULTIPLIER = new BigDecimal(1000);
    
    @SuppressWarnings("unused")
    public static Object externRequest(Environment environment, BObject clientObj, BString subject, BArray data,
                                       Object duration) {
        NatsTracingUtil.traceResourceInvocation(environment, clientObj, subject.getValue());
        Connection natsConnection = (Connection) clientObj.getNativeData(Constants.NATS_CONNECTION);
        NatsMetricsReporter natsMetricsReporter =
                (NatsMetricsReporter) clientObj.getNativeData(Constants.NATS_METRIC_UTIL);
        byte[] byteContent = convertDataIntoByteArray(data);
        try {
            Message reply;
            Future<Message> incoming = natsConnection.request(subject.getValue(), byteContent);
            natsMetricsReporter.reportRequest(subject.getValue(), byteContent.length);
            if (TypeUtils.getType(duration).getTag() == TypeTags.DECIMAL_TAG) {
                BigDecimal valueInSeconds = ((BDecimal) duration).decimalValue();
                int valueInMilliSeconds = (valueInSeconds).multiply(MILLISECOND_MULTIPLIER).intValue();
                reply = incoming.get(valueInMilliSeconds, TimeUnit.MILLISECONDS);
            } else {
                reply = incoming.get();
            }
            BArray msgData = ValueCreator.createArrayValue(reply.getData());
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(Utils.getModule(),
                                                                             Constants.NATS_MESSAGE_OBJ_NAME);
            BMap<BString, Object> populatedRecord = ValueCreator.createRecordValue(msgRecord, msgData,
                                                                   StringUtils.fromString(reply.getSubject()),
                                                                   StringUtils.fromString(reply.getReplyTo()));
            natsMetricsReporter.reportResponse(subject.getValue());
            return populatedRecord;
        } catch (TimeoutException ex) {
            natsMetricsReporter.reportProducerError(subject.getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_REQUEST);
            return Utils.createNatsError("Request to subject " + subject.getValue() +
                                                 " timed out while waiting for a reply");
        } catch (IllegalArgumentException | IllegalStateException | ExecutionException | InterruptedException ex) {
            natsMetricsReporter.reportProducerError(subject.getValue(),
                                                    NatsObservabilityConstants.ERROR_TYPE_REQUEST);
            return Utils.createNatsError("Error while requesting message to " +
                                                 "subject " + subject.getValue() + ". " + ex.getMessage());
        }
    }
}
