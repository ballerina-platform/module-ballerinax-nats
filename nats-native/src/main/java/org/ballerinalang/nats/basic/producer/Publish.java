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

package org.ballerinalang.nats.basic.producer;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.types.AnnotatableType;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;
import org.ballerinalang.nats.observability.NatsTracingUtil;

import static org.ballerinalang.nats.Utils.convertDataIntoByteArray;

/**
 * Extern function to publish message to a given subject.
 *
 * @since 0.995
 */
public class Publish {

    public static Object externPublish(Environment environment, BObject producerObject,
                                       BString subject, Object data, Object replyTo) {
        NatsTracingUtil.traceResourceInvocation(environment, producerObject, subject.getValue());
        Object connection = producerObject.get(Constants.CONNECTION_OBJ);
        if (TypeUtils.getType(connection).getTag() == TypeTags.OBJECT_TYPE_TAG) {
            BObject connectionObject = (BObject) connection;
            Connection natsConnection = (Connection) connectionObject.getNativeData(Constants.NATS_CONNECTION);
            NatsMetricsReporter natsMetricsReporter =
                    (NatsMetricsReporter) connectionObject.getNativeData(Constants.NATS_METRIC_UTIL);
            if (natsConnection == null) {
                natsMetricsReporter.reportProducerError(subject.getValue(),
                                                        NatsObservabilityConstants.ERROR_TYPE_PUBLISH);
                return Utils.createNatsError(Constants.PRODUCER_ERROR +
                                                     subject.getValue() + ". NATS connection doesn't exist.");
            }
            byte[] byteContent = convertDataIntoByteArray(data);
            try {
                if (TypeUtils.getType(replyTo).getTag() == TypeTags.STRING_TAG) {
                    natsConnection.publish(subject.getValue(), ((BString) replyTo).getValue(), byteContent);
                } else if (TypeUtils.getType(replyTo).getTag() == TypeTags.SERVICE_TAG) {
                    BMap<BString, Object> subscriptionConfig =
                            getSubscriptionConfig(((AnnotatableType) ((BObject) replyTo).getType()).getAnnotation(
                                    StringUtils.fromString(Constants.NATS_PACKAGE +
                                                                   ":" + Constants.SUBSCRIPTION_CONFIG)));
                    if (subscriptionConfig == null) {
                        natsMetricsReporter.reportProducerError(subject.getValue(),
                                                                NatsObservabilityConstants.ERROR_TYPE_PUBLISH);
                        return Utils.createNatsError("Cannot find subscription configuration");
                    }
                    String replyToSubject = subscriptionConfig.getStringValue(Constants.SUBJECT).getValue();
                    natsConnection.publish(subject.getValue(), replyToSubject, byteContent);
                } else {
                    natsConnection.publish(subject.getValue(), byteContent);
                }
                natsMetricsReporter.reportPublish(subject.getValue(), byteContent.length);
            } catch (IllegalArgumentException | IllegalStateException ex) {
                natsMetricsReporter.reportProducerError(subject.getValue(),
                                                        NatsObservabilityConstants.ERROR_TYPE_PUBLISH);
                return Utils.createNatsError(Constants.PRODUCER_ERROR +
                                                     subject.getValue() + ". " + ex.getMessage());
            }
        } else {
            NatsMetricsReporter.reportProducerError(NatsObservabilityConstants.ERROR_TYPE_PUBLISH);
            return Utils.createNatsError(Constants.PRODUCER_ERROR +
                                                 subject.getValue() + ". Producer is logically disconnected.");
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static BMap<BString, Object> getSubscriptionConfig(Object annotationData) {
        BMap annotationRecord = null;
        if (TypeUtils.getType(annotationData).getTag() == TypeTags.RECORD_TYPE_TAG) {
            annotationRecord = (BMap) annotationData;
        }
        return annotationRecord;
    }
}
