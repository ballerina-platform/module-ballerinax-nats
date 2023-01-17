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

package io.ballerina.stdlib.nats.jetstream.client;

import io.ballerina.runtime.api.creators.ValueCreator;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;

import java.io.IOException;
import java.time.Duration;

/**
 * Extern functions of the APIs provided by the JetStreamManagementClient.
 */
public class ClientUtils {

    private ClientUtils() {}

    public static Object streamClientInit(BObject selfObj, BObject natsClientObj) {
        Connection natsConnection = (Connection) natsClientObj.getNativeData(Constants.NATS_CONNECTION);
        try {
            JetStreamManagement jetStreamManagement = natsConnection.jetStreamManagement();
            selfObj.addNativeData(Constants.JET_STREAM_MANAGEMENT, jetStreamManagement);
            selfObj.addNativeData(Constants.NATS_CONNECTION, natsConnection);
        } catch (IOException e) {
            String errorMsg = "Error occurred while initializing the JetStreamClient.";
            return Utils.createNatsError(errorMsg, e);
        }
        return null;
    }

    public static Object publishMessage(BObject clientObject, BMap<BString, Object> message) {
        try {
            Connection natsConnection = (Connection) clientObject.getNativeData(Constants.NATS_CONNECTION);
            JetStream jetStream;
            if (clientObject.getNativeData(Constants.JET_STREAM) != null) {
                jetStream = (JetStream) clientObject.getNativeData(Constants.JET_STREAM);
            } else {
                jetStream = natsConnection.jetStream();
                clientObject.addNativeData(Constants.JET_STREAM, jetStream);
            }
            byte[] byteContent =
                    (message.getArrayValue(StringUtils.fromString(Constants.MESSAGE_CONTENT))).getByteArray();
            String subjectValue =
                    (message.getStringValue(StringUtils.fromString(Constants.MESSAGE_SUBJECT))).getValue();
            jetStream.publish(subjectValue, byteContent);
        } catch (IOException | JetStreamApiException e) {
            String errorMsg = "Error occurred while publishing message.";
            return Utils.createNatsError(errorMsg, e);
        }
        return null;
    }

    public static Object consumeMessage(BObject clientObject, BString subject, BDecimal timeout) {
        try {
            Connection natsConnection = (Connection) clientObject.getNativeData(Constants.NATS_CONNECTION);
            JetStream jetStream;
            if (clientObject.getNativeData(Constants.JET_STREAM) != null) {
                jetStream = (JetStream) clientObject.getNativeData(Constants.JET_STREAM);
            } else {
                jetStream = natsConnection.jetStream();
                clientObject.addNativeData(Constants.JET_STREAM, jetStream);
            }
            JetStreamSubscription streamSubscription = jetStream.subscribe(subject.getValue());
            Message message = streamSubscription.nextMessage(Duration.ofSeconds(timeout.intValue()));
            BMap<BString, Object> msgRecord = ValueCreator.createRecordValue(
                    Utils.getModule(), Constants.STREAM_MESSAGE);
            Object[] msgRecordValues = new Object[2];

            msgRecordValues[0] = StringUtils.fromString(message.getSubject());
            msgRecordValues[1] = ValueCreator.createArrayValue(message.getData());

            BMap<BString, Object> populatedMsgRecord = ValueCreator.createRecordValue(msgRecord, msgRecordValues);
            populatedMsgRecord.addNativeData(Constants.JET_STREAM_MESSAGE, message);
            return populatedMsgRecord;
        } catch (IOException | JetStreamApiException | InterruptedException e) {
            String errorMsg = "Error occurred while consuming message.";
            return Utils.createNatsError(errorMsg, e);
        }
    }

    public static void ack(BMap<BString, Object> message) {
        Message jetStreamMessage = (Message) message.getNativeData(Constants.JET_STREAM_MESSAGE);
        jetStreamMessage.ack();
    }

    public static void nak(BMap<BString, Object> message) {
        Message jetStreamMessage = (Message) message.getNativeData(Constants.JET_STREAM_MESSAGE);
        jetStreamMessage.nak();
    }

    public static void inProgress(BMap<BString, Object> message) {
        Message jetStreamMessage = (Message) message.getNativeData(Constants.JET_STREAM_MESSAGE);
        jetStreamMessage.inProgress();
    }
}
