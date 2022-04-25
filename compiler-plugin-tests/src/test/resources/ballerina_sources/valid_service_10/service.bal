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
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(string data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }

    function testFunction() {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(xml data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(decimal[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(byte[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(anydata data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(string[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(boolean data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

public type Employee record {
    readonly string name;
    int salary;
};

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(Employee employee) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(readonly & table<Employee>[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(json data) {
    }

    remote function onError(nats:Message message, nats:Error err) returns error? {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(int[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(Employee[] employees) {
    }

    remote function onError(nats:Message message, nats:Error err) returns error? {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(json[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) returns nats:Error? {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(readonly & xml[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {
    private final string var1 = "Service";
    private final int var2 = 54;

    remote function onMessage(readonly & anydata[] data) {
    }

    remote function onError(nats:Message message, nats:Error err) {
    }
}
