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

import ballerina/log;
import ballerina/runtime;
import ballerina/system;
import ballerina/test;

Connection? basicConnection = ();
const SUBJECT_NAME = "nats-basic";
const SERVICE_SUBJECT_NAME = "nats-basic-service";
string receivedConsumerMessage = "";

@test:BeforeSuite
function setup() {
    startDockerContainer();
    log:printInfo("Creating a ballerina NATS connection.");
    Connection newConnection = new(["nats://localhost:4222"]);
    basicConnection = newConnection;
}

@test:Config {
    groups: ["nats-basic"]
}
public function testConnection() {
    boolean flag = false;
    Connection? con = basicConnection;
    if (con is Connection) {
        flag = true;
    }
    test:assertTrue(flag, msg = "NATS Connection creation failed.");
}

@test:Config {
    dependsOn: ["testConnection"],
    groups: ["nats-basic"]
}
public function testProducer() {
    Connection? con = basicConnection;
    if (con is Connection) {
        Producer producer = new(con);
        Error? result = producer->publish(SUBJECT_NAME, "Hello World");
        test:assertEquals(result, (), msg = "Producing a message to the broker caused an error.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

@test:Config {
    dependsOn: ["testProducer"],
    groups: ["nats-basic"]
}
public function testConsumerService() {
    string message = "Testing Consumer Service";
    Connection? con = basicConnection;
    if (con is Connection) {
        Listener sub = new(con);
        Producer producer = new(con);
        checkpanic sub.__attach(consumerService);
        checkpanic sub.__start();
        checkpanic producer->publish(SERVICE_SUBJECT_NAME, message);
        runtime:sleep(5000);
        test:assertEquals(receivedConsumerMessage, message, msg = "Message received does not match.");
    } else {
        test:assertFail("NATS Connection creation failed.");
    }
}

service consumerService =
@SubscriptionConfig {
    subject: SERVICE_SUBJECT_NAME
}
service {
    resource function onMessage(Message msg, string data) {
        receivedConsumerMessage = <@untainted> data;
        log:printInfo("Message Received: " + receivedConsumerMessage);
    }

    resource function onError(Message msg, Error err) {
    }
};

@test:AfterSuite {}
function cleanUp() {
    var dockerStopResult = system:exec("docker", {}, "/", "stop", "nats-tests");
    var dockerRmResult = system:exec("docker", {}, "/", "rm", "nats-tests");
}

function startDockerContainer() {
    log:printInfo("Starting NATS Docker Container.");
    var dockerStartResult = system:exec("docker", {}, "/", "run", "-d", "--name", "nats-tests", "-p", "4222:4222",
        "nats:latest");
    runtime:sleep(20000);
}
