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
import ballerinax/nats;

isolated string receivedStringPayloadValuePublish = "";
isolated string receivedBytesPayloadValuePublish = "";
isolated string receivedXmlPayloadValuePublish = "";
isolated json receivedJsonPayloadValuePublish = ();
isolated int receivedIntPayloadValuePublish = 0;
isolated float receivedFloatPayloadValuePublish = 0.0;
isolated decimal receivedDecimalPayloadValuePublish = 0d;
isolated boolean receivedBooleanPayloadValuePublish = false;
isolated Person? receivedPersonPayloadValuePublish = ();
isolated map<Person>? receivedMapPayloadValuePublish = ();
isolated table<Person>? receivedTablePayloadValuePublish = ();
isolated int|string? receivedUnionPayloadValuePublish = ();
isolated RandomPayload? receivedRandomPayloadValuePublish = ();

isolated function setReceivedStringPayloadValuePublish(string message) {
    lock {
        receivedStringPayloadValuePublish = message;
    }
}

isolated function getReceivedStringPayloadValuePublish() returns string {
    lock {
        return receivedStringPayloadValuePublish;
    }
}

isolated function setReceivedBytesPayloadValuePublish(string message) {
    lock {
        receivedBytesPayloadValuePublish = message;
    }
}

isolated function getReceivedBytesPayloadValuePublish() returns string {
    lock {
        return receivedBytesPayloadValuePublish;
    }
}

isolated function setReceivedXmlPayloadValuePublish(string message) {
    lock {
        receivedXmlPayloadValuePublish = message;
    }
}

isolated function getReceivedXmlPayloadValuePublish() returns string {
    lock {
        return receivedXmlPayloadValuePublish;
    }
}

isolated function setReceivedJsonPayloadValuePublish(json message) {
    lock {
        receivedJsonPayloadValuePublish = message.clone();
    }
}

isolated function getReceivedJsonPayloadValuePublish() returns json {
    lock {
        return receivedJsonPayloadValuePublish.clone();
    }
}

isolated function setReceivedIntPayloadValuePublish(int message) {
    lock {
        receivedIntPayloadValuePublish = message;
    }
}

isolated function getReceivedIntPayloadValuePublish() returns int {
    lock {
        return receivedIntPayloadValuePublish;
    }
}

isolated function setReceivedFloatPayloadValuePublish(float message) {
    lock {
        receivedFloatPayloadValuePublish = message;
    }
}

isolated function getReceivedFloatPayloadValuePublish() returns float {
    lock {
        return receivedFloatPayloadValuePublish;
    }
}

isolated function setReceivedDecimalPayloadValuePublish(decimal message) {
    lock {
        receivedDecimalPayloadValuePublish = message;
    }
}

isolated function getReceivedDecimalPayloadValuePublish() returns decimal {
    lock {
        return receivedDecimalPayloadValuePublish;
    }
}

isolated function setReceivedBooleanPayloadValuePublish(boolean message) {
    lock {
        receivedBooleanPayloadValuePublish = message;
    }
}

isolated function getReceivedBooleanPayloadValuePublish() returns boolean {
    lock {
        return receivedBooleanPayloadValuePublish;
    }
}

isolated function setReceivedPersonPayloadValuePublish(Person? message) {
    lock {
        receivedPersonPayloadValuePublish = message.clone();
    }
}

isolated function getReceivedPersonPayloadValuePublish() returns Person? {
    lock {
        return receivedPersonPayloadValuePublish.clone();
    }
}

isolated function setReceivedMapPayloadValuePublish(map<Person> message) {
    lock {
        receivedMapPayloadValuePublish = message.clone();
    }
}

isolated function getReceivedMapPayloadValuePublish() returns map<Person>? {
    lock {
        return receivedMapPayloadValuePublish.clone();
    }
}

isolated function setReceivedTablePayloadValuePublish(table<Person> message) {
    lock {
        receivedTablePayloadValuePublish = message.clone();
    }
}

isolated function getReceivedTablePayloadValuePublish() returns table<Person>? {
    lock {
        return receivedTablePayloadValuePublish.clone();
    }
}

isolated function setReceivedUnionPayloadValuePublish(int|string message) {
    lock {
        receivedUnionPayloadValuePublish = message.clone();
    }
}

isolated function getReceivedUnionPayloadValuePublish() returns int|string? {
    lock {
        return receivedUnionPayloadValuePublish.clone();
    }
}

isolated function setReceivedRandomPayloadValuePublish(RandomPayload message) {
    lock {
        receivedRandomPayloadValuePublish = message.clone();
    }
}

