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

import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.values.BString;
import org.ballerinalang.jvm.scheduling.StrandMetadata;
import org.ballerinalang.jvm.types.BPackage;

import static org.ballerinalang.jvm.util.BLangConstants.ORG_NAME_SEPARATOR;

/**
 * Represents the constants which will be used for NATS.
 */
public class Constants {
    // Represents the NATS objects.
    public static final String NATS_CONNECTION = "nats_connection";
    public static final String NATS_LISTENER = "Listener";
    public static final String NATS_PRODUCER = "Producer";

    // Represents the NATS streaming connection.
    public static final String NATS_STREAMING_CONNECTION = "nats_streaming_connection";

    public static final String NATS_METRIC_UTIL = "nats_metric_util";

    // Represents connected clients.
    public static final String CONNECTED_CLIENTS = "connected_clients";

    // Represents dispatcher list.
    public static final String DISPATCHER_LIST = "dispatcher_list";

    // Represent NATS Connection error listener.
    public static final String SERVICE_LIST = "service_list";

    // Represent whether connection close already triggered.
    public static final String CLOSING = "closing";

    public static final String NATS = "nats";
    public static final String ORG_NAME = "ballerinax";
    public static final String VERSION = "1.0.1";

    // Represents nats package.
    public static final String NATS_PACKAGE = ORG_NAME + ORG_NAME_SEPARATOR + NATS + ":" + VERSION;

    public static final BPackage NATS_PACKAGE_ID = new BPackage(ORG_NAME, "nats", VERSION);

    // Represents the message which will be consumed from NATS.
    public static final String NATS_MESSAGE_OBJ_NAME = "Message";

    // Represents the NATS message.
    public static final String NATS_MSG = "NATSMSG";

    // Error code for i/o.
    static final String NATS_ERROR = "NatsError";

    // Represents the NATS error detail record.
    static final String NATS_ERROR_DETAIL_RECORD = "Detail";

    // Represents the object which holds the connection.
    public static final BString CONNECTION_OBJ = BStringUtils.fromString("conn");

    // Represents the connection url
    public static final BString URL = BStringUtils.fromString("url");

    // Represents the NATS Streaming message.
    public static final String NATS_STREAMING_MSG = "nats_streaming_message";

    public static final String NATS_BASIC_CONSUMER_ANNOTATION = "SubscriptionConfig";

    public static final String NATS_STREAMING_SUBSCRIPTION_ANNOTATION = "StreamingSubscriptionConfig";
    public static final BString NATS_STREAMING_MANUAL_ACK = BStringUtils.fromString("manualAck");

    public static final String NATS_STREAMING_MESSAGE_OBJ_NAME = "StreamingMessage";

    public static final String NATS_STREAMING_LISTENER = "StreamingListener";

    public static final String BASIC_SUBSCRIPTION_LIST = "BasicSubscriptionList";

    public static final String STREAMING_DISPATCHER_LIST = "StreamingDispatcherList";
    public static final String STREAMING_SUBSCRIPTION_LIST = "StreamingSubscriptionsList";

    public static final String ON_MESSAGE_RESOURCE = "onMessage";

    public static final String ON_ERROR_RESOURCE = "onError";

    public static final String COUNTDOWN_LATCH = "count_down_latch";

    public static final String NATS_CLIENT_SUBSCRIBED = "[ballerina/nats] Client subscribed for ";
    public static final String NATS_CLIENT_UNSUBSCRIBED = "[ballerina/nats] Client unsubscribed from subject ";

    public static final BString CONNECTION_CONFIG_SECURE_SOCKET = BStringUtils.fromString("secureSocket");
    public static final BString CONNECTION_KEYSTORE = BStringUtils.fromString("keyStore");
    public static final BString CONNECTION_TRUSTORE = BStringUtils.fromString("trustStore");
    public static final BString CONNECTION_PROTOCOL = BStringUtils.fromString("protocol");
    public static final String KEY_STORE_TYPE = "PKCS12";
    public static final BString KEY_STORE_PASS = BStringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = BStringUtils.fromString("path");

    // Error messages and logs.
    public static final String MODULE = "[ballerina/nats] ";
    public static final String ERROR_SETTING_UP_SECURED_CONNECTION = "Error while setting up secured connection. ";
    public static final String THREAD_INTERRUPTED_ERROR =
            "Internal error occurred. The current thread got interrupted.";
    public static final String PRODUCER_ERROR = "Error while publishing message to subject ";

    // Service annotation fields.
    public static final String SUBSCRIPTION_CONFIG = "SubscriptionConfig";
    public static final BString QUEUE_NAME = BStringUtils.fromString("queueName");
    public static final BString SUBJECT = BStringUtils.fromString("subject");
    public static final BString PENDING_LIMITS = BStringUtils.fromString("pendingLimits");
    public static final BString MAX_MESSAGES = BStringUtils.fromString("maxMessages");
    public static final BString MAX_BYTES = BStringUtils.fromString("maxBytes");

    // Strand meta data
    public static final StrandMetadata ON_MESSAGE_METADATA = new StrandMetadata(ORG_NAME, NATS, VERSION,
                                                                                ON_MESSAGE_RESOURCE);
    public static final StrandMetadata ON_ERROR_METADATA = new StrandMetadata(ORG_NAME, NATS, VERSION,
                                                                              ON_ERROR_RESOURCE);

    private Constants() {
    }
}
