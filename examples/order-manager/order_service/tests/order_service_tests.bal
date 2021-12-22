// Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/http;
import ballerinax/nats;
import ballerina/lang.'string as strings;
import ballerina/lang.runtime as runtime;
import ballerina/lang.value;
import order_service.types;

isolated string messageReceived = "";

isolated function updateMessageReceived(string message) {
    lock {
        messageReceived = message;
    }
}

isolated function getMessageReceived() returns string {
    lock {
        return messageReceived;
    }
}

configurable string USERNAME = "user";
configurable string PASSWORD = "password";

@test:Config{}
function orderServiceTest() returns error? {
    http:Client orderClient = check new ("http://localhost:9090",
        auth = {
            username: USERNAME,
            password: PASSWORD
        }
    );

    nats:Listener sub = checkpanic new(nats:DEFAULT_URL);
    checkpanic sub.attach(consumerService);
    checkpanic sub.'start();
    runtime:sleep(5);

    string orderName = "PS5";
    string orderStatus = "SUCCESS";

    string response = check orderClient->get("/nats/publish?message=PS5&status=SUCCESS");
    string expectedResponse = "Message sent to the NATS subject " + SUBJECT + " successfully. Order " + orderName
                + " with status " + orderStatus;
    test:assertEquals(response, expectedResponse);

    string messageContent = getMessageReceived();
    json jsonContent = check value:fromJsonString(messageContent);
    json jsonClone = jsonContent.cloneReadOnly();
    types:Order neworder = <types:Order> jsonClone;

    test:assertEquals(neworder.name, orderName);
    test:assertEquals(neworder.status, orderStatus);
    return;
}

nats:Service consumerService =
@nats:ServiceConfig {
    subject: SUBJECT
}
service object {
    remote function onMessage(nats:Message msg) {
        byte[] messageContent = <@untainted> msg.content;

        string|error message = strings:fromBytes(messageContent);
        if (message is string) {
            updateMessageReceived(message);
        }
    }
};
