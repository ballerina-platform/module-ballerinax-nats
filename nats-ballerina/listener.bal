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

# Represents the NATS server connection to which a subscription service should be bound in order to
# receive messages of the corresponding subscription.
public class Listener {

    # Creates a new NATS Listener.
    #
    # + connection - An established NATS connection.
    public isolated function init(*ConnectionConfig config, string|string[] url = DEFAULT_URL) returns Error? {
        return consumerInit(self, url, config);
    }

    # Binds a service to the `nats:Listener`.
    #
    # + s - Type descriptor of the service
    # + name - Name of the service
    # + return - `()` or else a `nats:Error` upon failure to register the listener
    public isolated function attach(Service s, string[]|string? name = ()) returns error? {
        return basicRegister(self, s, name);
    }

    # Stops consuming messages and detaches the service from the `nats:Listener`.
    #
    # + s - Type descriptor of the service
    # + return - `()` or else a `nats:Error` upon failure to detach the service
    public isolated function detach(Service s) returns error? {
        return basicDetach(self, s);
    }

    # Starts the `nats:Listener`.
    #
    # + return - `()` or else a `nats:Error` upon failure to start the listener
    public isolated function 'start() returns error? {
        return basicStart(self);
    }

    # Stops the `nats:Listener` gracefully.
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function gracefulStop() returns error? {
        return basicGracefulStop(self);
    }

    # Stops the `nats:Listener` forcefully.
    #
    # + return - `()` or else a `nats:Error` upon failure to stop the listener
    public isolated function immediateStop() returns error? {
        return basicImmediateStop(self);
    }
}

isolated function basicRegister(Listener lis, Service s, string[]|string? name = ()) returns error? =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Register"
} external;

isolated function basicDetach(Listener lis, Service serviceType) returns error? =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Detach"
} external;

isolated function basicStart(Listener lis) =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Start"
} external;

isolated function basicGracefulStop(Listener lis) =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.GracefulStop"
} external;

isolated function basicImmediateStop(Listener lis) =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.ImmediateStop"
} external;

isolated function consumerInit(Listener lis, string|string[] url, *ConnectionConfig config) returns Error? =
@java:Method {
    'class: "org.ballerinalang.nats.basic.consumer.Init"
} external;
