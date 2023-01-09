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

# Represents the NATS JetStream listener to which a subscription service should be bound in order to
# receive messages. Receives messages from a NATS JetStream server.
public isolated class JetStreamListener {

    # Initializes the NATS JetStream listener.
    # 
    # + natsClient - NATS client object to create the streaming client. 
    public isolated function init(Client natsClient) returns Error? {
        return streamListenerInit(self, natsClient);
    }

    # Binds a service to the `nats:JetStreamListener`.
    # ```ballerina
    # check jetStreamListener.attach(service, "serviceName");
    # ```
    #
    # + s - The type descriptor of the service
    # + name - The name of the service
    # + return - `()` or else a `nats:Error` upon failure to attach
    public isolated function attach(JetStreamService s, string[]|string? name = ()) returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
    } external;

    # Stops consuming messages and detaches the service from the `nats:JetStreamListener`.
    # ```ballerina
    # check jetStreamListener.detach(service);
    # ```
    #
    # + s - The type descriptor of the service
    # + return - `()` or else a `nats:Error` upon failure to detach
    public isolated function detach(JetStreamService s) returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
    } external;

    # Starts the `nats:JetStreamListener`.
    # ```ballerina
    # check jetStreamListener.'start();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to start the listener
    public isolated function 'start() returns error? {
        return basicStreamStart(self);
    }

    # Stops the `nats:JetStreamListener` gracefully.
    # ```ballerina
    # check jetStreamListener.gracefulStop();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
    } external;

    # Stops the `nats:JetStreamListener` forcefully.
    # ```ballerina
    # check jetStreamListener.immediateStop();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
    } external;
}

isolated function streamListenerInit(JetStreamListener jetStreamLis, Client natsConnection) returns Error? =
@java:Method {
    'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
} external;

isolated function basicStreamStart(JetStreamListener lis) =
@java:Method {
    'class: "io.ballerina.stdlib.nats.jetstream.listener.ListenerUtils"
} external;
