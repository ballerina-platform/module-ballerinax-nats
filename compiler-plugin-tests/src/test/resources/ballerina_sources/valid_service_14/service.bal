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

public type BytesMessage record {|
    *nats:AnydataMessage;
    byte[] content;
|};

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(BytesMessage message) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onRequest(BytesMessage message) {
    }
}

service "hello" on subscription {

    remote function onRequest(BytesMessage message) {
    }
}

public type Person record {|
    string name;
|};

public type PersonMessage record {|
    Person content;
    string subject;
    string replyTo?;
|};

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(PersonMessage message) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onRequest(PersonMessage message) {
    }
}

service "hello" on subscription {

    remote function onRequest(PersonMessage message) {
    }
}

public type JsonMessage record {|
    json content;
    string subject;
    string replyTo?;
|};

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(JsonMessage message) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onRequest(JsonMessage message) {
    }
}

service "hello" on subscription {

    remote function onRequest(JsonMessage message) {
    }
}

public type XmlMessage record {|
    *nats:AnydataMessage;
    xml content;
|};

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(XmlMessage message) {
    }
}

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onRequest(XmlMessage message) {
    }
}

service "hello" on subscription {

    remote function onRequest(XmlMessage message) {
    }
}
