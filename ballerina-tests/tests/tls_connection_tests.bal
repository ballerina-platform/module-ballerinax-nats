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
import ballerinax/nats;

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection1() returns error? {
    nats:SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore.jks",
            password: "password"
        },
        key: {
            path: "tests/certs/keystore.jks",
            password: "password"
        }
    };
    nats:Client|nats:Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if newClient is error {
        test:assertFail("NATS Connection initialization with TLS failed.");
    } else {
        check newClient.close();
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection2() {
    nats:SecureSocket secured = {
        cert: {
            path: "tests/certs/truststore1.jks",
            password: "password"
        },
        protocol: {
            name: nats:TLS
        }
    };
    nats:Client|nats:Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if newClient is nats:Client {
        test:assertFail("Error expected for NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection3() returns error? {
    nats:SecureSocket secured = {
        cert: "tests/certs/server.crt",
        key: {
            certFile: "tests/certs/client.crt",
            keyFile: "tests/certs/client.key"
        }
    };
    nats:Client|nats:Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if newClient is error {
        test:assertFail("NATS Connection initialization with TLS failed.");
    } else {
        check newClient.close();
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection4() {
    nats:SecureSocket secured = {
        cert: "tests/certs/server1.crt",
        key: {
            certFile: "tests/certs/client.crt",
            keyFile: "tests/certs/client.key"
        }
    };
    nats:Client|nats:Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if newClient is nats:Client {
        test:assertFail("Error expected for NATS Connection initialization with TLS.");
    }
}

@test:Config {
    groups: ["nats-basic"]
}
public isolated function testTlsConnection5() {
    nats:SecureSocket secured = {
        cert: "tests/certs/server.crt",
        key: {
            certFile: "tests/certs/client1.crt",
            keyFile: "tests/certs/client1.key"
        }
    };
    nats:Client|nats:Error newClient = new("nats://localhost:4225", secureSocket = secured);
    if newClient is nats:Client {
        test:assertFail("Error expected for NATS Connection initialization with TLS.");
    }
}
