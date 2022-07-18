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
string receivedStringMaxLengthConstraintError = "";
string receivedIntMaxValueConstraintError = "";
string receivedFloatMinValueConstraintError = "";
string receivedNumberMaxValueConstraintError = "";
string receivedArrayMaxLengthConstraintError = "";
Child? receivedValidRecordValue = ();

string validStringSubject = "valid.string.subject";
string maxLengthStringSubject = "max.length.string.subject";
string maxValueIntSubject = "max.value,int.subject";
string minValueFloatSubject = "min.value.float.subject";
string maxValueNumberSubject = "max.value.number.subject";
string maxLengthArraySubject = "max.length.array.subject";
string validRecordSubject = "valid.record.subject";

@test:Config {}
function testValidStringConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(validStringService);
    check sub.'start();
    Message|error result = reqClient->requestMessage({content: "Hello".toBytes(), subject: validStringSubject}, 2);
    if result is error {
        test:assertFail(result.message());
    } else {
        test:assertEquals(strings:fromBytes(result.content), "Hello Back!");
        test:assertEquals(receivedValidStringValue, "Hello");
    }
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testMaxLengthStringConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(maxLengthStringService);
    check sub.'start();
    check reqClient->publishMessage({content: "Hello World!!!".toBytes(), subject: maxLengthStringSubject});
    runtime:sleep(3);
    test:assertEquals(receivedStringMaxLengthConstraintError, "Failed to validate");
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testMaxValueIntConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(maxValueIntService);
    check sub.'start();
    check reqClient->publishMessage({content: 1099.toString(), subject: maxValueIntSubject});
    runtime:sleep(3);
    test:assertEquals(receivedIntMaxValueConstraintError, "Failed to validate");
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testMinValueFloatConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(minValueFloatService);
    check sub.'start();
    check reqClient->publishMessage({content: 1.99.toString(), subject: minValueFloatSubject});
    runtime:sleep(3);
    test:assertEquals(receivedFloatMinValueConstraintError, "Failed to validate");
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testMaxValueNumberConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(maxValueNumberService);
    check sub.'start();
    check reqClient->publishMessage({content: 1123.595.toString(), subject: maxValueNumberSubject});
    runtime:sleep(3);
    test:assertEquals(receivedNumberMaxValueConstraintError, "Failed to validate");
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testMaxLengthArrayConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(maxLengthArrayService);
    check sub.'start();
    check reqClient->publishMessage({content: [1, 2, 3, 4, 5, 6, 7].toString(), subject: maxLengthArraySubject});
    runtime:sleep(3);
    test:assertEquals(receivedArrayMaxLengthConstraintError, "Failed to validate");
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

@test:Config {}
function testValidRecordConstraint() returns error? {
    Client reqClient = check new(DEFAULT_URL);
    Listener sub = check new(DEFAULT_URL);
    check sub.attach(validRecordService);
    check sub.'start();
    Message|error result = reqClient->requestMessage({content: {name: "PhilDunphy", age: 12}.toString(), subject: validRecordSubject}, 2);
    if result is error {
        test:assertFail(result.message());
    } else {
        test:assertEquals(strings:fromBytes(result.content), "Hello Back!");
        test:assertEquals(receivedValidRecordValue, {name: "PhilDunphy", age: 12});
    }
    checkpanic reqClient.close();
    check sub.gracefulStop();
}

Service validStringService =
@ServiceConfig {
    subject: validStringSubject
}
service object {
    remote function onRequest(StringConstraintMessage msg) returns string {
        log:printInfo("Message Received: " + msg.toString());
        receivedValidStringValue = msg.content;
        return "Hello Back!";
    }
};

Service maxLengthStringService =
@ServiceConfig {
    subject: maxLengthStringSubject
}
service object {
    remote function onMessage(StringConstraintMessage msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedStringMaxLengthConstraintError = err.message();
        }
    }
};

Service maxValueIntService =
@ServiceConfig {
    subject: maxValueIntSubject
}
service object {
    remote function onMessage(IntConstraintMessage msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedIntMaxValueConstraintError = err.message();
        }
    }
};

Service minValueFloatService =
@ServiceConfig {
    subject: minValueFloatSubject
}
service object {
    remote function onMessage(Price msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedFloatMinValueConstraintError = err.message();
        }
    }
};

Service maxValueNumberService =
@ServiceConfig {
    subject: maxValueNumberSubject
}
service object {
    remote function onMessage(Weight msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedNumberMaxValueConstraintError = err.message();
        }
    }
};

Service maxLengthArrayService =
@ServiceConfig {
    subject: maxLengthArraySubject
}
service object {
    remote function onMessage(NameList msg) {
        log:printInfo("Message Received: " + msg.toString());
    }

    remote function onError(Message message, Error err) {
        log:printInfo("Error Received: " + err.message());
        if err is PayloadValidationError {
            receivedArrayMaxLengthConstraintError = err.message();
        }
    }
};

Service validRecordService =
@ServiceConfig {
    subject: validRecordSubject
}
service object {
    remote function onRequest(Child msg) returns string {
        log:printInfo("Message Received: " + msg.toString());
        receivedValidRecordValue = msg;
        return "Hello Back!";
    }
};
