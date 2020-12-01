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
import ballerina/log;
import ballerina/runtime;
import ballerina/test;

Client? clientObj = ();
const SUBJECT_NAME = "nats-basic";
const SERVICE_SUBJECT_NAME = "nats-basic-service";
string receivedConsumerMessage = "";

@test:BeforeSuite
function setup() {
    log:printInfo("Creating a ballerina NATS connection.");
    Client newClient = new;
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
    dependsOn: ["testConnection"],
    groups: ["nats-basic"]
}
public function testProducer() {
    Client? newClient = clientObj;
    string message = "Hello World";
    if (newClient is Client) {
        Error? result = newClient->publish(SUBJECT_NAME, message.toBytes());
        test:assertEquals(result, (), msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: ["testProducer"],
    groups: ["nats-basic"]
}
public function testConsumerService() {
    string message = "Testing Consumer Service";
    Client? newClient = clientObj;
    if (newClient is Client) {
        Listener sub = new;
        checkpanic sub.__attach(consumerService);
        checkpanic sub.__start();
        checkpanic newClient->publish(SERVICE_SUBJECT_NAME, message.toBytes());
        runtime:sleep(5000);
        test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

service consumerService =
@ServiceConfig {
    subject: SERVICE_SUBJECT_NAME
}
service {
    resource function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if (message is string) {
            receivedConsumerMessage = message;
            log:printInfo("Message Received: " + message);
        }
    }

    resource function onError(Message msg, Error err) {
    }
};
