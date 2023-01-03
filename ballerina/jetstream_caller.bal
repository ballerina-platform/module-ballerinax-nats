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

# The client that provides functionality related to acknowledging a JetStream message.
public isolated client class JetStreamCaller {

    # Acknowledges a JetStream messages received from a Consumer, indicating
    # the message should not be received again later.
    # ```ballerina
    # check jetStreamCaller->ack();
    # ```
    #
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function ack() =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.message.MessageUtils"
    } external;

    # Acknowledges a JetStream message has been received but indicates 
    # that the message is not completely processed and should be sent again later.
    # ```ballerina
    # check jetStreamCaller->nak();
    # ```
    #
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function nak() =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.message.MessageUtils"
    } external;

    # Indicates that this message is being worked on and reset redelivery timer 
    # in the server.
    # ```ballerina
    # check jetStreamCaller->inProgress();
    # ```
    #
    # + return -  `()` or else a `nats:Error` if an error is occurred
    isolated remote function inProgress() =
    @java:Method {
        'class: "io.ballerina.stdlib.nats.jetstream.message.MessageUtils"
    } external;
}
