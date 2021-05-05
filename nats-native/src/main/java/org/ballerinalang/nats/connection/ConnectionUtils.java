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

import java.io.BufferedInputStream;
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
import java.util.Objects;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
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
            throws IOException, CertificateException, KeyStoreException,
            UnrecoverableKeyException, KeyManagementException, NoSuchAlgorithmException {
        // Keystore
        String keyFilePath = null;
        char[] keyPassphrase = null;
        char[] trustPassphrase;
        String trustFilePath;
        if (secureSocket.containsKey(Constants.CONNECTION_KEYSTORE)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> cryptoKeyStore =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_KEYSTORE);
            keyPassphrase = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PASS).getValue().toCharArray();
            keyFilePath = cryptoKeyStore.getStringValue(Constants.KEY_STORE_PATH).getValue();
        }

        // Truststore
        @SuppressWarnings("unchecked")
        BMap<BString, Object> cryptoTrustStore =
                (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_TRUSTORE);
        trustPassphrase = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PASS).getValue()
                .toCharArray();
        trustFilePath = cryptoTrustStore.getStringValue(Constants.KEY_STORE_PATH).getValue();

        // protocol
        String protocol = null;
        if (secureSocket.containsKey(Constants.CONNECTION_PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.CONNECTION_PROTOCOL);
            protocol = protocolRecord.getStringValue(Constants.CONNECTION_PROTOCOL_NAME).getValue();
        }
        SSLContext sslContext = createSSLContext(trustFilePath, trustPassphrase, keyFilePath, keyPassphrase, protocol);
        return sslContext;
    }


    public static KeyStore loadKeystore(String path, char[] pass) throws KeyStoreException, IOException,
            CertificateException, NoSuchAlgorithmException {
        KeyStore store = KeyStore.getInstance(Constants.KEY_STORE_TYPE);

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(path))) {
            store.load(in, pass);
        }
        return store;
    }

    public static KeyManager[] createTestKeyManagers(String keyStorePath, char[] keyStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException,
            UnrecoverableKeyException {
        KeyStore store = loadKeystore(keyStorePath, keyStorePass);
        KeyManagerFactory factory;
        factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(store, keyStorePass);
        return factory.getKeyManagers();
    }

    public static TrustManager[] createTestTrustManagers(String trustStorePath, char[] trustStorePass)
            throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        KeyStore store = loadKeystore(trustStorePath, trustStorePass);
        TrustManagerFactory factory;
        factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(store);
        return factory.getTrustManagers();
    }

    public static SSLContext createSSLContext(String trustStorePath, char[] trustStorePass, String keyStorePath,
                                              char[] keyStorePass, String protocol)
            throws UnrecoverableKeyException, CertificateException, KeyStoreException, IOException,
            NoSuchAlgorithmException, KeyManagementException {

        SSLContext ctx;
        ctx = SSLContext.getInstance(Objects.requireNonNullElse(protocol, Options.DEFAULT_SSL_PROTOCOL));
        ctx.init(keyStorePath != null ? createTestKeyManagers(keyStorePath, keyStorePass) : null,
                createTestTrustManagers(trustStorePath, trustStorePass), new SecureRandom());
        return ctx;
    }
}
