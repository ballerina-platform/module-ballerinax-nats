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

isolated string receivedStringValuePublish = "";
isolated string receivedBytesValuePublish = "";
isolated string receivedXmlValuePublish = "";
isolated json receivedJsonValuePublish = ();
isolated int receivedIntValuePublish = 0;
isolated float receivedFloatValuePublish = 0.0;
isolated decimal receivedDecimalValuePublish = 0d;
isolated boolean receivedBooleanValuePublish = false;
isolated Person? receivedPersonValuePublish = ();
isolated map<Person>? receivedMapValuePublish = ();
isolated table<Person>? receivedTableValuePublish = ();
isolated boolean onErrorReceived = false;
isolated string onErrorMessage = "";

isolated function setReceivedStringValuePublish(string message) {
    lock {
        receivedStringValuePublish = message;
    }
}

isolated function getReceivedStringValuePublish() returns string {
    lock {
        return receivedStringValuePublish;
    }
}

isolated function setReceivedBytesValuePublish(string message) {
    lock {
        receivedBytesValuePublish = message;
    }
}

isolated function getReceivedBytesValuePublish() returns string {
    lock {
        return receivedBytesValuePublish;
    }
}

isolated function setReceivedXmlValuePublish(string message) {
    lock {
        receivedXmlValuePublish = message;
    }
}

isolated function getReceivedXmlValuePublish() returns string {
    lock {
        return receivedXmlValuePublish;
    }
}

isolated function setReceivedJsonValuePublish(json message) {
    lock {
        receivedJsonValuePublish = message.clone();
    }
}

isolated function getReceivedJsonValuePublish() returns json {
    lock {
        return receivedJsonValuePublish.clone();
    }
}

isolated function setReceivedIntValuePublish(int message) {
    lock {
        receivedIntValuePublish = message;
    }
}

isolated function getReceivedIntValuePublish() returns int {
    lock {
        return receivedIntValuePublish;
    }
}

isolated function setReceivedFloatValuePublish(float message) {
    lock {
        receivedFloatValuePublish = message;
    }
}

isolated function getReceivedFloatValuePublish() returns float {
    lock {
        return receivedFloatValuePublish;
    }
}

isolated function setReceivedDecimalValuePublish(decimal message) {
    lock {
        receivedDecimalValuePublish = message;
    }
}

isolated function getReceivedDecimalValuePublish() returns decimal {
    lock {
        return receivedDecimalValuePublish;
    }
}

isolated function setReceivedBooleanValuePublish(boolean message) {
    lock {
        receivedBooleanValuePublish = message;
    }
}

isolated function getReceivedBooleanValuePublish() returns boolean {
    lock {
        return receivedBooleanValuePublish;
    }
}

isolated function setReceivedPersonValuePublish(Person? message) {
    lock {
        receivedPersonValuePublish = message.clone();
    }
}

isolated function getReceivedPersonValuePublish() returns Person? {
    lock {
        return receivedPersonValuePublish.clone();
    }
}

isolated function setReceivedMapValuePublish(map<Person>? message) {
    lock {
        receivedMapValuePublish = message.clone();
    }
}

isolated function getReceivedMapValuePublish() returns map<Person>? {
    lock {
        return receivedMapValuePublish.clone();
    }
}

isolated function setReceivedTableValuePublish(table<Person>? message) {
    lock {
        receivedTableValuePublish = message.clone();
    }
}

isolated function getReceivedTableValuePublish() returns table<Person>? {
    lock {
        return receivedTableValuePublish.clone();
    }
}

isolated function setOnErrorReceived(boolean message) {
    lock {
        onErrorReceived = message;
    }
}

isolated function getOnErrorReceived() returns boolean {
    lock {
        return onErrorReceived;
    }
}

isolated function setOnErrorMessage(string message) {
    lock {
        onErrorMessage = message;
    }
}

