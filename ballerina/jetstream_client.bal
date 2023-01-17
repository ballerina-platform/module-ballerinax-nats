// Copyright (c) 2022, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
// 
// WSO2 LLC. licenses this file to you under the Apache License,
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

import ballerina/jballerina.java;

# The client provides the capability to publish messages to the NATS JetStream server and 
# manage streams.
public isolated client class JetStreamClient {

    # Initializes the NATS JetStream client.
    # 
    # + natsClient - NATS client object to create the streaming client
    # + streamConfig - Configurations required to add/initialize a stream
    public isolated function init(Client natsClient) returns Error? {
        return streamClientInit(self, natsClient);
    }

    # Publishes data to a given subject.
    # ```ballerina
    # check jetStreamClient->publishMessage(message);
    # ```
    # 
    # + message - The JetStream message to send to
    # + return - `()` or else a `nats:Error` if an error is occurred
    isolated remote function publishMessage(JetStreamMessage message) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
    } external;

    # Retrieves a message synchronously from the given subject.
    # ```ballerina
    # nats:JetStreamMessage message = check jetStreamClient->consumeMessage("subjectName");
    # ```
    #
    # + subject - The name of the subject
    # + timeout - Timeout in seconds
    # + return - `nats:JetStreamMessage` or else a `nats:Error` if an error is occurred
    isolated remote function consumeMessage(string subject, decimal timeout)
    returns JetStreamMessage|Error =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
    } external;

    # Acknowledges a JetStream messages received from a Consumer, indicating
    # the message should not be received again later.
    # ```ballerina
    # check jetStreamCaller->ack(message);
    # ```
    #
    # + message - The message to be acknowledged
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function ack(JetStreamMessage message) =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
    } external;

    # Acknowledges a JetStream message has been received but indicates
    # that the message is not completely processed and should be sent again later.
    # ```ballerina
    # check jetStreamCaller->nak(message);
    # ```
    #
    # + message - The message to be acknowledged
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function nak(JetStreamMessage message) =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
    } external;

    # Indicates that this message is being worked on and reset redelivery timer
    # in the server.
    # ```ballerina
    # check jetStreamCaller->inProgress(message);
    # ```
    #
    # + message - The message to be acknowledged
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function inProgress(JetStreamMessage message) =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
    } external;

    # Loads or creates a stream.
    # ```ballerina
    # check jetStreamClient->addStream(streamConfig);
    # ```
    #
    # + streamConfig - Configurations required to load or create a stream
    # + return - () or else a `nats:Error` if an error occurred
    isolated remote function addStream(StreamConfiguration streamConfig) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ManagementUtils"
    } external;

    # Updates an existing stream.
    # ```ballerina
    # check jetStreamClient->updateStream(streamConfig);
    # ```
    #
    # + streamConfig - Configurations required to update a stream
    # + return - () or else a `nats:Error` if an error occurred
    isolated remote function updateStream(StreamConfiguration streamConfig) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ManagementUtils"
    } external;

    # Deletes an existing stream.
    # ```ballerina
    # check jetStreamClient->deleteStream("name");
    # ```
    #
    # + streamName - Name of the stream to be deleted
    # + return - `()` or else a `nats:Error` if an error is occurred
    isolated remote function deleteStream(string streamName) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ManagementUtils"
    } external;

    # Purge stream messages.
    # ```ballerina
    # check jetStreamClient->purgeStream("name");
    # ```
    #
    # + streamName - Name of the stream to be purged
    # + return - `()` or else a `nats:Error` if an error is occurred
    isolated remote function purgeStream(string streamName) returns Error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.client.ManagementUtils"
    } external;
}

isolated function streamClientInit(JetStreamClient jetStreamClient, Client natsConnection) returns Error? =
@java:Method {
    'class: "io.ballerina.stdlib.nats.jetstream.client.ClientUtils"
} external;
