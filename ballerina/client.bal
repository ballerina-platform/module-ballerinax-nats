// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/jballerina.java;

# The client provides the capability to publish messages to the NATS server.
public isolated client class Client {

    # Initializes the NATS client.
    # ```ballerina
    # nats:Client natsClient = check new(nats:DEFAULT_URL);
    # ```
    #
    # + url - The NATS broker URL. For a clustered use case, provide the URLs as a string array
    # + config - The connection configurations
    public isolated function init(string|string[] url, *ConnectionConfiguration config) returns Error? {
        return clientInit(self, url, config);
    }

    # Publishes data to a given subject.
    # ```ballerina
    # check natsClient->publishMessage(message);
    # ```
    #
    # + message - The message to be published
    # + return -  `()` or else a `nats:Error` if an error occurred
    isolated remote function publishMessage(Message message) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.basic.client.Publish"
    } external;

    # Publishes data to a given subject and waits for a response.
    # ```ballerina
    # check natsClient->requestMessage(message, 5);
    # ```
    #
    # + message - The message to be published
    # + duration - The time (in seconds) to wait for the response
    # + return -  The response or else a `nats:Error` if an error occurred
    isolated remote function requestMessage(Message message, decimal? duration = ())
            returns Message|Error =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.basic.client.Request"
    } external;

    # Closes the NATS client connection.
    # ```ballerina
    # check natsClient.close();
    # ```
    #
    # + return - `()` or else a `nats:Error` if an error is occurred
    public isolated function close() returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.basic.client.CloseConnection"
    } external;
}

isolated function clientInit(Client clientObj, string|string[] url, *ConnectionConfiguration config) returns Error? =
@java:Method {
    'class: "io.ballerina.stdlib.nats.basic.client.Init"
} external;
