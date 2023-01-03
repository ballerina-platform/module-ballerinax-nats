// Copyright (c) 2022, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
//
// WSO2 LLC. licenses this file to you under the Apache License,
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


isolated string receivedAsyncMessage = "";
isolated string receivedNakMessage = "";
isolated string receivedInProgressMessage = "";

isolated function setReceivedAsyncMessage(string message) {
    lock {
        receivedAsyncMessage = message;
    }
}

isolated function getReceivedAsyncMessage() returns string {
    lock {
        return receivedAsyncMessage;
    }
}

isolated function setReceivedNakMessage(string message) {
    lock {
        receivedNakMessage = message;
    }
}

isolated function getReceivedNakMessage() returns string {
    lock {
        return receivedNakMessage;
    }
}

isolated function setReceivedInProgressMessage(string message) {
    lock {
        receivedInProgressMessage = message;
    }
}

isolated function getReceivedInProgressMessage() returns string {
    lock {
        return receivedInProgressMessage;
    }
}

@test:Config {
    dependsOn: [testJetStreamSyncConsumer, testJetStreamClientManagement],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub() returns error? {
    string SUBJECT_NAME = "js.pubsub1";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jspubsub1",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onStreamService);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedAsyncMessage() !is "" {
                string receivedMessage = getReceivedAsyncMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        check jetStreamLis.detach(onStreamService);
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    dependsOn: [testJetStreamSyncConsumer, testJetStreamClientManagement],
    groups: ["nats-js"]
}
public function testJetStreamClientAcks() returns error? {
    string SUBJECT_NAME = "js.acks1";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jsacks1",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onNakService);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedNakMessage() !is "" {
                string receivedMessage = getReceivedNakMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        check jetStreamLis.detach(onNakService);
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    dependsOn: [testJetStreamSyncConsumer, testJetStreamClientManagement],
    groups: ["nats-js"]
}
public function testJetStreamClientAcks2() returns error? {
    string SUBJECT_NAME = "js.acks2";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jsacks2",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onInProgressService);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedInProgressMessage() !is "" {
                string receivedMessage = getReceivedInProgressMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        check jetStreamLis.detach(onInProgressService);
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    dependsOn: [testJetStreamClientPubSub],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub2() returns error? {
    string SUBJECT_NAME = "js.pubsub2";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jspubsub2",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onStreamService2, SUBJECT_NAME);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedAsyncMessage() !is "" {
                string receivedMessage = getReceivedAsyncMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    dependsOn: [testJetStreamClientPubSub2],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub3() returns error? {
    string SUBJECT_NAME = "js.pubsub3";
    Client natsClient = check new(JS_URL);
    StreamConfiguration config = { name: "jspubsub3",
                                   subjects: [SUBJECT_NAME],
                                   storageType:  MEMORY};
    JetStreamClient jetStreamClient = check new(natsClient);
    _ = check jetStreamClient->addStream(config);
    JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
    check jetStreamLis.attach(onStreamService3, SUBJECT_NAME);
    check jetStreamLis.'start();
    check jetStreamLis.detach(onStreamService3);
    check jetStreamLis.gracefulStop();
}

@test:Config {
    dependsOn: [testJetStreamClientPubSub3],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub4() returns error? {
    string SUBJECT_NAME = "js.pubsub4";
    Client natsClient = check new(JS_URL);
    StreamConfiguration config = { name: "jspubsub4",
                                   subjects: [SUBJECT_NAME],
                                   storageType:  MEMORY};
    JetStreamClient jetStreamClient = check new(natsClient);
    _ = check jetStreamClient->addStream(config);
    JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
    check jetStreamLis.attach(onStreamService3, SUBJECT_NAME);
    check jetStreamLis.'start();
    check jetStreamLis.detach(onStreamService3);
    check jetStreamLis.immediateStop();
}

@test:Config {
    dependsOn: [testJetStreamClientPubSub4],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub5() returns error? {
    string SUBJECT_NAME = "js.pubsub5";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jspubsub5",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onStreamService5);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedAsyncMessage() !is "" {
                string receivedMessage = getReceivedAsyncMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    dependsOn: [testJetStreamClientPubSub5],
    groups: ["nats-js"]
}
public function testJetStreamClientPubSub6() returns error? {
    string SUBJECT_NAME = "js.pubsub6";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jspubsub6",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        JetStreamListener jetStreamLis = check new JetStreamListener(natsClient);
        check jetStreamLis.attach(onStreamService6, SUBJECT_NAME);
        check jetStreamLis.'start();
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        int timeoutInSeconds = 120;
        // Test fails in 2 minutes if it is failed to receive the message
        while timeoutInSeconds > 0 {
            if getReceivedAsyncMessage() !is "" {
                string receivedMessage = getReceivedAsyncMessage();
                test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                break;
            } else {
                runtime:sleep(1);
                timeoutInSeconds = timeoutInSeconds - 1;
            }
        }
        if timeoutInSeconds == 0 {
            test:assertFail("Failed to receive the message for 2 minutes.");
        }
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamClientInit() returns error? {
    string SUBJECT_NAME = "js.init";
    Client natsClient = check new(JS_URL);
    StreamConfiguration config = { name: "jsinit",
                                   subjects: [SUBJECT_NAME],
                                   storageType:  MEMORY};
    check natsClient.close();
    JetStreamClient|Error jetStreamClient = new(natsClient);
    if jetStreamClient is JetStreamClient {
        test:assertFail("Error expected creating client with closed connection.");
    } else {
        string message = jetStreamClient.message();
        string expectedError = "Error occurred while initializing the JetStreamClient. A JetStream context can't be established during close.";
        test:assertEquals(message, expectedError, msg = "Error message received does not match.");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamSyncConsumer() returns error? {
    string SUBJECT_NAME = "js.sync1";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jssync1",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  FILE};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        JetStreamMessage message = check jetStreamClient->consumeMessage(SUBJECT_NAME, 0.5);
        string messageString = check string:fromBytes(message.content);
        test:assertEquals(messageString, messageToSend, msg = "Message received does not match.");
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamSyncConsumer2() returns error? {
    string SUBJECT_NAME = "js.sync2";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jssync2",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        JetStreamMessage message = check jetStreamClient->consumeMessage(SUBJECT_NAME, 0.5);
        string messageString = check string:fromBytes(message.content);
        test:assertEquals(messageString, messageToSend, msg = "Message received does not match.");
        jetStreamClient->ack(message);
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamSyncConsumer3() returns error? {
    string SUBJECT_NAME = "js.sync3";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jssync3",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        JetStreamMessage message = check jetStreamClient->consumeMessage(SUBJECT_NAME, 0.5);
        string messageString = check string:fromBytes(message.content);
        test:assertEquals(messageString, messageToSend, msg = "Message received does not match.");
        jetStreamClient->nak(message);
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamSyncConsumer4() returns error? {
    string SUBJECT_NAME = "js.sync4";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
    if natsClient is Client {
        StreamConfiguration config = { name: "jssync4",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        JetStreamMessage message = check jetStreamClient->consumeMessage(SUBJECT_NAME, 0.5);
        string messageString = check string:fromBytes(message.content);
        test:assertEquals(messageString, messageToSend, msg = "Message received does not match.");
        jetStreamClient->inProgress(message);
    } else {
        test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamClientManagement() returns error? {
    string SUBJECT_NAME = "js.manage.test2";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
        if natsClient is Client {
        StreamConfiguration config = { name: "jsmanagetest2",
                                       subjects: [SUBJECT_NAME],
                                       storageType:  MEMORY};
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        _ = check jetStreamClient->updateStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        _ = check jetStreamClient->purgeStream("jsmanagetest2");
        _ = check jetStreamClient->deleteStream("jsmanagetest2");
    } else {
          test:assertFail("NATS connection creation failed");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamClientManagement2() returns error? {
    string SUBJECT_NAME = "js.manage.test3";
    string messageToSend = "Hello World, from Ballerina";
    Client jsClient = check new("nats://localhost:4226");
    StreamConfiguration config = { name: "jsmanagetest3",
                                   subjects: [SUBJECT_NAME],
                                   storageType:  MEMORY};
    JetStreamClient jetStreamClient = check new(jsClient);
    check jsClient.close();
    Error? result = jetStreamClient->addStream(config);
    if result is () {
        test:assertFail("Error expected with the closed connection");
    } else {
        string message = result.message();
        string expectedError = "Error occurred while adding the stream. Connection is Closed";
        test:assertEquals(message, expectedError, msg = "Error message received does not match.");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamClientManagement3() returns error? {
    string SUBJECT_NAME = "js.manage.test4";
    string messageToSend = "Hello World, from Ballerina";
    Client jsClient = check new("nats://localhost:4226");
    StreamConfiguration config = { name: "jsmanagetest4",
                                   subjects: [SUBJECT_NAME],
                                   storageType:  MEMORY};
    JetStreamClient jetStreamClient = check new(jsClient);
    check jsClient.close();
    Error? result = jetStreamClient->updateStream(config);
    if result is () {
        test:assertFail("Error expected with the closed connection");
    } else {
        string message = result.message();
        string expectedError = "Error occurred while updating the stream. Connection is Closed";
        test:assertEquals(message, expectedError, msg = "Error message received does not match.");
    }
}

@test:Config {
    groups: ["nats-js"]
}
public function testJetStreamClientManagement4() returns error? {
    string SUBJECT_NAME = "js.manage.test5";
    string messageToSend = "Hello World, from Ballerina";
    Client? natsClient = jsClient;
        if natsClient is Client {
        StreamConfiguration config = {  name: "jsmanagetest5",
                                        description: "Stream to test the management functions.",
                                        subjects: [SUBJECT_NAME],
                                        retentionPolicy: LIMITS,
                                        maxConsumers: 4,
                                        maxMsgs: 1000,
                                        maxMsgsPerSubject: 100,
                                        maxBytes: 1024,
                                        maxAge: 10,
                                        maxMsgSize: 100,
                                        replicas: 1,
                                        noAck: false,
                                        discardPolicy: OLD,
                                        storageType:  MEMORY
                                       };
        JetStreamClient jetStreamClient = check new(natsClient);
        _ = check jetStreamClient->addStream(config);
        _ = check jetStreamClient->updateStream(config);
        check jetStreamClient->publishMessage({subject: SUBJECT_NAME, content: messageToSend.toBytes()});
        _ = check jetStreamClient->purgeStream("jsmanagetest5");
        _ = check jetStreamClient->deleteStream("jsmanagetest5");
    } else {
          test:assertFail("NATS connection creation failed");
    }
}

JetStreamService onStreamService =
@StreamServiceConfig {
    subject: "js.pubsub1",
    autoAck: false
}
service object {
    remote function onMessage(JetStreamMessage msg, JetStreamCaller caller) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedAsyncMessage(message);
        }
        caller->ack();
    }
};

JetStreamService onNakService =
@StreamServiceConfig {
    subject: "js.acks1",
    autoAck: false
}
service object {
    remote function onMessage(JetStreamMessage msg, JetStreamCaller caller) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedNakMessage(message);
        }
        caller->nak();
    }
};

JetStreamService onInProgressService =
@StreamServiceConfig {
    subject: "js.acks2",
    autoAck: false
}
service object {
    remote function onMessage(JetStreamMessage msg, JetStreamCaller caller) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedInProgressMessage(message);
        }
        caller->inProgress();
    }
};

JetStreamService onStreamService2 =
service object {
    remote function onMessage(JetStreamMessage msg, JetStreamCaller caller) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedAsyncMessage(message);
        }
        caller->ack();
    }
};

JetStreamService onStreamService3 =
@StreamServiceConfig {
    subject: "js.pubsub3",
    autoAck: false
}
service object {
    remote function onMessage(JetStreamMessage msg, JetStreamCaller caller) {
        caller->ack();
    }
};

JetStreamService onStreamService5 =
@StreamServiceConfig {
    subject: "js.pubsub5"
}
service object {
    remote function onMessage(readonly & JetStreamMessage msg) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedAsyncMessage(message);
        }
    }
};

JetStreamService onStreamService6 =
@StreamServiceConfig {
    subject: "js.pubsub6",
    autoAck: false
}
service object {
    remote function onMessage(readonly & JetStreamMessage msg, JetStreamCaller caller) {
        byte[] messageContent = msg.content;

        string|error message = string:fromBytes(messageContent);
        if message is string {
            setReceivedAsyncMessage(message);
        }
        caller->ack();
    }
};

