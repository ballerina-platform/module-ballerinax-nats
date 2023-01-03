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

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;

import java.io.IOException;

/**
 * Initialize NATS JetStream client using the connection.
 */
public class Init {

    public static Object streamClientInit(BObject selfObj, BObject natsClientObj) {
        Connection natsConnection = (Connection) natsClientObj.getNativeData(Constants.NATS_CONNECTION);
        try {
            JetStreamManagement jetStreamManagement = natsConnection.jetStreamManagement();
            selfObj.addNativeData(Constants.JET_STREAM_MANAGEMENT, jetStreamManagement);
            selfObj.addNativeData(Constants.NATS_CONNECTION, natsConnection);
        } catch (IOException e) {
            String errorMsg = "Error occurred while initializing the JetStreamClient. " +
                    (e.getMessage() != null ? e.getMessage() : "");
            return Utils.createNatsError(errorMsg);
        }
        return null;
    }
}