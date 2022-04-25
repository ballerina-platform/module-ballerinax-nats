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
boolean onErrorReceived = false;
string onErrorMessage = "";

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

@test:Config {
    dependsOn: [testDataBindingBytesPublish],
    groups: ["nats-basic"]
}
public function testDataBindingXmlPublish() {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceXmlPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedXmlValuePublish !is "" {
                    string receivedMessage = receivedXmlValuePublish;
                    test:assertEquals(receivedMessage, messageToSend.toString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceXmlPublish);
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
public function testDataBindingJsonPublish() {
    json messageToSend = jsonData;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceJsonPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedJsonValuePublish !is () {
                    json receivedMessage = receivedJsonValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceJsonPublish);
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
public function testDataBindingIntPublish() {
    int messageToSend = 521;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceIntPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedIntValuePublish !is 0 {
                    int receivedMessage = receivedIntValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceIntPublish);
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
public function testDataBindingFloatPublish() {
    float messageToSend = 1995.52;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceFloatPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedFloatValuePublish !is 0.0 {
                    float receivedMessage = receivedFloatValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceFloatPublish);
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
public function testDataBindingDecimalPublish() {
    decimal messageToSend = 1995.52d;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceDecimalPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedDecimalValuePublish !is 0d {
                    decimal receivedMessage = receivedDecimalValuePublish;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            checkpanic sub.detach(consumerServiceDecimalPublish);
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
public function testDataBindingBooleanPublish() {
    boolean messageToSend = true;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceBooleanPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
            checkpanic sub.detach(consumerServiceBooleanPublish);
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
public function testDataBindingPersonPublish() {
    Person messageToSend = personRecord1;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServicePersonPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
            checkpanic sub.detach(consumerServicePersonPublish);
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
public function testDataBindingMapPublish() {
    map<Person> messageToSend = personMap;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceMapPublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
            checkpanic sub.detach(consumerServiceMapPublish);
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
public function testDataBindingTablePublish() {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceTablePublish);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
            checkpanic sub.detach(consumerServiceTablePublish);
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
public function testDataBindingPublishError() {
    json messageToSend = jsonData;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServicePublishError);
            checkpanic sub.'start();
            checkpanic newClient->publishMessage({ content: messageToSend, subject: DATA_BINDING_PUBLISH_SUBJECT });
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
            checkpanic sub.detach(consumerServicePublishError);
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
