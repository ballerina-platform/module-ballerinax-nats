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

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.ErrorListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.connection.DefaultConnectionListener;
import org.ballerinalang.nats.connection.DefaultErrorListener;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;

import java.io.IOException;
import java.time.Duration;

/**
 * Initialize NATS producer using the connection.
 *
 * @since 0.995
 */
public class Init {

    private static final BString RECONNECT_WAIT = StringUtils.fromString("reconnectWaitInSeconds");
    private static final BString CONNECTION_NAME = StringUtils.fromString("connectionName");
    private static final BString MAX_RECONNECT = StringUtils.fromString("maxReconnect");
    private static final BString CONNECTION_TIMEOUT = StringUtils.fromString("connectionTimeoutInSeconds");
    private static final BString PING_INTERVAL = StringUtils.fromString("pingIntervalInMinutes");
    private static final BString MAX_PINGS_OUT = StringUtils.fromString("maxPingsOut");
    private static final BString INBOX_PREFIX = StringUtils.fromString("inboxPrefix");
    private static final BString NO_ECHO = StringUtils.fromString("noEcho");
    private static final BString ENABLE_ERROR_LISTENER = StringUtils.fromString("enableErrorListener");

    public static void clientInit(BObject clientObj, Object urlString, Object connectionConfig) {

        Options.Builder opts = new Options.Builder();
        try {
            if (TypeUtils.getType(urlString).getTag() == TypeTags.ARRAY_TAG) {
                String[] serverUrls = ((BArray) urlString).getStringArray();
                opts.servers(serverUrls);
            } else {
                String[] serverUrls = {((BString) urlString).getValue()};
                opts.servers(serverUrls);
            }

            if (TypeUtils.getType(connectionConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {

                // Add connection name.
                opts.connectionName(((BMap) connectionConfig).getStringValue(CONNECTION_NAME).getValue());

                // Add max reconnect.
                opts.maxReconnects(Math.toIntExact(((BMap) connectionConfig).getIntValue(MAX_RECONNECT)));

                // Add reconnect wait.
                opts.reconnectWait(Duration.ofSeconds(((BMap) connectionConfig).getIntValue(RECONNECT_WAIT)));

                // Add connection timeout.
                opts.connectionTimeout(Duration.ofSeconds(((BMap) connectionConfig).getIntValue(CONNECTION_TIMEOUT)));

                // Add ping interval.
                opts.pingInterval(Duration.ofMinutes(((BMap) connectionConfig).getIntValue(PING_INTERVAL)));

                // Add max ping out.
                opts.maxPingsOut(Math.toIntExact(((BMap) connectionConfig).getIntValue(MAX_PINGS_OUT)));

                // Add inbox prefix.
                opts.inboxPrefix(((BMap) connectionConfig).getStringValue(INBOX_PREFIX).getValue());

                // Add NATS connection listener.
                opts.connectionListener(new DefaultConnectionListener());

                // Add NATS error listener.
                if (((BMap) connectionConfig).getBooleanValue(ENABLE_ERROR_LISTENER)) {
                    ErrorListener errorListener = new DefaultErrorListener();
                    opts.errorListener(errorListener);
                }

                // Add noEcho.
                if (((BMap) connectionConfig).getBooleanValue(NO_ECHO)) {
                    opts.noEcho();
                }

            }

            Connection natsConnection = Nats.connect(opts.build());
            clientObj.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(natsConnection));
            clientObj.addNativeData(Constants.NATS_CONNECTION, natsConnection);
            ((NatsMetricsReporter) clientObj.getNativeData(Constants.NATS_METRIC_UTIL)).reportNewClient();
        } catch (IOException | InterruptedException e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_CONNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            String errorMsg = "Error while setting up a connection. " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            throw Utils.createNatsError(errorMsg);
        } catch (IllegalArgumentException e) {
            NatsMetricsReporter.reportError(NatsObservabilityConstants.CONTEXT_CONNECTION,
                                            NatsObservabilityConstants.ERROR_TYPE_CONNECTION);
            throw Utils.createNatsError(e.getMessage());
        }
    }
}
