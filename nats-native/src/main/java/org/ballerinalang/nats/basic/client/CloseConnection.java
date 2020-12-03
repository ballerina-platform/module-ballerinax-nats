/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.nats.basic.client;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BObject;
import io.nats.client.Connection;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsTracingUtil;


/**
 * Extern function to close logical connection in producer.
 *
 * @since 0.995
 */
public class CloseConnection {

    public static Object closeConnection(Environment environment, BObject clientObject) {
        NatsTracingUtil.traceResourceInvocation(environment, clientObject);
        Connection connection = (Connection) clientObject.getNativeData(Constants.NATS_CONNECTION);
        try {
            connection.close();
        } catch (InterruptedException e) {
            return Utils.createNatsError("Error while closing the connection: " + e.getMessage());
        }
        ((NatsMetricsReporter) clientObject.getNativeData(Constants.NATS_METRIC_UTIL)).reportClientClose();
        return null;
    }
}
