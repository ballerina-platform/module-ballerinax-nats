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
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceStringPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataStringPayload" });
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

nats:Service consumerServiceStringPayloadPublish =
@nats:ServiceConfig {
    subject: "dataStringPayload"
}
service object {
    remote function onMessage(string payload) {
        receivedStringPayloadValuePublish = payload;
        log:printInfo("Message Received: " + receivedStringValuePublish);
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBytesPayloadPublish() returns error? {
    string messageToSend = "Testing Consumer Service With Bytes Data Binding.";
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceBytesPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend.toBytes(), subject: "dataBytesPayload" });
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

nats:Service consumerServiceBytesPayloadPublish =
@nats:ServiceConfig {
    subject: "dataBytesPayload"
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
    groups: ["nats-basic"]
}
public function testDataBindingXmlPayloadPublish() returns error? {
    xml messageToSend = xml `<start><Person><name>wso2</name><location>col-03</location></Person><Person><name>wso2</name><location>col-03</location></Person></start>`;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceXmlPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataXmlPayload" });
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

nats:Service consumerServiceXmlPayloadPublish =
@nats:ServiceConfig {
    subject: "dataXmlPayload"
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
    groups: ["nats-basic"]
}
public function testDataBindingJsonPayloadPublish() returns error? {
    json messageToSend = jsonData;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceJsonPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataJsonPayload" });
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

nats:Service consumerServiceJsonPayloadPublish =
@nats:ServiceConfig {
    subject: "dataJsonPayload"
}
service object {
    remote function onMessage(json payload) {
        receivedJsonPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingIntPayloadPublish() returns error? {
    int messageToSend = 521;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceIntPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataIntPayload" });
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

nats:Service consumerServiceIntPayloadPublish =
@nats:ServiceConfig {
    subject: "dataIntPayload"
}
service object {
    remote function onMessage(int payload) {
        receivedIntPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingFloatPayloadPublish() returns error? {
    float messageToSend = 1995.52;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceFloatPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataFloatPayload" });
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

nats:Service consumerServiceFloatPayloadPublish =
@nats:ServiceConfig {
    subject: "dataFloatPayload"
}
service object {
    remote function onMessage(float payload) {
        receivedFloatPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingDecimalPayloadPublish() returns error? {
    decimal messageToSend = 1995.52d;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceDecimalPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataDecimalPayload" });
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

nats:Service consumerServiceDecimalPayloadPublish =
@nats:ServiceConfig {
    subject: "dataDecimalPayload"
}
service object {
    remote function onMessage(decimal payload) {
        receivedDecimalPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingBooleanPayloadPublish() returns error? {
    boolean messageToSend = true;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceBooleanPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataBooleanPayload" });
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

nats:Service consumerServiceBooleanPayloadPublish =
@nats:ServiceConfig {
    subject: "dataBooleanPayload"
}
service object {
    remote function onMessage(boolean payload) {
        receivedBooleanPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingPersonPayloadPublish() returns error? {
    Person messageToSend = personRecord1;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServicePersonPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataPersonPayload" });
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

nats:Service consumerServicePersonPayloadPublish =
@nats:ServiceConfig {
    subject: "dataPersonPayload"
}
service object {
    remote function onMessage(Person payload) {
        receivedPersonPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingMapPayloadPublish() returns error? {
    map<Person> messageToSend = personMap;
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceMapPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataMapPayload" });
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

nats:Service consumerServiceMapPayloadPublish =
@nats:ServiceConfig {
    subject: "dataMapPayload"
}
service object {
    remote function onMessage(map<Person> payload) {
        receivedMapPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindingTablePayloadPublish() returns error? {
    table<Person> messageToSend = table [];
    messageToSend.add(personRecord1);
    messageToSend.add(personRecord2);
    messageToSend.add(personRecord3);
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceTablePayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: messageToSend, subject: "dataTablePayload" });
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

nats:Service consumerServiceTablePayloadPublish =
@nats:ServiceConfig {
    subject: "dataTablePayload"
}
service object {
    remote function onMessage(TableMessage msg, table<Person> payload) {
        receivedTablePayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindinUnionPayloadPublish() returns error? {
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceUnionPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: "Hello", subject: "dataUnionPayload" });
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

nats:Service consumerServiceUnionPayloadPublish =
@nats:ServiceConfig {
    subject: "dataUnionPayload"
}
service object {
    remote function onMessage(int|string payload) {
        receivedUnionPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

@test:Config {
    groups: ["nats-basic"]
}
public function testDataBindinRandomPayloadPublish() returns error? {
    RandomPayload randomPayload = {
        content: "Hello",
        replyTo: "test",
        subject: "test-subject"
    };
    nats:Client? newClient = dataClientObj2;
    if newClient is nats:Client {
        nats:Listener? sub = dataListenerObj2;
        if sub is nats:Listener {
            check sub.attach(consumerServiceRandomPayloadPublish);
            check sub.'start();
            check newClient->publishMessage({ content: randomPayload, subject: "dataRandomPayload" });
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

nats:Service consumerServiceRandomPayloadPublish =
@nats:ServiceConfig {
    subject: "dataRandomPayload"
}
service object {
    remote function onMessage(@nats:Payload RandomPayload payload) {
        receivedRandomPayloadValuePublish = payload;
        log:printInfo("Message Received: " + payload.toJsonString());
    }
};

