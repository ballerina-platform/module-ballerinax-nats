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
import ballerina/test;
import ballerina/log;

const DATA_BINDING_REQUEST_SUBJECT = "bind.request";
string receivedStringValueRequest = "";
string receivedBytesValueRequest = "";
string receivedXmlValueRequest = "";
json? receivedJsonValueRequest = ();
int receivedIntValueRequest = 0;
float receivedFloatValueRequest = 0.0;
decimal receivedDecimalValueRequest = 0d;
boolean receivedBooleanValueRequest = false;
Person? receivedPersonValueRequest = ();
map<Person>? receivedMapValueRequest = ();
table<Person>? receivedTableValueRequest = ();

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

@test:Config {
    dependsOn: [testDataBindingStringRequest],
    groups: ["nats-basic"]
}
public function testDataBindingBytesRequest() {
    string messageToReceive = "consumerServiceBytesRequest received message";
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceBytesRequest);
            checkpanic sub.'start();
            BytesMessage dataBoundMessage = 
                checkpanic newClient->requestMessage({ content: messageToSend.toBytes(), subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedBytesValueRequest !is "" {
                    string receivedMessage = receivedBytesValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            byte[] messageContent = <@untainted> dataBoundMessage.content;
            string|error message = 'strings:fromBytes(messageContent);
            if (message is string) {
                test:assertEquals(message, messageToReceive, msg = "Message received does not match.");
            } else {
                test:assertFail("Failed to convert message to string from byte message.");
            }

            checkpanic sub.detach(consumerServiceBytesRequest);
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

Service consumerServiceBytesRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(BytesMessage msg) returns anydata {
        byte[] messageContent = <@untainted> msg.content;
        string|error message = 'strings:fromBytes(messageContent);
        if message is string {
            receivedBytesValueRequest = message;
            log:printInfo("Message Received: " + message);
            return "consumerServiceBytesRequest received message";
        } else {
            return "error";
        }
    }
};

@test:Config {
    dependsOn: [testDataBindingBytesRequest],
    groups: ["nats-basic"]
}
public function testDataBindingXmlRequest() {
    xml messageToReceive = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceXmlRequest);
            checkpanic sub.'start();
            XmlMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedXmlValueRequest !is "" {
                    string receivedMessage = receivedXmlValueRequest;
                    test:assertEquals(receivedMessage, messageToSend.toString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content.toString(), messageToReceive.toString(), msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceXmlRequest);
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

Service consumerServiceXmlRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(XmlMessage msg) returns anydata {
        string|error message = msg.content.toString();
        if message is string {
            receivedXmlValueRequest = message;
            log:printInfo("Message Received: " + message);
        }
        return xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    }
};


@test:Config {
    dependsOn: [testDataBindingXmlRequest],
    groups: ["nats-basic"]
}
public function testDataBindingJsonRequest() {
    json messageToSend = jsonData;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceJsonRequest);
            checkpanic sub.'start();
            JsonMessage dataBoundMessage = checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedJsonValueRequest !is () {
                    json receivedMessage = receivedJsonValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content.toJsonString(), jsonData.toJsonString(), msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceJsonRequest);
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

Service consumerServiceJsonRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(JsonMessage msg) returns anydata {
        receivedJsonValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
        return jsonData;
    }
};



@test:Config {
    dependsOn: [testDataBindingJsonRequest],
    groups: ["nats-basic"]
}
public function testDataBindingIntRequest() {
    int messageToSend = 521;
    int messageToReceive = 1995;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceIntRequest);
            checkpanic sub.'start();
            IntMessage dataBoundMessage = checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedIntValueRequest !is 0 {
                    int receivedMessage = receivedIntValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content, messageToReceive, msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceIntRequest);
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

Service consumerServiceIntRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(IntMessage msg) returns anydata {
        receivedIntValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
        return 1995;
    }
};

@test:Config {
    dependsOn: [testDataBindingIntRequest],
    groups: ["nats-basic"]
}
public function testDataBindingFloatRequest() {
    float messageToReceive = 5.21;
    float messageToSend = 1995.52;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceFloatRequest);
            checkpanic sub.'start();
            FloatMessage dataBoundMessage = checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedFloatValueRequest !is 0.0 {
                    float receivedMessage = receivedFloatValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content, messageToReceive, msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceFloatRequest);
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

