// Copyright (c) 2023, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
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

import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

@display {
    label: "natsService"
}
service "demo.bbe" on subscription {

    remote function onMessage(nats:Message message) {
    }
}

@display {
    label: "natsService"
}
@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(nats:Message message) {
    }
}

nats:Client natsClient = check new(nats:DEFAULT_URL);
listener nats:JetStreamListener streamSubscription = new(natsClient);

@display {
    label: "natsService"
}
@nats:StreamServiceConfig {
    subject: "demo.bbe.*"
}
service nats:JetStreamService on streamSubscription {

    remote function onMessage(readonly & nats:JetStreamMessage message) {
    }
}
