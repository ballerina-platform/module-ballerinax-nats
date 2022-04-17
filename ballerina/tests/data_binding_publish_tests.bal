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
import ballerina/test;
import ballerina/log;

const DATA_BINDING_PUBLISH_SUBJECT = "bind.publish";
string receivedStringValuePublish = "";
string receivedBytesValuePublish = "";

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testDataBindingStringPublish() {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceStringPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedStringValuePublish !is "" {
                    string receivedMessage = receivedStringValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceStringPublish);
            if timeoutInSeconds == 0 {
                test:assertFail("Failed to receive the message for 2 minutes.");
            }
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceStringPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(StringMessage msg) {
        receivedStringValuePublish = msg.content;
        log:printInfo("Message Received: " + receivedStringValuePublish);
    }
};

@test:Config {
    dependsOn: [testDataBindingStringPublish],
    groups: ["nats-basic"]
}
public function testDataBindingBytesPublish() {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceBytesPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend.toBytes(), subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedBytesValuePublish !is "" {
                    string receivedMessage = receivedBytesValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceBytesPublish);
            if timeoutInSeconds == 0 {
                test:assertFail("Failed to receive the message for 2 minutes.");
            }
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceBytesPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(BytesMessage msg) {
        byte[] messageContent = <@untainted> msg.content;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            receivedBytesValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};
