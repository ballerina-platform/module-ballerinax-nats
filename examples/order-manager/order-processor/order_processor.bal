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
import order_processor.types;
import ballerina/lang.value;
import ballerina/log;

configurable string LISTENING_SUBJECT = ?;
configurable string PUBLISH_SUBJECT = ?;

@nats:ServiceConfig {
    subject: LISTENING_SUBJECT
}
service "orderProcessorService" on new nats:Listener(nats:DEFAULT_URL) {
    // Creates a NATS client with default configurations
    nats:Client natsClient = checkpanic new(nats:DEFAULT_URL);

    // Listens to NATS subject for any new orders and process them
    remote function onMessage(nats:BytesMessage message) returns error? {

        // Uses Ballerina query expressions to filter out the successful orders and publish to NATS subject
        check from types:Order 'order in check getOrdersFromMessage(message)
        where 'order.status == types:SUCCESS
        do {
             log:printInfo("Sending successful order to " + PUBLISH_SUBJECT + " " + 'order.toString());
             // Publish the order to the NATS subject
             check self.natsClient->publishMessage({
                  content: 'order.toString().toBytes(),
                  subject: PUBLISH_SUBJECT
             });
         };
        return;
    }
}

// Convert the byte values in NATS message to type Order[]
function getOrdersFromMessage(nats:BytesMessage message) returns types:Order[]|error {
    types:Order[] receivedOrders = [];
    string messageContent = check string:fromBytes(message.content);
    json jsonContent = check value:fromJsonString(messageContent);
    json jsonClone = jsonContent.cloneReadOnly();
    types:Order receivedOrder = check jsonClone.ensureType(types:Order);
    receivedOrders.push(receivedOrder);
    return receivedOrders;
}

