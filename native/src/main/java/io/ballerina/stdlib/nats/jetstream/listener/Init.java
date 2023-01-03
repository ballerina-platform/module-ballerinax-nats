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

import io.ballerina.runtime.api.values.BObject;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Initialize NATS JetStream listener using the connection.
 */
public class Init {
    public static Object streamListenerInit(BObject selfObj, BObject natsClientObj) {
        Connection natsConnection = (Connection) natsClientObj.getNativeData(Constants.NATS_CONNECTION);
        try {
            JetStream jetStream = natsConnection.jetStream();
            selfObj.addNativeData(Constants.JET_STREAM, jetStream);
            selfObj.addNativeData(Constants.NATS_CONNECTION, natsConnection);
            ConcurrentHashMap<String, Dispatcher> dispatcherList = new ConcurrentHashMap<>();
            selfObj.addNativeData(Constants.DISPATCHER_LIST, dispatcherList);
            ArrayList<JetStreamSubscription> subscriptionsList = new ArrayList<>();
            selfObj.addNativeData(Constants.BASIC_SUBSCRIPTION_LIST, subscriptionsList);
            List<BObject> serviceList = Collections.synchronizedList(new ArrayList<>());
            selfObj.addNativeData(Constants.SERVICE_LIST, serviceList);
        } catch (IOException e) {
            String errorMsg = "Error occurred while initializing the JetStreamListener. " +
                    (e.getMessage() != null ? e.getMessage() : "");
            return Utils.createNatsError(errorMsg);
        }
        return null;
    }
}
