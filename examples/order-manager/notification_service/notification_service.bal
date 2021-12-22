// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerinax/nats;
import ballerina/lang.value;
import ballerina/log;
import notification_service.types;

configurable string LISTENING_SUBJECT = ?;

@nats:ServiceConfig {
    subject: LISTENING_SUBJECT
}
service nats:Service on new nats:Listener(nats:DEFAULT_URL) {

    // Listens to NATS subject for any successful orders
    remote function onMessage(nats:Message message) returns error? {

        // Convert the byte values in the NATS Message to type Order
        string messageContent = check string:fromBytes(message.content);
        json content = check value:fromJsonString(messageContent);
        json jsonMessage = content.cloneReadOnly();
        types:Order newOrder = check jsonMessage.ensureType(types:Order);
        log:printInfo("We have successfully ordered and going to send success message: " + newOrder.toString());
    }
}
