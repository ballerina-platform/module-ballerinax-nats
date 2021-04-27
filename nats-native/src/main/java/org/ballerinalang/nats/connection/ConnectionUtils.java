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
import io.ballerina.runtime.api.values.BDecimal;
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
import java.security.SecureRandom;
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
    private static final BString RECONNECT_WAIT = StringUtils.fromString("reconnectWait");
    private static final BString CONNECTION_NAME = StringUtils.fromString("connectionName");
    private static final BString MAX_RECONNECT = StringUtils.fromString("maxReconnect");
    private static final BString CONNECTION_TIMEOUT = StringUtils.fromString("connectionTimeout");
    private static final BString PING_INTERVAL = StringUtils.fromString("pingInterval");
    private static final BString MAX_PINGS_OUT = StringUtils.fromString("maxPingsOut");
    private static final BString INBOX_PREFIX = StringUtils.fromString("inboxPrefix");
    private static final BString NO_ECHO = StringUtils.fromString("noEcho");
    private static final BString RETRY_CONFIG = StringUtils.fromString("retryConfig");
    private static final BString PING_CONFIG = StringUtils.fromString("ping");
    private static final BString AUTH_CONFIG = StringUtils.fromString("auth");
    private static final BString USERNAME = StringUtils.fromString("username");
    private static final BString PASSWORD = StringUtils.fromString("password");
    private static final BString TOKEN = StringUtils.fromString("token");

    public static Connection getNatsConnection(Object urlString, BMap connectionConfig)
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

        opts.connectionName(connectionConfig.getStringValue(CONNECTION_NAME).getValue());

        // Retry configs
        if (connectionConfig.containsKey(RETRY_CONFIG)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> retryConfig = connectionConfig.getMapValue(RETRY_CONFIG);
            opts.maxReconnects(Math.toIntExact(retryConfig.getIntValue(MAX_RECONNECT)));
            opts.reconnectWait(Duration.ofSeconds(((BDecimal) retryConfig.get(RECONNECT_WAIT)).intValue()));
            opts.connectionTimeout(Duration.ofSeconds(
                    ((BDecimal) retryConfig.get(CONNECTION_TIMEOUT)).intValue()));
        }

        // Ping configs
        if (connectionConfig.containsKey(PING_CONFIG)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> pingConfig = connectionConfig.getMapValue(PING_CONFIG);
            opts.pingInterval(Duration.ofSeconds(((BDecimal) pingConfig.get(PING_INTERVAL)).intValue()));
            opts.maxPingsOut(Math.toIntExact(pingConfig.getIntValue(MAX_PINGS_OUT)));
        }

        opts.inboxPrefix(connectionConfig.getStringValue(INBOX_PREFIX).getValue());
        opts.oldRequestStyle();

        // Auth configs
        if (connectionConfig.containsKey(AUTH_CONFIG)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> authConfig = connectionConfig.getMapValue(AUTH_CONFIG);
            if (authConfig.containsKey(USERNAME) && authConfig.containsKey(PASSWORD)) {
                // Credentials based auth
                opts.userInfo(authConfig.getStringValue(USERNAME).getValue().toCharArray(),
                             authConfig.getStringValue(PASSWORD).getValue().toCharArray());
            } else if (authConfig.containsKey(TOKEN)) {
                // Token based auth
                opts.token(authConfig.getStringValue(TOKEN).getValue().toCharArray());
            }
        }

        // Add noEcho.
        if (connectionConfig.getBooleanValue(NO_ECHO)) {
            opts.noEcho();
        }

        // Secure socket configs
        if (connectionConfig.containsKey(Constants.CONNECTION_CONFIG_SECURE_SOCKET)) {
            BMap secureSocket = connectionConfig.getMapValue(Constants.CONNECTION_CONFIG_SECURE_SOCKET);
            SSLContext sslContext = getSSLContext(secureSocket);
            opts.sslContext(sslContext);
        }
        return Nats.connect(opts.build());
    }

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    private static SSLContext getSSLContext(BMap<BString, Object> secureSocket)
            throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException,
                   UnrecoverableKeyException, KeyManagementException {
        // Keystore
        KeyManagerFactory keyManagerFactory = null;
        if (secureSocket.containsKey(Constants.CONNECTION_KEYSTORE)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> cryptoKeyStore =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_KEYSTORE);
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

        // Truststore
        @SuppressWarnings("unchecked")
        BMap<BString, Object> cryptoTrustStore =
                (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_TRUSTORE);
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
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // protocol
        SSLContext sslContext;
        if (secureSocket.containsKey(Constants.CONNECTION_PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_PROTOCOL);
            String protocol = protocolRecord.getStringValue(Constants.CONNECTION_PROTOCOL_NAME).getValue();
            sslContext = SSLContext.getInstance(protocol);
        } else {
            sslContext = SSLContext.getDefault();
        }
        sslContext.init(keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                         trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }
}
