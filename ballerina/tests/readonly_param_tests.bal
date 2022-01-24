// Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

isolated string receivedReadOnlyMessage = "";
isolated string receivedReadOnlyOnRequestMessage = "";

const READONLY_PARAM_SUBJECT_NAME = "basic-service-readonly";
const READONLY_PARAM_ON_REQUEST = "on-req-readonly";
const READONLY_REPLY_TO_SUBJECT = "nats-rep-readonly";

isolated function setReceivedReadOnlyOnRequestMessage(string message) {
    lock {
        receivedReadOnlyOnRequestMessage = message;
    }
}

isolated function getReceivedReadOnlyOnRequestMessage() returns string {
    lock {
        return receivedReadOnlyOnRequestMessage;
    }
}

isolated function setReceivedReadOnlyMessage(string message) {
    lock {
        receivedReadOnlyMessage = message;
    }
}

isolated function getReceivedReadOnlyMessage() returns string {
    lock {
        return receivedReadOnlyMessage;
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testConsumerServiceWithReadOnlyParams() {
    string message = "Testing Consumer Service with Readonly params";
    Client newClient = checkpanic new(DEFAULT_URL);
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(consumerServiceWithReadOnlyParams);
    checkpanic sub.'start();
    checkpanic newClient->publishMessage({ content: message.toBytes(), subject: READONLY_PARAM_SUBJECT_NAME });
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedReadOnlyMessage() !is "" {
            string receivedMessage = getReceivedReadOnlyMessage();
            test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    checkpanic sub.detach(consumerServiceWithReadOnlyParams);
    checkpanic sub.gracefulStop();
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 5 minutes.");
    }
    checkpanic newClient.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testOnRequestWithReadOnlyParams() {
    string message = "Hello from the other side But ReadOnly!";
    Client newClient = checkpanic new(DEFAULT_URL);
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(onRequestReadOnlyService);
    checkpanic sub.'start();
    checkpanic newClient->publishMessage({ content: message.toBytes(), subject: READONLY_PARAM_ON_REQUEST,
                                                replyTo: READONLY_REPLY_TO_SUBJECT });
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedReadOnlyOnRequestMessage() !is "" {
            string receivedMessage = getReceivedReadOnlyOnRequestMessage();
            test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    checkpanic sub.detach(onRequestReadOnlyService);
    checkpanic sub.gracefulStop();
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 5 minutes.");
    }
    checkpanic newClient.close();
}

Service consumerServiceWithReadOnlyParams =
@ServiceConfig {
    subject: READONLY_PARAM_SUBJECT_NAME
}
service object {
    remote function onMessage(readonly & Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            setReceivedReadOnlyMessage(message);
            log:printInfo("Message Received: " + message);
        }
    }
};

Service onRequestReadOnlyService =
@ServiceConfig {
    subject: READONLY_PARAM_ON_REQUEST
}
service object {
    isolated remote function onMessage(Message msg) {
        // ignored
    }

    remote function onRequest(readonly & Message msg) returns string {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            setReceivedReadOnlyOnRequestMessage(message);
            log:printInfo("Message Received: " + message);
        }
        return "Hello Back!";
    }
};
