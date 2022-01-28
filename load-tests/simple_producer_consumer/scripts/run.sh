#!/bin/bash -e
# Copyright 2022 WSO2 Inc. (http://wso2.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# ----------------------------------------------------------------------------
# Execution script for ballerina performance tests
# ----------------------------------------------------------------------------
set -e
source base-scenario.sh

echo "----------Downloading Ballerina----------"
wget https://dist.ballerina.io/downloads/swan-lake-beta6/ballerina-linux-installer-x64-swan-lake-beta6.deb

echo "----------Setting Up Ballerina----------"
sudo dpkg -i ballerina-linux-installer-x64-swan-lake-beta6.deb

echo "----------Running Load Test----------"
bal run $scriptsDir/load_test/ -- "Kafka Simple Producer Consumer" "$resultsDir/summary.csv"
