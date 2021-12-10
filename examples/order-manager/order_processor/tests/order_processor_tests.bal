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

import ballerina/test;
import order_processor.types;
import ballerinax/nats;
import ballerina/lang.runtime;
import ballerina/lang.value;

isolated string messageReceived = "";

isolated function updateMessageReceived(string message) {
    lock {
        messageReceived = message;
    }
}

isolated function getMessageReceived() returns string {
    lock {
        return messageReceived;
    }
}

@test:Config{}
function orderProcessorTest() returns error? {
    nats:Client testProducer = check new(nats:DEFAULT_URL);

    nats:Listener sub = checkpanic new(nats:DEFAULT_URL);
    checkpanic sub.attach(consumerService);
    checkpanic sub.'start();

    types:Order 'order = {
        id: 1,
        name: "Test Order",
        status: types:SUCCESS
    };
    check natsClient->publishMessage({
                                content: 'order.toString().toBytes(),
                                subject: LISTENING_SUBJECT });
    runtime:sleep(10);

    string messageContent = getMessageReceived();
    json content = check value:fromJsonString(messageContent);
    json jsonTweet = content.cloneReadOnly();
    types:Order neworder = <types:Order> jsonTweet;

    test:assertEquals(neworder, 'order);
    return;
}

nats:Service consumerService =
@nats:ServiceConfig {
    subject: PUBLISH_SUBJECT
}
service object {
    remote function onMessage(nats:Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = string:fromBytes(messageContent);
        if (message is string) {
            updateMessageReceived(message);
        }
    }
};
