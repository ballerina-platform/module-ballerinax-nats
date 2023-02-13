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
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/test;
import ballerina/constraint;
import ballerina/log;
import ballerina/lang.'string as strings;
import ballerina/lang.runtime;

public type StringConstraintMessage record {|
    *AnydataMessage;
    @constraint:String {
        minLength: 2,
        maxLength: 10
    }
    string content;
|};

public type IntConstraintMessage record {|
    @constraint:Int {
        maxValue: 100,
        minValue: 10
    }
    int content;
    string subject;
    string replyTo?;
|};

@constraint:Float {
    maxValue: 100,
    minValue: 10
}
public type Price float;

@constraint:Number {
    maxValue: 100,
    minValue: 10
}
public type Weight decimal;

@constraint:Array {
    minLength: 2,
    maxLength: 5
}
public type NameList int[];

public type Child record {|
    @constraint:String {
        length: 10
    }
    string name;
    int age;
|};

string receivedValidStringValue = "";
string receivedDisabledValidationValue = "";
string receivedStringMaxLengthConstraintError = "";
string receivedIntMaxValueConstraintError = "";
string receivedFloatMinValueConstraintError = "";
string receivedNumberMaxValueConstraintError = "";
string receivedArrayMaxLengthConstraintError = "";
string receivedDisabledValidationConstraintError = "";
Child? receivedValidRecordValue = ();

string validStringSubject = "valid.string.subject";
string maxLengthStringSubject = "max.length.string.subject";
string maxValueIntSubject = "max.value,int.subject";
string minValueFloatSubject = "min.value.float.subject";
string maxValueNumberSubject = "max.value.number.subject";
string maxLengthArraySubject = "max.length.array.subject";
string validRecordSubject = "valid.record.subject";
string disabledValidationSubject = "disabled.validation.subject";

@test:Config {}
function testValidStringConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Message|error result = reqClient->requestMessage({content: "Hello".toBytes(), subject: validStringSubject}, 2);
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedValidStringValue !is "" && result !is error {
            test:assertEquals(receivedValidStringValue, "Hello", msg = "Message received does not match.");
            test:assertEquals(strings:fromBytes(result.content), "Hello Back!");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testMaxLengthStringConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    check reqClient->publishMessage({content: "Hello World!!!".toBytes(), subject: maxLengthStringSubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedStringMaxLengthConstraintError !is "" {
            test:assertEquals(receivedStringMaxLengthConstraintError,
                "Validation failed for '$.content:maxLength' constraint(s).", msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testMaxValueIntConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    check reqClient->publishMessage({content: 1099.toString(), subject: maxValueIntSubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedIntMaxValueConstraintError !is "" {
            test:assertEquals(receivedIntMaxValueConstraintError,
                "Validation failed for '$.content:maxValue' constraint(s).", msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testMinValueFloatConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    check reqClient->publishMessage({content: 1.99.toString(), subject: minValueFloatSubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedFloatMinValueConstraintError !is "" {
            test:assertEquals(receivedFloatMinValueConstraintError, "Validation failed for '$:minValue' constraint(s).");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testMaxValueNumberConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    check reqClient->publishMessage({content: 1123.595.toString(), subject: maxValueNumberSubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedNumberMaxValueConstraintError !is "" {
            test:assertEquals(receivedNumberMaxValueConstraintError,
                "Validation failed for '$:maxValue' constraint(s).", msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testMaxLengthArrayConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    check reqClient->publishMessage({content: [1, 2, 3, 4, 5, 6, 7].toString(), subject: maxLengthArraySubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedArrayMaxLengthConstraintError !is "" {
            test:assertEquals(receivedArrayMaxLengthConstraintError,
                "Validation failed for '$:maxLength' constraint(s).", msg = "Message received does not match.");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@test:Config {}
function testValidRecordConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Message|error result = reqClient->requestMessage({content: {name: "PhilDunphy", age: 12}.toString(), subject: validRecordSubject}, 2);
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedValidRecordValue !is () && result !is error {
            test:assertEquals(receivedValidRecordValue, {name: "PhilDunphy", age: 12}, msg = "Message received does not match.");
            test:assertEquals(strings:fromBytes(result.content), "Hello Back!");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    check reqClient.close();
}

@ServiceConfig {
    subject: validStringSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onRequest(StringConstraintMessage msg) returns string {
        log:printInfo("Message Received: " + msg.toString());
        receivedValidStringValue = msg.content;
        return "Hello Back!";
    }
}

@ServiceConfig {
    subject: maxLengthStringSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onMessage(StringConstraintMessage msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedStringMaxLengthConstraintError = err.message();
        }
    }
}

@ServiceConfig {
    subject: maxValueIntSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onMessage(IntConstraintMessage msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedIntMaxValueConstraintError = err.message();
        }
    }
}

@ServiceConfig {
    subject: minValueFloatSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onMessage(Price msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedFloatMinValueConstraintError = err.message();
        }
    }
}

@ServiceConfig {
    subject: maxValueNumberSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onMessage(Weight msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedNumberMaxValueConstraintError = err.message();
        }
    }
}

@ServiceConfig {
    subject: maxLengthArraySubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onMessage(NameList msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedArrayMaxLengthConstraintError = err.message();
        }
    }
}

@ServiceConfig {
    subject: validRecordSubject
}
service Service on new Listener(DEFAULT_URL) {
    remote function onRequest(Child msg) returns string {
        log:printInfo("Message Received: " + msg.toString());
        receivedValidRecordValue = msg;
        return "Hello Back!";
    }
}

@test:Config {}
function testMaxLengthStringConstraintWithValidationDisabled() returns error? {
    Client reqClient = check new("nats://localhost:4222", {validation: false});
    check reqClient->publishMessage({content: "Hello World!!!".toBytes(), subject: disabledValidationSubject});
    int timeoutInSeconds = 120;
    // Test fails in 2 minutes if it is failed to receive the message
    while timeoutInSeconds > 0 {
        if receivedDisabledValidationValue !is "" {
            test:assertEquals(receivedDisabledValidationValue, "Hello World!!!", msg = "Message received does not match.");
            test:assertEquals(receivedDisabledValidationConstraintError, "");
            break;
        } else {
            runtime:sleep(1);
            timeoutInSeconds = timeoutInSeconds - 1;
        }
    }
    if timeoutInSeconds == 0 {
        test:assertFail("Failed to receive the message for 2 minutes.");
    }
    checkpanic reqClient.close();
}

@ServiceConfig {
    subject: disabledValidationSubject
}
service Service on new Listener("nats://localhost:4222", {validation: false}) {
    remote function onMessage(StringConstraintMessage msg) {
        log:printInfo("Message Received: " + msg.toString());
        receivedDisabledValidationValue = msg.content;
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        receivedDisabledValidationConstraintError = err.message();
    }
}
