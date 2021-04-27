// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/lang.'string as strings;
import ballerina/lang.runtime as runtime;
import ballerina/log;
import ballerina/test;

Client? clientObj = ();
const SUBJECT_NAME = "nats-basic";
const SERVICE_SUBJECT_NAME = "nats-basic-service";
const ON_REQUEST_SUBJECT = "nats-on-req";
const REPLY_TO_SUBJECT = "nats-rep";
string receivedConsumerMessage = "";
string receivedOnRequestMessage = "";
string receivedReplyMessage = "";

@test:BeforeSuite
function setup() {
    Client newClient = checkpanic new(DEFAULT_URL);
    clientObj = newClient;
}

@test:Config {
    groups: ["nats-basic"]
}
public function testConnection() {
    boolean flag = false;
    Client? newClient = clientObj;
    if (newClient is Client) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Connection creation failed.");
}

@test:Config {
    groups: ["nats-basic"]
}
public function testCloseConnection() {
    Client closeClient = checkpanic new(DEFAULT_URL);
    Error? closeResult = closeClient.close();
    test:assertEquals(closeResult, (), msg = "NATS Connection closing failed.");
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-basic"]
}
public function testProducer() {
    Client? newClient = clientObj;
    string message = "Hello World";
    if (newClient is Client) {
        Error? result = newClient->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME });
        test:assertEquals(result, (), msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-basic"]
}
public function testProducerNegative() {
    Client closeClient = checkpanic new(DEFAULT_URL);
    Error? closeResult = closeClient.close();
    string message = "Hello World";
    Error? result = closeClient->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME });
    if (result is ()) {
        test:assertFail("Error expected for publishing with closed client.");
    }
}

@test:Config {
    dependsOn: [testConnection],
    groups: ["nats-basic"]
}
public function testProducerWithReplyTo() {
    Client? newClient = clientObj;
    string message = "Hello World";
    if (newClient is Client) {
        Error? result = newClient->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME,
                                            replyTo: "replyToTest"});
        test:assertEquals(result, (), msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testConsumerService() {
    string message = "Testing Consumer Service";
    Client? newClient = clientObj;
    if (newClient is Client) {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(consumerService);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_SUBJECT_NAME });
        runtime:sleep(7);
        test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
        checkpanic sub.detach(consumerService);
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testOnRequest1() {
    string message = "Hello from the other side!";
    Client? newClient = clientObj;
    if (newClient is Client) {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(onRequestService);
        checkpanic sub.attach(onReplyService);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ON_REQUEST_SUBJECT,
                                                    replyTo: REPLY_TO_SUBJECT });
        runtime:sleep(10);
        test:assertEquals(receivedOnRequestMessage, message, msg = "Message received does not match.");
        test:assertEquals(receivedReplyMessage, "Hello Back!", msg = "Message received does not match.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testOnRequest1],
    groups: ["nats-basic"]
}
public function testOnRequest2() {
    string message = "Hey There Delilah!";
    Client? newClient = clientObj;
    if (newClient is Client) {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(onRequestService);
        checkpanic sub.'start();
        Message replyMessage =
            checkpanic newClient->requestMessage({ content: message.toBytes(), subject: ON_REQUEST_SUBJECT});
        runtime:sleep(10);
        test:assertEquals(receivedOnRequestMessage, message, msg = "Message received does not match.");

        byte[] messageContent = <@untainted> replyMessage.content;
        string|error messageTxt = strings:fromBytes(messageContent);
        if (messageTxt is string) {
            test:assertEquals(messageTxt, "Hello Back!", msg = "Message received does not match.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerService =
@ServiceConfig {
    subject: SERVICE_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if (message is string) {
            receivedConsumerMessage = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

Service onRequestService =
@ServiceConfig {
    subject: ON_REQUEST_SUBJECT
}
service object {
    remote function onMessage(Message msg) {
       // ignored
    }

    remote function onRequest(Message msg) returns string {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if (message is string) {
            receivedOnRequestMessage = message;
            log:printInfo("Message Received: " + message);
        }
        return "Hello Back!";
    }
};

Service onReplyService =
@ServiceConfig {
    subject: REPLY_TO_SUBJECT
}
service object {
    remote function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if (message is string) {
            receivedReplyMessage = message;
            log:printInfo("Message Received: " + message);
        }
    }
};
