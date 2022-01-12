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
const PENDING_LIMITS_SUBJECT = "nats-pending";
const QUEUE_GROUP_SUBJECT = "nats-queues";
const STOP_SUBJECT_NAME = "stopping-subject";
const SERVICE_SUBJECT_NAME = "nats-basic-service";
const SERVICE_SUBJECT_NAME_ANNOT = "nats-basic-service-annot";
const ON_REQUEST_SUBJECT = "nats-on-req";
const ON_REQUEST_TIMEOUT_SUBJECT = "nats-on-req-timeout";
const REPLY_TO_SUBJECT = "nats-rep";
const REPLY_TO_DUMMY = "nats-rep-dummy";
string authToken = "MyToken";
const ISOLATED_SUBJECT_NAME = "nats-isolated";

isolated boolean messageReceived = false;
isolated boolean requestReceived = false;
isolated string receivedQueueMessage = "";
isolated string receivedConsumerMessage = "";
isolated string receivedOnRequestMessage = "";
isolated string withoutAnnotMessage = "";
isolated string receivedReplyMessage = "";

isolated function setReceivedConsumerMessage(string message) {
    lock {
        receivedConsumerMessage = message;
    }
}

isolated function getReceivedConsumerMessage() returns string {
    lock {
        return receivedConsumerMessage;
    }
}

isolated function setReceivedOnRequestMessage(string message) {
    lock {
        receivedOnRequestMessage = message;
    }
}

isolated function getReceivedOnRequestMessage() returns string {
    lock {
        return receivedOnRequestMessage;
    }
}

isolated function setWithoutAnnotMessage(string message) {
    lock {
        withoutAnnotMessage = message;
    }
}

isolated function getWithoutAnnotMessage() returns string {
    lock {
        return withoutAnnotMessage;
    }
}

isolated function setReceivedReplyMessage(string message) {
    lock {
        receivedReplyMessage = message;
    }
}

isolated function getReceivedReplyMessage() returns string {
    lock {
        return receivedReplyMessage;
    }
}

isolated function setReceivedQueueMessage(string message) {
    lock {
        receivedQueueMessage = message;
    }
}

isolated function getReceivedQueueMessage() returns string {
    lock {
        return receivedQueueMessage;
    }
}

isolated function updateMessageReceived(boolean state) {
    lock {
        messageReceived = state;
    }
}

isolated function isMessageReceived() returns boolean {
    lock {
        return messageReceived;
    }
}

isolated function updateRequestReceived(boolean state) {
    lock {
        requestReceived = state;
    }
}

