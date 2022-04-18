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

public type Person record {|
    readonly string name;
    int age;
|};

public type IntMessage record {|
    int content;
    string subject;
    string replyTo?;
|};

public type FloatMessage record {|
    float content;
    string subject;
    string replyTo?;
|};

public type DecimalMessage record {|
    decimal content;
    string subject;
    string replyTo?;
|};

public type BooleanMessage record {|
    boolean content;
    string subject;
    string replyTo?;
|};

public type StringMessage record {|
    string content;
    string subject;
    string replyTo?;
|};

public type PersonMessage record {|
    Person content;
    string subject;
    string replyTo?;
|};

public type MapMessage record {|
    map<PersonMessage> content;
    string subject;
    string replyTo?;
|};

public type XmlMessage record {|
    xml content;
    string subject;
    string replyTo?;
|};

public type JsonMessage record {|
    json content;
    string subject;
    string replyTo?;
|};

public type TableMessage record {|
    table<Person> content;
    string subject;
    string replyTo?;
|};

// BytesMessage -- Already defined in module

Person personRecord1 = {
    name: "Adam",
    age: 21
};

Person personRecord2 = {
    name: "Peter",
    age: 34
};

Person personRecord3 = {
    name: "Will",
    age: 27
};

map<Person> personMap = {
    "P1": personRecord1,
    "P2": personRecord2,
    "P3": personRecord3
};

json jsonData = personMap.toJson();