isolated function getOnErrorMessage() returns string {
    lock {
        return onErrorMessage;
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingStringPublish() returns error? {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataString" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedStringValuePublish() !is "" {
            string receivedMessage = getReceivedStringValuePublish();
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
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataString"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(StringMessage msg) {
        string message = msg.content;
        setReceivedStringValuePublish(message);
        log:printInfo("Message Received: " + message);
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingBytesPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend.toBytes(), subject: "dataBytes" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedBytesValuePublish() !is "" {
            string receivedMessage = getReceivedBytesValuePublish();
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
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataBytes"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(nats:BytesMessage msg) {
        byte[] messageContent = msg.content;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            setReceivedBytesValuePublish(message);
            log:printInfo("Message Received: " + message);
        }
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingXmlPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataXml" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedXmlValuePublish() !is "" {
            string receivedMessage = getReceivedXmlValuePublish();
            test:assertEquals(receivedMessage, messageToSend.toString(), msg = "Message received does not match.");
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
    subject: "dataXml"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(XmlMessage msg) {
        string|error message = msg.content.toString();
        if message is string {
            setReceivedXmlValuePublish(message);
            log:printInfo("Message Received: " + message);
        }
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingJsonPublish() returns error? {
    json messageToSend = jsonData;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataJson" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        json messageReceived = getReceivedJsonValuePublish();
        if messageReceived !is () {
            test:assertEquals(messageReceived, messageToSend, msg = "Message received does not match.");
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
    subject: "dataJson"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(JsonMessage msg) {
        setReceivedJsonValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingIntPublish() returns error? {
    int messageToSend = 521;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataInt" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedIntValuePublish() !is 0 {
            int receivedMessage = getReceivedIntValuePublish();
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
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataInt"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(IntMessage msg) {
        setReceivedIntValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingFloatPublish() returns error? {
    float messageToSend = 1995.52;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataFloat" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedFloatValuePublish() !is 0.0 {
            float receivedMessage = getReceivedFloatValuePublish();
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
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataFloat"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(FloatMessage msg) {
        setReceivedFloatValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingDecimalPublish() returns error? {
    decimal messageToSend = 1995.52d;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataDecimal" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedDecimalValuePublish() !is 0d {
            decimal receivedMessage = getReceivedDecimalValuePublish();
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
    check newClient.close();
}

@nats:ServiceConfig {
    subject: "dataDecimal"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(DecimalMessage msg) {
        setReceivedDecimalValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingBooleanPublish() returns error? {
    boolean messageToSend = true;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataBoolean" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if getReceivedBooleanValuePublish() !is false {
            boolean receivedMessage = getReceivedBooleanValuePublish();
            test:assertTrue(receivedMessage, msg = "Message received does not match.");
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
    subject: "dataBoolean"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(BooleanMessage msg) {
        setReceivedBooleanValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingPersonPublish() returns error? {
    Person messageToSend = personRecord1;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataPerson" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        Person? messageReceived = getReceivedPersonValuePublish();
        if (messageReceived is Person) {
            test:assertEquals(messageReceived.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
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
    subject: "dataPerson"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(PersonMessage msg) {
        setReceivedPersonValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingMapPublish() returns error? {
    map<Person> messageToSend = personMap;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataMap" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        map<Person>? receivedMessage = getReceivedMapValuePublish();
        if (receivedMessage is map<Person>) {
            test:assertEquals(receivedMessage.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
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
    subject: "dataMap"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(MapMessage msg) {
        setReceivedMapValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingTablePublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataTable" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        table<Person>? messageReceived = getReceivedTableValuePublish();
        if (messageReceived is table<Person>) {
            test:assertEquals(messageReceived.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
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
    subject: "dataTable"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(TableMessage msg) {
        setReceivedTableValuePublish(msg.content);
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
}

@test:Config {
    groups: ["nats-databinding"]
}
public function testDataBindingPublishError() returns error? {
    json messageToSend = jsonData;
    nats:Client newClient = check new(DATA_BINDING_URL);
    check newClient->publishMessage({ content: messageToSend, subject: "dataError" });
    int timeoutInSeconds = 60;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if (getOnErrorReceived()) {
            string errorMessage = "Data binding failed: failed to parse xml:";
            string receivedMessage = getOnErrorMessage();
            test:assertTrue(receivedMessage.startsWith(errorMessage), msg = "Message received does not match.");
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
    subject: "dataError"
}
service nats:Service on new nats:Listener(DATA_BINDING_URL) {
    remote function onMessage(XmlMessage msg) {
    }

    remote function onError(nats:AnydataMessage message, nats:Error err) {
        log:printInfo("Error Received: " + err.message());
        setOnErrorReceived(true);
        setOnErrorMessage(err.message());
    }
}
