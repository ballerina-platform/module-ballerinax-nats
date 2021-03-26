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

package org.ballerinalang.nats.basic.consumer;

import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.connection.ConnectionUtils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.ballerinalang.nats.Constants.DISPATCHER_LIST;

/**
 * Initialize NATS subscriber using the connection.
 *
 * @since 0.995
 */
public class Init {

    public static Object consumerInit(BObject listenerObject, Object url, BMap connectionConfig) {
        Connection natsConnection = null;
        try {
            natsConnection = ConnectionUtils.getNatsConnection(url, connectionConfig);
        } catch (UnrecoverableKeyException e) {
            return Utils.createNatsError(
                    Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "The key in the keystore cannot be recovered.");
        } catch (CertificateException e) {
            return Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "certificate error, "
                                                 + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            return Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "algorithm error, "
                                                 + e.getMessage());
        } catch (KeyStoreException e) {
            return Utils.createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "keystore error, "
                                                 + e.getMessage());
        } catch (KeyManagementException e) {
            return Utils
                    .createNatsError(Constants.ERROR_SETTING_UP_SECURED_CONNECTION + "key management error, "
                                             + e.getMessage());
        } catch (InterruptedException | IOException e) {
            String errorMsg = "error while setting up the connection. " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return Utils.createNatsError(errorMsg);
        }
        listenerObject.addNativeData(Constants.NATS_METRIC_UTIL, new NatsMetricsReporter(natsConnection));
        listenerObject.addNativeData(Constants.NATS_CONNECTION, natsConnection);
        ((NatsMetricsReporter) listenerObject.getNativeData(Constants.NATS_METRIC_UTIL)).reportNewClient();

        // Initialize dispatcher list to use in service register and listener close.
        ConcurrentHashMap<String, Dispatcher> dispatcherList = new ConcurrentHashMap<>();
        listenerObject.addNativeData(DISPATCHER_LIST, dispatcherList);
        ArrayList<String> subscriptionsList = new ArrayList<>();
        listenerObject.addNativeData(Constants.BASIC_SUBSCRIPTION_LIST, subscriptionsList);
        List<BObject> serviceList = Collections.synchronizedList(new ArrayList<>());
        listenerObject.addNativeData(Constants.SERVICE_LIST, serviceList);
        return null;
    }
}
