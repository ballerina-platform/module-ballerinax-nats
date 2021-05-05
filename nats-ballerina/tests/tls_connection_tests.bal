// Copyright (c) 2021 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/test;

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection1() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore.p12",
            password: "password"
        },
        key: {
            path: "tests/certs/keystore.p12",
            password: "password"
        },
        protocol: {
            name: TLS
        }
    };
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is error) {
        test:assertFail("NATS Connection initialization with TLS failed.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection3() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore1.p12",
            password: "password"
        }
    };
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is Client) {
        test:assertFail("Error expected for NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection4() {
    SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore.p12",
            password: "password"
        }
    };
    Client|Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if (newClient is error) {
        test:assertFail("NATS Connection initialization with TLS failed.");
    }
}
