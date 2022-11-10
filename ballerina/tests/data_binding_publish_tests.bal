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
string receivedXmlValuePublish = "";
json? receivedJsonValuePublish = ();
int receivedIntValuePublish = 0;
float receivedFloatValuePublish = 0.0;
decimal receivedDecimalValuePublish = 0d;
boolean receivedBooleanValuePublish = false;
Person? receivedPersonValuePublish = ();
map<Person>? receivedMapValuePublish = ();
table<Person>? receivedTableValuePublish = ();
string receivedStringPayloadValuePublish = "";
string receivedBytesPayloadValuePublish = "";
string receivedXmlPayloadValuePublish = "";
json? receivedJsonPayloadValuePublish = ();
int receivedIntPayloadValuePublish = 0;
float receivedFloatPayloadValuePublish = 0.0;
decimal receivedDecimalPayloadValuePublish = 0d;
boolean receivedBooleanPayloadValuePublish = false;
Person? receivedPersonPayloadValuePublish = ();
map<Person>? receivedMapPayloadValuePublish = ();
table<Person>? receivedTablePayloadValuePublish = ();
int|string? receivedUnionPayloadValuePublish = ();
RandomPayload? receivedRandomPayloadValuePublish = ();
boolean onErrorReceived = false;
string onErrorMessage = "";

public type RandomPayload record {|
    string content;
    string subject;
    string replyTo?;
|};

@test:Config {
    dependsOn: [testProducer],
    groups: ["nats-basic"]
}
public function testDataBindingStringPublish() returns error? {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceStringPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
public function testDataBindingBytesPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceBytesPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend.toBytes(), subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        byte[] messageContent = msg.content;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            receivedBytesValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

@test:Config {
    dependsOn: [testDataBindingBytesPublish],
    groups: ["nats-basic"]
}
public function testDataBindingXmlPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceXmlPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceXmlPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
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
    dependsOn: [testDataBindingXmlPublish],
    groups: ["nats-basic"]
}
public function testDataBindingJsonPublish() returns error? {
    json messageToSend = jsonData;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceJsonPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceJsonPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(JsonMessage msg) {
        receivedJsonValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingJsonPublish],
    groups: ["nats-basic"]
}
public function testDataBindingIntPublish() returns error? {
    int messageToSend = 521;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceIntPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceIntPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(IntMessage msg) {
        receivedIntValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingIntPublish],
    groups: ["nats-basic"]
}
public function testDataBindingFloatPublish() returns error? {
    float messageToSend = 1995.52;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceFloatPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceFloatPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(FloatMessage msg) {
        receivedFloatValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingFloatPublish],
    groups: ["nats-basic"]
}
public function testDataBindingDecimalPublish() returns error? {
    decimal messageToSend = 1995.52d;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceDecimalPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceDecimalPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(DecimalMessage msg) {
        receivedDecimalValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingDecimalPublish],
    groups: ["nats-basic"]
}
public function testDataBindingBooleanPublish() returns error? {
    boolean messageToSend = true;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceBooleanPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceBooleanPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(BooleanMessage msg) {
        receivedBooleanValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingBooleanPublish],
    groups: ["nats-basic"]
}
public function testDataBindingPersonPublish() returns error? {
    Person messageToSend = personRecord1;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServicePersonPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServicePersonPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(PersonMessage msg) {
        receivedPersonValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingPersonPublish],
    groups: ["nats-basic"]
}
public function testDataBindingMapPublish() returns error? {
    map<Person> messageToSend = personMap;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceMapPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceMapPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(MapMessage msg) {
        receivedMapValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingMapPublish],
    groups: ["nats-basic"]
}
public function testDataBindingTablePublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceTablePublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServiceTablePublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(TableMessage msg) {
        receivedTableValuePublish = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingTablePublish],
    groups: ["nats-basic"]
}
public function testDataBindingPublishError() returns error? {
    json messageToSend = jsonData;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServicePublishError);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
        } else {
            test:assertFail("NATS Connection creation failed.");
        }
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

Service consumerServicePublishError =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(XmlMessage msg) {
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        onErrorReceived = true;
        onErrorMessage = err.message();
    }
};

