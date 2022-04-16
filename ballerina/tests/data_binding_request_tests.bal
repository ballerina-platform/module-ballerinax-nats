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

import ballerina/lang.runtime as runtime;
import ballerina/test;
import ballerina/log;

const DATA_BINDING_REQUEST_SUBJECT = "bind.request";
string receivedStringValueRequest = "";

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testDataBindingStringRequest() {
    string messageToReceive = "consumerServiceStringRequest received message";
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceStringRequest);
            checkpanic sub.'start();
            StringMessage dataBoundMessage = checkpanic newClient->requestMessage({ content: messageToSend,
                            subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedStringValueRequest !is "" {
                    string receivedMessage = receivedStringValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }

            test:assertEquals(dataBoundMessage.content, messageToReceive, msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceStringRequest);
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

Service consumerServiceStringRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(StringMessage msg) returns string {
        receivedStringValueRequest = msg.content;
        log:printInfo("Message Received: " + receivedStringValueRequest);
        return "consumerServiceStringRequest received message";
    }
};
