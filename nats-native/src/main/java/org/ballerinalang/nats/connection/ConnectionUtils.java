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

import io.ballerina.runtime.api.values.BMap;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Establish a connection with NATS server.
 *
 * @since 0.995
 */
public class ConnectionUtils {

    /**
     * Creates and retrieves the SSLContext from socket configuration.
     *
     * @param secureSocket secureSocket record.
     * @return Initialized SSLContext.
     */
    public static SSLContext getSSLContext(BMap secureSocket) {
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