isolated function isRequestReceived() returns boolean {
    lock {
        return requestReceived;
    }
}

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
    if newClient is Client {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Connection creation failed.");
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithToken() {
    Tokens myToken = { token: "MyToken" };
    Client|Error newClient = new("nats://localhost:4223", auth = myToken);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithNoEcho() {
    Client|Error newClient = new(DEFAULT_URL, noEcho = true);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithMultipleServers() {
    Client|Error newClient = new([DEFAULT_URL, DEFAULT_URL]);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithPingConfig() {
    Ping pingConf = { pingInterval: 120, maxPingsOut: 2 };
    Client|Error newClient = new(DEFAULT_URL, ping = pingConf);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithRetryConfig() {
    RetryConfig retryConf = { maxReconnect: 60, reconnectWait: 2, connectionTimeout: 2 };
    Client|Error newClient = new(DEFAULT_URL, retryConfig = retryConf);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testConnectionWithCredentials() {
    Credentials myCredentials = { username: "ballerina", password: "ballerina123" };
    Client|Error newClient = new("nats://localhost:4224", auth = myCredentials);
    if newClient is error {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testCloseConnection() {
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
    if newClient is Client {
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
public isolated function testProducerNegative() {
    Client closeClient = checkpanic new(DEFAULT_URL);
    Error? closeResult = closeClient.close();
    string message = "Hello World";
    Error? result = closeClient->publishMessage({ content: message.toBytes(), subject: SUBJECT_NAME });
    if result is () {
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
    if newClient is Client {
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
    if newClient is Client {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(consumerService);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_SUBJECT_NAME });
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedConsumerMessage() !is "" {
                string receivedMessage = getReceivedConsumerMessage();
                test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        checkpanic sub.detach(consumerService);
        checkpanic sub.gracefulStop();
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testConsumerService1() {
    string message = "Testing Consumer Service without Annotation";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(serviceWithoutAnnotation, SERVICE_SUBJECT_NAME_ANNOT);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: SERVICE_SUBJECT_NAME_ANNOT });
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getWithoutAnnotMessage() !is "" {
                string receivedMessage = getWithoutAnnotMessage();
                test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        checkpanic sub.detach(serviceWithoutAnnotation);
        checkpanic sub.gracefulStop();
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
function testIsolatedConsumerService() returns error? {
    string message = "Testing Isolated Consumer Service";
    Listener sub = checkpanic new(DEFAULT_URL);
    Client newClient = checkpanic new(DEFAULT_URL);
    check sub.attach(isolatedService);
    check sub.'start();
    check newClient->publishMessage({ content: message.toBytes(),
                                                       subject: ISOLATED_SUBJECT_NAME});
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
       if isMessageReceived() {
           break;
       } else {
           runtime:sleep(1);
           timeoutInSeconds = timeoutInSeconds - 1;
       }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 5 minutes.");
    }
    check newClient.close();
    return;
}

@test:Config {
    dependsOn: [testIsolatedConsumerService],
    groups: ["nats-basic"]
}
public function testIsolatedConsumerService2() returns error? {
    string message = "Testing Isolated Consumer Service";
    Client newClient = checkpanic new(DEFAULT_URL);
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(isolatedRequestService);
    checkpanic sub.'start();
    checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ISOLATED_SUBJECT_NAME,
                                                replyTo: REPLY_TO_DUMMY });
    int timeoutInSeconds = 300;
    // Test fails in 5 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
       if isRequestReceived() {
           break;
       } else {
           runtime:sleep(1);
           timeoutInSeconds = timeoutInSeconds - 1;
       }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 5 minutes.");
    }
    check newClient.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testConsumerService2() {
    Listener sub = checkpanic new(DEFAULT_URL);
    error? result = trap sub.attach(serviceWithoutAnnotation);
    if result is () {
        test:assertFail("Expected error in attaching a service without a service name.");
    } else {
        string expected = "Subject name cannot be found";
        test:assertEquals(result.message(), expected, msg = "Error message mismatch");
    }
    checkpanic sub.gracefulStop();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testConsumerServiceWithQueue() {
    string message = "Testing Consumer Service with Queues";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(queueService);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: QUEUE_GROUP_SUBJECT });
        int timeoutInSeconds = 300;
        // Test fails in 5 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedQueueMessage() !is "" {
                string receivedMessage = getReceivedQueueMessage();
                test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        checkpanic sub.detach(queueService);
        checkpanic sub.gracefulStop();
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 5 minutes.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testServiceWithoutConfig() {
    Listener sub = checkpanic new(DEFAULT_URL);
    error? result = trap sub.attach(noConfigService);
    if result !is error {
        test:assertFail("testServiceWithoutConfig failed.");
    }
    checkpanic sub.gracefulStop();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testImmediateStop() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(stopService);
    checkpanic sub.'start();
    checkpanic sub.detach(stopService);
    error? stopResult = sub.immediateStop();
    if stopResult is error {
        test:assertFail("Stopping listener immediately failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testDetach1() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(dummyService1);
    checkpanic sub.attach(dummyService2);
    checkpanic sub.'start();
    checkpanic sub.detach(dummyService1);
    checkpanic sub.detach(dummyService2);
    error? stopResult = sub.immediateStop();
    if stopResult is error {
        test:assertFail("Stopping listener with multiple services immediately failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testDetach2() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(dummyService1);
    checkpanic sub.attach(dummyService2);
    checkpanic sub.'start();
    checkpanic sub.detach(dummyService1);
    checkpanic sub.detach(dummyService2);
    error? stopResult = sub.gracefulStop();
    if stopResult is error {
        test:assertFail("Stopping listener with multiple services immediately failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testGracefulStop() {
    Listener sub = checkpanic new(DEFAULT_URL);
    checkpanic sub.attach(stopService);
    checkpanic sub.'start();
    checkpanic sub.detach(stopService);
    error? stopResult = sub.gracefulStop();
    if stopResult is error {
        test:assertFail("Stopping listener gracefully failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testPendingLimits() {
    Listener sub = checkpanic new(DEFAULT_URL);
    error? pendingResult = sub.attach(pendingLimitsService);
    if pendingResult is error {
        test:assertFail("Attaching service with pending limits failed.");
    }
    checkpanic sub.'start();
    checkpanic sub.detach(pendingLimitsService);
    checkpanic sub.gracefulStop();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testOnRequest1() {
    string message = "Hello from the other side!";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(onRequestService);
        checkpanic sub.attach(onReplyService);
        checkpanic sub.'start();
        checkpanic newClient->publishMessage({ content: message.toBytes(), subject: ON_REQUEST_SUBJECT,
                                                    replyTo: REPLY_TO_SUBJECT });
        int timeoutInSeconds = 300;
        // Test fails in 5 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedOnRequestMessage() !is "" && getReceivedReplyMessage() !is "" {
                string receivedRequestMessage = getReceivedOnRequestMessage();
                string receivedReplyMessage = getReceivedReplyMessage();
                test:assertEquals(receivedRequestMessage, message, msg = "Message received does not match.");
                test:assertEquals(receivedReplyMessage, "Hello Back!", msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 5 minutes.");
        }
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
    if newClient is Client {
        Listener sub = checkpanic new(DEFAULT_URL);
        checkpanic sub.attach(onRequestService);
        checkpanic sub.'start();
        Message replyMessage =
            checkpanic newClient->requestMessage({ content: message.toBytes(), subject: ON_REQUEST_SUBJECT});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedOnRequestMessage() !is "" {
                string receivedMessage = getReceivedOnRequestMessage();
                test:assertEquals(receivedMessage, message, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }

        byte[] messageContent = <@untainted> replyMessage.content;
        string|error messageTxt = strings:fromBytes(messageContent);
        if messageTxt is string {
            test:assertEquals(messageTxt, "Hello Back!", msg = "Message received does not match.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public isolated function testRequestMessage1() {
    string message = "Hello, you won't here me!";
    Client reqClient = checkpanic new(DEFAULT_URL);
    Message|Error replyMessage =
             reqClient->requestMessage({ content: message.toBytes(), subject: ON_REQUEST_TIMEOUT_SUBJECT}, 2);
    if replyMessage is error {
        string errorMessage = "Request to subject nats-on-req-timeout timed out while waiting for a reply";
        test:assertEquals(replyMessage.message(), errorMessage, msg = "Error message mismatch.");
    } else {
        test:assertFail("Expected request timeout.");
    }
    checkpanic reqClient.close();
}

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public isolated function testRequestMessage2() {
    string message = "Hello, you won't here me!";
    Client reqClient = checkpanic new(DEFAULT_URL);
    checkpanic reqClient.close();
    Message|Error replyMessage =
             reqClient->requestMessage({ content: message.toBytes(), subject: ON_REQUEST_TIMEOUT_SUBJECT}, 5);
    if replyMessage is error {
        string errorMessage = "Error while requesting message to subject nats-on-req-timeout. Connection is Closed";
        test:assertEquals(replyMessage.message(), errorMessage, msg = "Error message mismatch.");
    } else {
        test:assertFail("Expected illegal state error.");
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
        if message is string {
            setReceivedConsumerMessage(message);
            log:printInfo("Message Received: " + message);
        }
    }
};

Service serviceWithoutAnnotation =
service object {
    remote function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            setWithoutAnnotMessage(message);
            log:printInfo("Message Received: " + message);
        }
    }
};

Service onRequestService =
@ServiceConfig {
    subject: ON_REQUEST_SUBJECT
}
service object {
    isolated remote function onMessage(Message msg) {
       // ignored
    }

    remote function onRequest(Message msg) returns string {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            setReceivedOnRequestMessage(message);
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
        if message is string {
            setReceivedReplyMessage(message);
            log:printInfo("Message Received: " + message);
        }
    }
};

Service stopService =
@ServiceConfig {
    subject: STOP_SUBJECT_NAME
}
service object {
    isolated remote function onMessage(Message msg) {
    }
};

Service dummyService1 =
@ServiceConfig {
    subject: STOP_SUBJECT_NAME
}
service object {
    isolated remote function onMessage(Message msg) {
    }
};

Service dummyService2 =
@ServiceConfig {
    subject: STOP_SUBJECT_NAME
}
service object {
    isolated remote function onMessage(Message msg) {
    }
};

Service pendingLimitsService =
@ServiceConfig {
    subject: PENDING_LIMITS_SUBJECT,
    pendingLimits: {
        maxMessages: 10,
        maxBytes: 5 * 1024
    }
}
service object {
    isolated remote function onMessage(Message msg) {
    }
};

Service queueService =
@ServiceConfig {
    subject: QUEUE_GROUP_SUBJECT,
    queueName: "queue-group-1"
}
service object {
    remote function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            setReceivedQueueMessage(message);
            log:printInfo("Message Received for queue group: " + message);
        }
    }
};

Service noConfigService =
service object {
    isolated remote function onMessage(Message msg) {
    }
};

Service isolatedService =
@ServiceConfig {
    subject: ISOLATED_SUBJECT_NAME
}
service object {
    remote function onMessage(Message msg) {
        byte[] messageContent = <@untainted> msg.content;
        string|error message = 'string:fromBytes(messageContent);
        if message is string {
            updateMessageReceived(true);
        }
    }
};

Service isolatedRequestService =
@ServiceConfig {
    subject: ISOLATED_SUBJECT_NAME
}
service object {
    isolated remote function onMessage(Message msg) {
       // ignored
    }

    remote function onRequest(Message msg) returns string {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if message is string {
            updateRequestReceived(true);
        }
        return "Hello Back!";
    }
};