isolated function getReceivedRandomPayloadValuePublish() returns RandomPayload? {
    lock {
        return receivedRandomPayloadValuePublish.clone();
    }
}

public type RandomPayload record {|
    string content;
    string subject;
    string replyTo?;
|};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingStringPayloadPublish() returns error? {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataStringPayload" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedStringPayloadValuePublish() !is "" {
            string receivedMsg = getReceivedStringPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataStringPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(string payload) {
        setReceivedStringPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload);
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBytesPayloadPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend.toBytes(), subject: "dataBytesPayload" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedBytesPayloadValuePublish() !is "" {
            string receivedMsg = getReceivedBytesPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataBytesPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(byte[] payload) {
        byte[] messageContent = payload;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            setReceivedBytesPayloadValuePublish(message);
            log:printInfo("Message Received: " + message);
        }
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingXmlPayloadPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataXmlPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedXmlPayloadValuePublish() !is "" {
            string receivedMsg = getReceivedXmlPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend.toString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataXmlPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(xml payload) {
        string|error message = payload.toString();
        if message is string {
            setReceivedXmlPayloadValuePublish(message);
            log:printInfo("Message Received: " + message);
        }
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingJsonPayloadPublish() returns error? {
    json messageToSend = jsonData;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataJsonPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedJsonPayloadValuePublish() !is () {
            json receivedMsg = getReceivedJsonPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataJsonPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(json payload) {
        setReceivedJsonPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingIntPayloadPublish() returns error? {
    int messageToSend = 521;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataIntPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedIntPayloadValuePublish() !is 0 {
            int receivedMsg = getReceivedIntPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataIntPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(int payload) {
        setReceivedIntPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingFloatPayloadPublish() returns error? {
    float messageToSend = 1995.52;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataFloatPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedFloatPayloadValuePublish() !is 0.0 {
            float receivedMsg = getReceivedFloatPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataFloatPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(float payload) {
        setReceivedFloatPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingDecimalPayloadPublish() returns error? {
    decimal messageToSend = 1995.52d;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataDecimalPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedDecimalPayloadValuePublish() !is 0d {
            decimal receivedMsg = getReceivedDecimalPayloadValuePublish();
            test:assertEquals(receivedMsg, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataDecimalPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(decimal payload) {
        setReceivedDecimalPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBooleanPayloadPublish() returns error? {
    boolean messageToSend = true;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataBooleanPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedBooleanPayloadValuePublish() !is false {
            boolean receivedMsg = getReceivedBooleanPayloadValuePublish();
            test:assertTrue(receivedMsg, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataBooleanPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(boolean payload) {
        setReceivedBooleanPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingPersonPayloadPublish() returns error? {
    Person messageToSend = personRecord1;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataPersonPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        Person? receivedMsg = getReceivedPersonPayloadValuePublish();
        if (receivedMsg !is ()) {
            test:assertEquals(receivedMsg.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataPersonPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(Person payload) {
        setReceivedPersonPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingMapPayloadPublish() returns error? {
    map<Person> messageToSend = personMap;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataMapPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        map<Person>? receivedMsg = getReceivedMapPayloadValuePublish();
        if (receivedMsg is map<Person>) {
            test:assertEquals(receivedMsg.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataMapPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(map<Person> payload) {
        setReceivedMapPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingTablePayloadPublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataTablePayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        table<Person>? receivedMsg = getReceivedTablePayloadValuePublish();
        if (receivedMsg is table<Person>) {
            test:assertEquals(receivedMsg.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataTablePayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(TableMessage msg, table<Person> payload) {
        setReceivedTablePayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingUnionPayloadPublish() returns error? {
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: "Hello", subject: "dataUnionPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        int|string? receivedMessage = getReceivedUnionPayloadValuePublish();
        if (receivedMessage !is ()) {
            test:assertEquals(receivedMessage, "Hello", msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataUnionPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(int|string payload) {
        setReceivedUnionPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindinRandomPayloadPublish() returns error? {
    RandomPayload randomPayload = {
        content: "Hello",
        replyTo: "test",
        subject: "test-subject"
    };
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: randomPayload, subject: "dataRandomPayload" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        RandomPayload? receivedMsg = getReceivedRandomPayloadValuePublish();
        if (receivedMsg is RandomPayload) {
            test:assertEquals(receivedMsg, randomPayload, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataRandomPayload"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(@nats:Payload RandomPayload payload) {
        setReceivedRandomPayloadValuePublish(payload);
        log:printInfo("Message Received: " + payload.toJsonString());
    }
}

