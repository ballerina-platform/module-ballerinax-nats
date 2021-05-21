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

# Represents the NATS listener to which a subscription service should be bound in order to
# receive messages.
public isolated class Listener {

    # Initializes the NATS listener.
    # ```ballerina
    #  nats:Listener natsListener = check new(nats:DEFAULT_URL);
    # ```
    #
    # + url - The NATS Broker URL. For a clustered use case, provide the URLs as a string array
    # + config - The connection configurations
    public isolated function init(string|string[] url, *ConnectionConfiguration config) returns Error? {
        return consumerInit(self, url, config);
    }

    # Binds a service to the `nats:Listener`.
    # ```ballerina
    # check natsListener.attach(service, "serviceName");
    # ```
    #
    # + s - The type descriptor of the service
    # + name - The name of the service
    # + return - `()` or else a `nats:Error` upon failure to attach
    public isolated function attach(Service s, string[]|string? name = ()) returns error? =
    @java:Method {
        'class: "org.ballerinalang.nats.basic.consumer.Register"
    } external;

    # Stops consuming messages and detaches the service from the `nats:Listener`.
    # ```ballerina
    # check natsListener.detach(service);
    # ```
    #
    # + s - The type descriptor of the service
    # + return - `()` or else a `nats:Error` upon failure to detach
    public isolated function detach(Service s) returns error? =
    @java:Method {
        'class: "org.ballerinalang.nats.basic.consumer.Detach"
    } external;

    # Starts the `nats:Listener`.
    # ```ballerina
    # check natsListener.'start();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to start the listener
    public isolated function 'start() returns error? {
        return basicStart(self);
    }

    # Stops the `nats:Listener` gracefully.
    # ```ballerina
    # check natsListener.gracefulStop();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? =
    @java:Method {
        'class: "org.ballerinalang.nats.basic.consumer.GracefulStop"
    } external;

    # Stops the `nats:Listener` forcefully.
    # ```ballerina
    # check natsListener.immediateStop();
    # ```
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? =
    @java:Method {
        'class: "org.ballerinalang.nats.basic.consumer.ImmediateStop"
    } external;
}

isolated function basicStart(Listener lis) =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Start"
} external;

isolated function consumerInit(Listener lis, string|string[] url, *ConnectionConfiguration config) returns Error? =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Init"
} external;
