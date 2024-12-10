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

package io.ballerina.stdlib.nats.connection;

import io.ballerina.runtime.api.types.TypeTags;
import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.utils.TypeUtils;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.crypto.nativeimpl.Decode;
import io.ballerina.stdlib.nats.Constants;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

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
            throws Exception {
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
            SSLContext sslContext = getSslContext(secureSocket);
            opts.sslContext(sslContext);
        }
        return Nats.connect(opts.build());
    }

    private static SSLContext getSslContext(BMap<BString, ?> secureSocket) throws Exception {
        // protocol
        String protocol = null;
        if (secureSocket.containsKey(Constants.PROTOCOL)) {
            @SuppressWarnings("unchecked")
            BMap<BString, Object> protocolRecord =
                    (BMap<BString, Object>) secureSocket.getMapValue(Constants.PROTOCOL);
            protocol = protocolRecord.getStringValue(Constants.PROTOCOL_NAME).getValue();
        }

        Object cert = secureSocket.get(Constants.CERT);
        @SuppressWarnings("unchecked")
        BMap<BString, BString> key = (BMap<BString, BString>) getBMapValueIfPresent(secureSocket, Constants.KEY);

        KeyManagerFactory kmf;
        TrustManagerFactory tmf;
        if (cert instanceof BString) {
            if (key != null) {
                if (key.containsKey(Constants.CERT_FILE)) {
                    BString certFile = key.get(Constants.CERT_FILE);
                    BString keyFile = key.get(Constants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, Constants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory((BString) cert);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        if (cert instanceof BMap) {
            BMap<BString, BString> trustStore = (BMap<BString, BString>) cert;
            if (key != null) {
                if (key.containsKey(Constants.CERT_FILE)) {
                    BString certFile = key.get(Constants.CERT_FILE);
                    BString keyFile = key.get(Constants.KEY_FILE);
                    BString keyPassword = getBStringValueIfPresent(key, Constants.KEY_PASSWORD);
                    kmf = getKeyManagerFactory(certFile, keyFile, keyPassword);
                } else {
                    kmf = getKeyManagerFactory(key);
                }
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(kmf.getKeyManagers(), tmf.getTrustManagers(), protocol);
            } else {
                tmf = getTrustManagerFactory(trustStore);
                return buildSslContext(null, tmf.getTrustManagers(), protocol);
            }
        }
        return null;
    }

    private static TrustManagerFactory getTrustManagerFactory(BString cert) throws Exception {
        Object publicKeyMap = Decode.decodeRsaPublicKeyFromCertFile(cert);
        if (publicKeyMap instanceof BMap) {
            X509Certificate x509Certificate = (X509Certificate) ((BMap<BString, Object>) publicKeyMap).getNativeData(
                    Constants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            KeyStore ts = KeyStore.getInstance(Constants.PKCS12);
            ts.load(null, "".toCharArray());
            ts.setCertificateEntry(UUID.randomUUID().toString(), x509Certificate);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            return tmf;
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKeyMap).getErrorMessage().getValue());
        }
    }

    private static TrustManagerFactory getTrustManagerFactory(BMap<BString, BString> trustStore) throws Exception {
        BString trustStorePath = trustStore.getStringValue(Constants.KEY_STORE_PATH);
        BString trustStorePassword = trustStore.getStringValue(Constants.KEY_STORE_PASS);
        KeyStore ts = getKeyStore(trustStorePath, trustStorePassword);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);
        return tmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BMap<BString, BString> keyStore) throws Exception {
        BString keyStorePath = keyStore.getStringValue(Constants.KEY_STORE_PATH);
        BString keyStorePassword = keyStore.getStringValue(Constants.KEY_STORE_PASS);
        KeyStore ks = getKeyStore(keyStorePath, keyStorePassword);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.getValue().toCharArray());
        return kmf;
    }

    private static KeyManagerFactory getKeyManagerFactory(BString certFile, BString keyFile, BString keyPassword)
            throws Exception {
        Object publicKey = Decode.decodeRsaPublicKeyFromCertFile(certFile);
        if (publicKey instanceof BMap) {
            X509Certificate publicCert = (X509Certificate) ((BMap<BString, Object>) publicKey).getNativeData(
                    Constants.NATIVE_DATA_PUBLIC_KEY_CERTIFICATE);
            Object privateKeyMap = Decode.decodeRsaPrivateKeyFromKeyFile(keyFile, keyPassword);
            if (privateKeyMap instanceof BMap) {
                PrivateKey privateKey = (PrivateKey) ((BMap<BString, Object>) privateKeyMap).getNativeData(
                        Constants.NATIVE_DATA_PRIVATE_KEY);
                KeyStore ks = KeyStore.getInstance(Constants.PKCS12);
                ks.load(null, "".toCharArray());
                ks.setKeyEntry(UUID.randomUUID().toString(), privateKey, "".toCharArray(),
                        new X509Certificate[]{publicCert});
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, "".toCharArray());
                return kmf;
            } else {
                throw new Exception("Failed to get the private key from Crypto API. " +
                        ((BError) privateKeyMap).getErrorMessage().getValue());
            }
        } else {
            throw new Exception("Failed to get the public key from Crypto API. " +
                    ((BError) publicKey).getErrorMessage().getValue());
        }
    }

    private static KeyStore getKeyStore(BString path, BString password) throws Exception {
        try (FileInputStream is = new FileInputStream(path.getValue())) {
            char[] passphrase = password.getValue().toCharArray();
            KeyStore ks = KeyStore.getInstance(Constants.PKCS12);
            ks.load(is, passphrase);
            return ks;
        }
    }

    private static SSLContext buildSslContext(KeyManager[] keyManagers, TrustManager[] trustManagers,
                                              String protocol) throws Exception {
        SSLContext sslContext =
                SSLContext.getInstance(Objects.requireNonNullElse(protocol, Options.DEFAULT_SSL_PROTOCOL));
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
        return sslContext;
    }

    private static BMap<BString, ?> getBMapValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? (BMap<BString, ?>) config.getMapValue(key) : null;
    }

    private static BString getBStringValueIfPresent(BMap<BString, ?> config, BString key) {
        return config.containsKey(key) ? config.getStringValue(key) : null;
    }
}