@test:Config {
    dependsOn: [testDataBindingPublishError],
    groups: ["nats-basic"]
}
public function testDataBindingStringPayloadPublish() returns error? {
    string messageToSend = "Testing Consumer Service With String Data Binding.";
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceStringPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedStringPayloadValuePublish !is "" {
                    test:assertEquals(receivedStringPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceStringPayloadPublish);
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

Service consumerServiceStringPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(string payload) {
        receivedStringPayloadValuePublish = payload;
        log:printInfo("Message Received: " + receivedStringValuePublish);
    }
};

@test:Config {
    dependsOn: [testDataBindingStringPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingBytesPayloadPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceBytesPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend.toBytes(), subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedBytesPayloadValuePublish !is "" {
                    test:assertEquals(receivedBytesPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceBytesPayloadPublish);
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

Service consumerServiceBytesPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(byte[] payload) {
        byte[] messageContent = payload;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            receivedBytesPayloadValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

@test:Config {
    dependsOn: [testDataBindingBytesPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingXmlPayloadPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceXmlPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedXmlPayloadValuePublish !is "" {
                    test:assertEquals(receivedXmlPayloadValuePublish, messageToSend.toString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceXmlPayloadPublish);
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

Service consumerServiceXmlPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(xml payload) {
        string|error message = payload.toString();
        if message is string {
            receivedXmlPayloadValuePublish = message;
            log:printInfo("Message Received: " + message);
        }
    }
};

@test:Config {
    dependsOn: [testDataBindingXmlPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingJsonPayloadPublish() returns error? {
    json messageToSend = jsonData;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceJsonPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedJsonPayloadValuePublish !is () {
                    test:assertEquals(receivedJsonPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceJsonPayloadPublish);
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

Service consumerServiceJsonPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(json payload) {
        receivedJsonPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingJsonPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingIntPayloadPublish() returns error? {
    int messageToSend = 521;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceIntPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedIntPayloadValuePublish !is 0 {
                    test:assertEquals(receivedIntPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceIntPayloadPublish);
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

Service consumerServiceIntPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(int payload) {
        receivedIntPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingIntPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingFloatPayloadPublish() returns error? {
    float messageToSend = 1995.52;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceFloatPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedFloatPayloadValuePublish !is 0.0 {
                    test:assertEquals(receivedFloatPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceFloatPayloadPublish);
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

Service consumerServiceFloatPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(float payload) {
        receivedFloatPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingFloatPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingDecimalPayloadPublish() returns error? {
    decimal messageToSend = 1995.52d;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceDecimalPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedDecimalPayloadValuePublish !is 0d {
                    test:assertEquals(receivedDecimalPayloadValuePublish, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceDecimalPayloadPublish);
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

Service consumerServiceDecimalPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(decimal payload) {
        receivedDecimalPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingDecimalPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingBooleanPayloadPublish() returns error? {
    boolean messageToSend = true;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceBooleanPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedBooleanPayloadValuePublish !is false {
                    test:assertTrue(receivedBooleanPayloadValuePublish, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceBooleanPayloadPublish);
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

Service consumerServiceBooleanPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(boolean payload) {
        receivedBooleanPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    dependsOn: [testDataBindingBooleanPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingPersonPayloadPublish() returns error? {
    Person messageToSend = personRecord1;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServicePersonPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedPersonPayloadValuePublish is Person) {
                    test:assertEquals(receivedPersonPayloadValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServicePersonPayloadPublish);
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

Service consumerServicePersonPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(Person payload) {
        receivedPersonPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingPersonPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingMapPayloadPublish() returns error? {
    map<Person> messageToSend = personMap;
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceMapPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedMapPayloadValuePublish is map<Person>) {
                    test:assertEquals(receivedMapPayloadValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceMapPayloadPublish);
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

Service consumerServiceMapPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(map<Person> payload) {
        receivedMapPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingMapPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindingTablePayloadPublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceTablePayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedTablePayloadValuePublish is table<Person>) {
                    test:assertEquals(receivedTablePayloadValuePublish.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceTablePayloadPublish);
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

Service consumerServiceTablePayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(TableMessage msg, table<Person> payload) {
        receivedTablePayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindingTablePayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindinUnionPayloadPublish() returns error? {
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceUnionPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: "Hello", subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedUnionPayloadValuePublish !is ()) {
                    test:assertEquals(receivedUnionPayloadValuePublish, "Hello", msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceUnionPayloadPublish);
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

Service consumerServiceUnionPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(int|string payload) {
        receivedUnionPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    dependsOn: [testDataBindinUnionPayloadPublish],
    groups: ["nats-basic"]
}
public function testDataBindinRandomPayloadPublish() returns error? {
    RandomPayload randomPayload = {
        content: "Hello",
        replyTo: "test",
        subject: "test-subject"
    };
    Client? newClient = dataClientObj;
    if newClient is Client {
        Listener? sub = dataListenerObj;
        if sub is Listener {
            check sub.attach(consumerServiceRandomPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: randomPayload, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedRandomPayloadValuePublish is RandomPayload) {
                    test:assertEquals(receivedRandomPayloadValuePublish, randomPayload, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            check sub.detach(consumerServiceRandomPayloadPublish);
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

Service consumerServiceRandomPayloadPublish =
@ServiceConfig {
    subject: DATA_BINDING_PUBLISH_SUBJECT
}
service object {
    remote function onMessage(@Payload RandomPayload payload) {
        receivedRandomPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

