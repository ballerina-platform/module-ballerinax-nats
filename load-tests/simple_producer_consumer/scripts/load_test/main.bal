// Copyright (c) 2022, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import ballerina/log;
import ballerina/http;
import ballerina/lang.runtime;
import ballerina/io;
import ballerina/time;

public function main(string label, string output_csv_path) returns error? {
    http:Client loadTestClient = check new ("http://bal.perf.test");

    boolean response = check loadTestClient->get("/nats/publish");
    if response {
        log:printInfo("Started producing messages");
    } else {
        log:printError("Error occured while producing messages");
    }

    map<string> testResults = {};

    boolean finished = false;
    while !finished {
        boolean|map<string>|error res = loadTestClient->get("/nats/getResults");
        if res is error {
            log:printError("Error occurred", res);
        } else if res is boolean {
            log:printInfo(res.toString());
        } else {
            finished = true;
            testResults = res;
        }
        runtime:sleep(60);
    }
    int errorCount = check int:fromString(testResults.get("errorCount"));
    decimal time = check decimal:fromString(testResults.get("time"));
    int sentCount = check int:fromString(testResults.get("sentCount"));
    int receivedCount = check int:fromString(testResults.get("receivedCount"));
    any[] results = [label, sentCount, <float>time/<float>receivedCount, 0, 0, 0, 0, 0, 0, <float>errorCount/<float>sentCount,
        <float>receivedCount/<float>time, 0, 0, time:utcNow()[0], 0, 1];
    check writeResultsToCsv(results, output_csv_path);
}

function writeResultsToCsv(any[] results, string output_path) returns error? {
    string[][] summary_data = check io:fileReadCsv(output_path);
    string[] final_results = [];
    foreach var result in results {
        final_results.push(result.toString());
    }
    summary_data.push(final_results);
    check io:fileWriteCsv(output_path, summary_data);
}
