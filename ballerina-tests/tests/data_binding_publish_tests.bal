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

string receivedStringValuePublish = "";
string receivedBytesValuePublish = "";
string receivedXmlValuePublish = "";
json? receivedJsonValuePublish = ();
int receivedIntValuePublish = 0;
float receivedFloatValuePublish = 0.0;
decimal receivedDecimalValuePublish = 0d;
boolean receivedBooleanValuePublish = false;
Person? receivedPersonValuePublish = ();
map<Person>? receivedMapValuePublish = ();
table<Person>? receivedTableValuePublish = ();
boolean onErrorReceived = false;
string onErrorMessage = "";

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingStringPublish() returns error? {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceStringPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataString" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedStringValuePublish !is "" {
            test:assertEquals(receivedStringValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceStringPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceStringPublish =
@nats:ServiceConfig {
    subject: "dataString"
}
service object {
    remote function onMessage(StringMessage msg) {
        receivedStringValuePublish = msg.content;
        log:printInfo("Message Received: " + receivedStringValuePublish);
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBytesPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceBytesPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend.toBytes(), subject: "dataBytes" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedBytesValuePublish !is "" {
            test:assertEquals(receivedBytesValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceBytesPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceBytesPublish =
@nats:ServiceConfig {
    subject: "dataBytes"
}
service object {
    remote function onMessage(nats:BytesMessage msg) {
        byte[] messageContent = msg.content;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            receivedBytesValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingXmlPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceXmlPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataXml" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedXmlValuePublish !is "" {
            test:assertEquals(receivedXmlValuePublish, messageToSend.toString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceXmlPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceXmlPublish =
@nats:ServiceConfig {
    subject: "dataXml"
}
service object {
    remote function onMessage(XmlMessage msg) {
        string|error message = msg.content.toString();
        if message is string {
            receivedXmlValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingJsonPublish() returns error? {
    json messageToSend = jsonData;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceJsonPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataJson" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedJsonValuePublish !is () {
            test:assertEquals(receivedJsonValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceJsonPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceJsonPublish =
@nats:ServiceConfig {
    subject: "dataJson"
}
service object {
    remote function onMessage(JsonMessage msg) {
        receivedJsonValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingIntPublish() returns error? {
    int messageToSend = 521;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceIntPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataInt" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedIntValuePublish !is 0 {
            test:assertEquals(receivedIntValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceIntPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceIntPublish =
@nats:ServiceConfig {
    subject: "dataInt"
}
service object {
    remote function onMessage(IntMessage msg) {
        receivedIntValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingFloatPublish() returns error? {
    float messageToSend = 1995.52;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceFloatPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataFloat" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedFloatValuePublish !is 0.0 {
            test:assertEquals(receivedFloatValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceFloatPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceFloatPublish =
@nats:ServiceConfig {
    subject: "dataFloat"
}
service object {
    remote function onMessage(FloatMessage msg) {
        receivedFloatValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingDecimalPublish() returns error? {
    decimal messageToSend = 1995.52d;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceDecimalPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataDecimal" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedDecimalValuePublish !is 0d {
            test:assertEquals(receivedDecimalValuePublish, messageToSend, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceDecimalPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceDecimalPublish =
@nats:ServiceConfig {
    subject: "dataDecimal"
}
service object {
    remote function onMessage(DecimalMessage msg) {
        receivedDecimalValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBooleanPublish() returns error? {
    boolean messageToSend = true;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceBooleanPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataBoolean" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedBooleanValuePublish !is false {
            test:assertTrue(receivedBooleanValuePublish, msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceBooleanPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceBooleanPublish =
@nats:ServiceConfig {
    subject: "dataBoolean"
}
service object {
    remote function onMessage(BooleanMessage msg) {
        receivedBooleanValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingPersonPublish() returns error? {
    Person messageToSend = personRecord1;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServicePersonPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataPerson" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if (receivedPersonValuePublish is Person) {
            test:assertEquals(receivedPersonValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServicePersonPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServicePersonPublish =
@nats:ServiceConfig {
    subject: "dataPerson"
}
service object {
    remote function onMessage(PersonMessage msg) {
        receivedPersonValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingMapPublish() returns error? {
    map<Person> messageToSend = personMap;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceMapPublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataMap" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if (receivedMapValuePublish is map<Person>) {
            test:assertEquals(receivedMapValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceMapPublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceMapPublish =
@nats:ServiceConfig {
    subject: "dataMap"
}
service object {
    remote function onMessage(MapMessage msg) {
        receivedMapValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingTablePublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServiceTablePublish);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataTable" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if (receivedTableValuePublish is table<Person>) {
            test:assertEquals(receivedTableValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServiceTablePublish);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServiceTablePublish =
@nats:ServiceConfig {
    subject: "dataTable"
}
service object {
    remote function onMessage(TableMessage msg) {
        receivedTableValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingPublishError() returns error? {
    json messageToSend = jsonData;
    nats:Client newClient = check new(DATA_BINDING_URL);
    nats:Listener sub = check new(DATA_BINDING_URL);
    check sub.attach(consumerServicePublishError);
    check sub.'start();
    check newClient->publishMessage({ content: messageToSend, subject: "dataError" });
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if (onErrorReceived) {
            string errorMessage = "Data binding failed: failed to parse xml:";
            test:assertTrue(onErrorMessage.startsWith(errorMessage), msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    check sub.detach(consumerServicePublishError);
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
}

nats:Service consumerServicePublishError =
@nats:ServiceConfig {
    subject: "dataError"
}
service object {
    remote function onMessage(XmlMessage msg) {
    }

    remote function onError(nats:Message message, nats:Error err) {
        log:printInfo("Error Received: " + err.message());
        onErrorReceived = true;
        onErrorMessage = err.message();
    }
};
