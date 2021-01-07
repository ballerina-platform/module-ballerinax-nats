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
import org.ballerinalang.nats.connection.DefaultErrorListener;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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
    private static final BString RETRY_CONFIG = StringUtils.fromString("retryConfig");
    private static final BString PING_CONFIG = StringUtils.fromString("ping");
    private static final BString AUTH_CONFIG = StringUtils.fromString("auth");
    private static final BString USERNAME = StringUtils.fromString("username");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString TOKEN = StringUtils.fromString("token");

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

                @SuppressWarnings("unchecked")
                BMap<BString, Object> retryConfig = ((BMap) connectionConfig).getMapValue(RETRY_CONFIG);

                // Add max reconnect.
                opts.maxReconnects(Math.toIntExact(((BMap) retryConfig).getIntValue(MAX_RECONNECT)));

                // Add reconnect wait.
                opts.reconnectWait(Duration.ofSeconds(((BMap) retryConfig).getIntValue(RECONNECT_WAIT)));

                // Add connection timeout.
                opts.connectionTimeout(Duration.ofSeconds(((BMap) retryConfig).getIntValue(CONNECTION_TIMEOUT)));

                @SuppressWarnings("unchecked")
                BMap<BString, Object> pingConfig = ((BMap) connectionConfig).getMapValue(PING_CONFIG);

                // Add ping interval.
                opts.pingInterval(Duration.ofMinutes(((BMap) pingConfig).getIntValue(PING_INTERVAL)));

                // Add max ping out.
                opts.maxPingsOut(Math.toIntExact(((BMap) pingConfig).getIntValue(MAX_PINGS_OUT)));

                // Add inbox prefix.
                opts.inboxPrefix(((BMap) connectionConfig).getStringValue(INBOX_PREFIX).getValue());

                // Add NATS connection listener.
                //opts.connectionListener(new DefaultConnectionListener());

                // Add NATS error listener.
                if (((BMap) connectionConfig).getBooleanValue(ENABLE_ERROR_LISTENER)) {
                    ErrorListener errorListener = new DefaultErrorListener();
                    opts.errorListener(errorListener);
                }

                @SuppressWarnings("unchecked")
                Object authConfig = ((BMap) connectionConfig).getObjectValue(AUTH_CONFIG);

                if (TypeUtils.getType(authConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {
                    if (((BMap) connectionConfig).containsKey(USERNAME)
                            && ((BMap) connectionConfig).containsKey(PASSWORD)) {
                        opts.userInfo(((BMap) authConfig).getStringValue(USERNAME).getValue().toCharArray(),
                                      ((BMap) authConfig).getStringValue(PASSWORD).getValue().toCharArray());
                    } else if (((BMap) connectionConfig).containsKey(TOKEN)) {
                        opts.token(((BMap) authConfig).getStringValue(TOKEN).getValue().toCharArray());
                    }
                }

                // Add noEcho.
                if (((BMap) connectionConfig).getBooleanValue(NO_ECHO)) {
                    opts.noEcho();
                }

                BMap secureSocket = ((BMap) connectionConfig).getMapValue(Constants.CONNECTION_CONFIG_SECURE_SOCKET);
                if (secureSocket != null) {
                    SSLContext sslContext = getSSLContext(secureSocket);
                    opts.sslContext(sslContext);
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

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    private static SSLContext getSSLContext(BMap secureSocket) {
        try {
            BMap cryptoKeyStore = secureSocket.getMapValue(Constants.CONNECTION_KEYSTORE);
            KeyManagerFactory keyManagerFactory = null;
            if (cryptoKeyStore != null) {
                char[] keyPassphrase = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PASS).getValue().toCharArray();
                String keyFilePath = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
                KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
                if (keyFilePath != null) {
                    try (FileInputStream keyFileInputStream = new FileInputStream(keyFilePath)) {
                        keyStore.load(keyFileInputStream, keyPassphrase);
                    }
                } else {
                    throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION +
                                                        "Keystore path doesn't exist.");
                }
                keyManagerFactory =
                        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyPassphrase);
            }

            BMap cryptoTrustStore = secureSocket.getMapValue(Constants.CONNECTION_TRUSTORE);
            TrustManagerFactory trustManagerFactory = null;
            if (cryptoTrustStore != null) {
                KeyStore trustStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
                char[] trustPassphrase = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PASS).getValue()
                        .toCharArray();
                String trustFilePath = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
                if (trustFilePath != null) {
                    try (FileInputStream trustFileInputStream = new FileInputStream(trustFilePath)) {
                        trustStore.load(trustFileInputStream, trustPassphrase);
                    }
                } else {
                    throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION
                                                        + "Truststore path doesn't exist.");
                }
                trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
            }

            String tlsVersion = secureSocket.getStringValue(Constants.CONNECTION_PROTOCOL).getValue();
            SSLContext sslContext = SSLContext.getInstance(tlsVersion);
            sslContext.init(keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                            trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null, null);
            return sslContext;
        } catch (FileNotFoundException e) {
            throw Utils
                    .createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "File not found error, "
                                             + e.getMessage());
        } catch (CertificateException e) {
            throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "Certificate error, "
                                                + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "Algorithm error, "
                                                + e.getMessage());
        } catch (IOException e) {
            throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "IO error, "
                                                + e.getMessage());
        } catch (KeyStoreException e) {
            throw Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "Keystore error, "
                                                + e.getMessage());
        } catch (UnrecoverableKeyException e) {
            throw Utils.createNatsError(
                    Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "The key in the keystore cannot be recovered.");
        } catch (KeyManagementException e) {
            throw Utils
                    .createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "Key management error, "
                                             + e.getMessage());
        }
    }
}
