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

package org.ballerinalang.nats.connection;

import io.ballerina.runtime.api.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

import java.io.FileInputStream;
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
 * Creates the NATS connection from the user given connection configurations.
 */
public class ConnectionUtils {
    private static final BString RECONNECT_WAIT = StringUtils.fromString("reconnectWaitInSeconds");
    private static final BString CONNECTION_NAME = StringUtils.fromString("connectionName");
    private static final BString MAX_RECONNECT = StringUtils.fromString("maxReconnect");
    private static final BString CONNECTION_TIMEOUT = StringUtils.fromString("connectionTimeoutInSeconds");
    private static final BString PING_INTERVAL = StringUtils.fromString("pingIntervalInMinutes");
    private static final BString MAX_PINGS_OUT = StringUtils.fromString("maxPingsOut");
    private static final BString INBOX_PREFIX = StringUtils.fromString("inboxPrefix");
    private static final BString NO_ECHO = StringUtils.fromString("noEcho");
    private static final BString RETRY_CONFIG = StringUtils.fromString("retryConfig");
    private static final BString PING_CONFIG = StringUtils.fromString("ping");
    private static final BString AUTH_CONFIG = StringUtils.fromString("auth");
    private static final BString USERNAME = StringUtils.fromString("username");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString TOKEN = StringUtils.fromString("token");

    public static Connection getNatsConnection(Object urlString, Object connectionConfig)
            throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                   KeyManagementException, IOException, InterruptedException {
        Options.Builder opts = new Options.Builder();
        if (TypeUtils.getType(urlString).getTag() == TypeTags.ARRAY_TAG) {
            String[] serverUrls = ((BArray) urlString).getStringArray();
            opts.servers(serverUrls);
        } else {
            String[] serverUrls = {((BString) urlString).getValue()};
            opts.servers(serverUrls);
        }

        if (TypeUtils.getType(connectionConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {

            opts.connectionName(((BMap) connectionConfig).getStringValue(CONNECTION_NAME).getValue());

            // Retry configs
            @SuppressWarnings("unchecked")
            BMap<BString, Object> retryConfig = ((BMap) connectionConfig).getMapValue(RETRY_CONFIG);
            opts.maxReconnects(Math.toIntExact(((BMap) retryConfig).getIntValue(MAX_RECONNECT)));
            opts.reconnectWait(Duration.ofSeconds(((BMap) retryConfig).getIntValue(RECONNECT_WAIT)));
            opts.connectionTimeout(Duration.ofSeconds(((BMap) retryConfig).getIntValue(CONNECTION_TIMEOUT)));

            // Ping configs
            @SuppressWarnings("unchecked")
            BMap<BString, Object> pingConfig = ((BMap) connectionConfig).getMapValue(PING_CONFIG);
            opts.pingInterval(Duration.ofMinutes(((BMap) pingConfig).getIntValue(PING_INTERVAL)));
            opts.maxPingsOut(Math.toIntExact(((BMap) pingConfig).getIntValue(MAX_PINGS_OUT)));

            opts.inboxPrefix(((BMap) connectionConfig).getStringValue(INBOX_PREFIX).getValue());
            opts.oldRequestStyle();

            // Auth configs
            @SuppressWarnings("unchecked")
            Object authConfig = ((BMap) connectionConfig).getObjectValue(AUTH_CONFIG);
            if (TypeUtils.getType(authConfig).getTag() == TypeTags.RECORD_TYPE_TAG) {
                if (((BMap) authConfig).containsKey(USERNAME) && ((BMap) authConfig).containsKey(PASSWORD)) {
                    // Credentials based auth
                    opts.userInfo(((BMap) authConfig).getStringValue(USERNAME).getValue().toCharArray(),
                                  ((BMap) authConfig).getStringValue(PASSWORD).getValue().toCharArray());
                } else if (((BMap) authConfig).containsKey(TOKEN)) {
                    // Token based auth
                    opts.token(((BMap) authConfig).getStringValue(TOKEN).getValue().toCharArray());
                }
            }

            // Add noEcho.
            if (((BMap) connectionConfig).getBooleanValue(NO_ECHO)) {
                opts.noEcho();
            }

            // Secure socket configs
            BMap secureSocket = ((BMap) connectionConfig).getMapValue(Constants.CONNECTION_CONFIG_SECURE_SOCKET);
            if (secureSocket != null) {
                SSLContext sslContext = getSSLContext(secureSocket);
                opts.sslContext(sslContext);
            }
        }
        return Nats.connect(opts.build());
    }

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    private static SSLContext getSSLContext(BMap secureSocket)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                   UnrecoverableKeyException, KeyManagementException {
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
                                                    + "truststore path doesn't exist.");
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
    }
}
