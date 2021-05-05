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

package org.ballerinalang.nats;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

/**
 * Represents the constants which will be used for NATS.
 */
public class Constants {
    // Represents the NATS objects.
    public static final String NATS_CONNECTION = "nats_connection";

    public static final String NATS_METRIC_UTIL = "nats_metric_util";

    // Represents connected clients.
    public static final String CONNECTED_CLIENTS = "connected_clients";

    // Represents dispatcher list.
    public static final String DISPATCHER_LIST = "dispatcher_list";

    // Represent NATS Connection error listener.
    public static final String SERVICE_LIST = "service_list";

    // Represent whether connection close already triggered.
    public static final String CLOSING = "closing";

    // Represents the message which will be consumed from NATS.
    public static final String NATS_MESSAGE_OBJ_NAME = "Message";

    // Error code for i/o.
    static final String NATS_ERROR = "Error";

    public static final String BASIC_SUBSCRIPTION_LIST = "BasicSubscriptionList";

    public static final String ON_MESSAGE_RESOURCE = "onMessage";
    public static final String ON_REQUEST_RESOURCE = "onRequest";
    public static final String COUNTDOWN_LATCH = "count_down_latch";
    public static final String SERVICE_NAME = "service_name";

    public static final String NATS_CLIENT_SUBSCRIBED = "[ballerina/nats] Client subscribed for ";
    public static final String NATS_CLIENT_UNSUBSCRIBED = "[ballerina/nats] Client unsubscribed from subject ";

    public static final BString CONNECTION_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString CONNECTION_KEYSTORE = StringUtils.fromString("key");
    public static final BString CONNECTION_TRUSTORE = StringUtils.fromString("cert");
    public static final BString CONNECTION_PROTOCOL = StringUtils.fromString("protocol");
    public static final BString CONNECTION_PROTOCOL_NAME = StringUtils.fromString("name");
    public static final String KEY_STORE_TYPE = "PKCS12";
    public static final BString KEY_STORE_PASS = StringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = StringUtils.fromString("path");

    // Error messages and logs.
    public static final String ERROR_SETTING_UP_SECURED_CONNECTION = "Error while setting up secured connection. ";
    public static final String THREAD_INTERRUPTED_ERROR =
            "internal error occurred. The current thread got interrupted.";
    public static final String PRODUCER_ERROR = "error while publishing message to subject ";

    // Service annotation fields.
    public static final String SUBSCRIPTION_CONFIG = "ServiceConfig";
    public static final BString QUEUE_NAME = StringUtils.fromString("queueName");
    public static final BString SUBJECT = StringUtils.fromString("subject");
    public static final BString PENDING_LIMITS = StringUtils.fromString("pendingLimits");
    public static final BString MAX_MESSAGES = StringUtils.fromString("maxMessages");
    public static final BString MAX_BYTES = StringUtils.fromString("maxBytes");

    private Constants() {
    }
}
