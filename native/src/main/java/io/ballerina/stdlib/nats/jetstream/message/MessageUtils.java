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

package io.ballerina.stdlib.nats.jetstream.message;

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.nats.Constants;
import io.nats.client.Message;

/**
 * Extern functions of the APIs provided by the JetStreamMessage client.
 */
public class MessageUtils {
    public static void ack(BObject messageObj) {
        Message streamMessage = (Message) messageObj.getNativeData(Constants.JET_STREAM_MESSAGE);
        streamMessage.ack();
    }

    public static void nak(BObject messageObj) {
        Message streamMessage = (Message) messageObj.getNativeData(Constants.JET_STREAM_MESSAGE);
        streamMessage.nak();
    }

    public static void inProgress(BObject messageObj) {
        Message streamMessage = (Message) messageObj.getNativeData(Constants.JET_STREAM_MESSAGE);
        streamMessage.inProgress();
    }

    private MessageUtils() {}
}