Service consumerServiceFloatRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(FloatMessage msg) returns anydata {
        receivedFloatValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
        return 5.21;
    }
};

@test:Config {
    dependsOn: [testDataBindingFloatRequest],
    groups: ["nats-basic"]
}
public function testDataBindingDecimalRequest() {
    decimal messageToReceive = 5.21d;
    decimal messageToSend = 1995.52d;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceDecimalRequest);
            checkpanic sub.'start();
            DecimalMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedDecimalValueRequest !is 0d {
                    decimal receivedMessage = receivedDecimalValueRequest;
                    test:assertEquals(receivedMessage, messageToSend, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content, messageToReceive, msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceDecimalRequest);
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

Service consumerServiceDecimalRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(DecimalMessage msg) returns anydata {
        receivedDecimalValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
        return 5.21d;
    }
};

@test:Config {
    dependsOn: [testDataBindingDecimalRequest],
    groups: ["nats-basic"]
}
public function testDataBindingBooleanRequest() {
    boolean messageToSend = true;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceBooleanRequest);
            checkpanic sub.'start();
            BooleanMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if receivedBooleanValueRequest !is false {
                    test:assertTrue(receivedBooleanValueRequest, msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertTrue(dataBoundMessage.content, msg = "Message received as reply does not match.");
            checkpanic sub.detach(consumerServiceBooleanRequest);
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

Service consumerServiceBooleanRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(BooleanMessage msg) returns anydata {
        receivedBooleanValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toString());
        return true;
    }
};

@test:Config {
    dependsOn: [testDataBindingBooleanRequest],
    groups: ["nats-basic"]
}
public function testDataBindingPersonRequest() {
    Person messageToReceive = personRecord2;
    Person messageToSend = personRecord1;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServicePersonRequest);
            checkpanic sub.'start();
            PersonMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedPersonValueRequest is Person) {
                    test:assertEquals(receivedPersonValueRequest.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content.toJsonString(), messageToReceive.toJsonString(), msg = "Message received does not match.");
            checkpanic sub.detach(consumerServicePersonRequest);
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

Service consumerServicePersonRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(PersonMessage msg) returns anydata {
        receivedPersonValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
        return personRecord2;
    }
};

@test:Config {
    dependsOn: [testDataBindingPersonRequest],
    groups: ["nats-basic"]
}
public function testDataBindingMapRequest() {
    map<Person> messageToSend = personMap;
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceMapRequest);
            checkpanic sub.'start();
            MapMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedMapValueRequest is map<Person>) {
                    test:assertEquals(receivedMapValueRequest.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content.toJsonString(), personMap.toJsonString(), msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceMapRequest);
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

Service consumerServiceMapRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(MapMessage msg) returns anydata {
        receivedMapValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
        return personMap;
    }
};

@test:Config {
    dependsOn: [testDataBindingMapRequest],
    groups: ["nats-basic"]
}
public function testDataBindingTableRequest() {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    Client? newClient = clientObj;
    if newClient is Client {
        Listener? sub = listenerObj;
        if sub is Listener {
            checkpanic sub.attach(consumerServiceTableRequest);
            checkpanic sub.'start();
            TableMessage dataBoundMessage =
                checkpanic newClient->requestMessage({ content: messageToSend, subject: DATA_BINDING_REQUEST_SUBJECT });
            int timeoutInSeconds = 120;
            // Test fails in 2 minutes if it is failed to receive the message
            while timeoutInSeconds > 0 {
                if (receivedTableValueRequest is table<Person>) {
                    test:assertEquals(receivedTableValueRequest.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
                    break;
                } else {
                    runtime:sleep(1);
                    timeoutInSeconds = timeoutInSeconds - 1;
                }
            }
            test:assertEquals(dataBoundMessage.content.toJsonString(), messageToSend.toJsonString(), msg = "Message received does not match.");
            checkpanic sub.detach(consumerServiceTableRequest);
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

Service consumerServiceTableRequest =
@ServiceConfig {
    subject: DATA_BINDING_REQUEST_SUBJECT
}
service object {
    remote function onRequest(TableMessage msg) returns anydata {
        receivedTableValueRequest = msg.content;
        log:printInfo("Message Received: " + msg.content.toJsonString());
        table<Person> message = table [];
        message.add(personRecord1);
        message.add(personRecord2);
        message.add(personRecord3);
        return message;
    }
};
