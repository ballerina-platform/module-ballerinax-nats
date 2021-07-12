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

package io.ballerina.stdlib.nats.basic.client;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.ballerina.stdlib.nats.connection.ConnectionUtils;
import io.ballerina.stdlib.nats.observability.NatsMetricsReporter;
import io.nats.client.Connection;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * Initialize NATS producer using the connection.
 *
 * @since 0.995
 */
public class Init {

    public static Object clientInit(BObject clientObj, Object url, BMap<BString, Object> connectionConfig) {
        Connection natsConnection;
        try {
            natsConnection = ConnectionUtils.getNatsConnection(url, connectionConfig);
        } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException |
                UnrecoverableKeyException | InterruptedException | IOException e) {
            String errorMsg = "error occurred while setting up the connection. " +
                    (e.getMessage() != null ? e.getMessage() : "");
            return Utils.createNatsError(errorMsg);
        }
        clientObj.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(natsConnection));
        clientObj.addNativeData(Constants.NATS_CONNECTION, natsConnection);
        ((NatsMetricsReporter) clientObj.getNativeData(Constants.NATS_METRIC_UTIL)).reportNewClient();
        return null;
    }
}
