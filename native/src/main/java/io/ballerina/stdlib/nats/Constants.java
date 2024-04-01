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

package io.ballerina.stdlib.nats;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BString;

import static io.ballerina.stdlib.nats.Utils.getModule;

/**
 * Represents the constants which will be used for NATS.
 */
public class Constants {

    // NATS package name constant fields
    public static final String ORG_NAME = "ballerinax";
    public static final String NATS = "nats";

    // Represents the NATS objects.
    public static final String NATS_CONNECTION = "nats_connection";

    // JetStream constants.
    public static final String JET_STREAM = "jet_stream";
    public static final String JET_STREAM_MESSAGE = "jet_stream_message";
    public static final String JET_STREAM_AUTO_ACK = "jet_stream_auto";
    public static final String JET_STREAM_MANAGEMENT = "jet_stream_management";
    public static final BString STREAM_CONFIG_SUBJECTS = StringUtils.fromString("subjects");
    public static final String STREAM_CONFIG_NAME = "name";
    public static final String STREAM_CONFIG_STORAGE = "storageType";
    public static final String STREAM_CONFIG_RETENTION = "retentionPolicy";
    public static final String STREAM_CONFIG_MAX_CONSUMERS = "maxConsumers";
    public static final String STREAM_CONFIG_MAX_MSGS = "maxMsgs";
    public static final String STREAM_CONFIG_MAX_PER_SUBJECT = "maxMsgsPerSubject";
    public static final String STREAM_CONFIG_MAX_BYTES = "maxBytes";
    public static final String STREAM_CONFIG_MAX_AGE = "maxAge";
    public static final String STREAM_CONFIG_MAX_MSG_SIZE = "maxMsgSize";
    public static final String STREAM_CONFIG_REPLICAS = "replicas";
    public static final String STREAM_CONFIG_DISCARD_POLICY = "discardPolicy";
    public static final String STREAM_CONFIG_DESC = "description";
    public static final String STREAM_MESSAGE = "JetStreamMessage";
    public static final String STREAM_CALLER = "JetStreamCaller";
    public static final String FILE_STORAGE = "FILE";
    public static final String INTEREST_RETENTION = "INTEREST";
    public static final String WORKQUEUE_RETENTION = "WORKQUEUE";
    public static final String DISCARD_NEW = "NEW";
    public static final String STREAM_SUBSCRIPTION_CONFIG = "StreamServiceConfig";


    public static final String CONSTRAINT_VALIDATION = "validation";

    public static final String NATS_METRIC_UTIL = "nats_metric_util";

    // Represents dispatcher list.
    public static final String DISPATCHER_LIST = "dispatcher_list";

    // Represent NATS Connection error listener.
    public static final String SERVICE_LIST = "service_list";

    // Represents the message which will be consumed from NATS.
    public static final String NATS_MESSAGE_OBJ_NAME = "AnydataMessage";
    public static final String MESSAGE_CONTENT = "content";
    public static final String MESSAGE_SUBJECT = "subject";
    public static final String MESSAGE_REPLY_TO = "replyTo";

    // Error code for i/o.
    static final String NATS_ERROR = "Error";
    public static final String PAYLOAD_BINDING_ERROR = "PayloadBindingError";
    public static final String PAYLOAD_VALIDATION_ERROR = "PayloadValidationError";

    public static final String BASIC_SUBSCRIPTION_LIST = "BasicSubscriptionList";

    public static final String ON_MESSAGE_RESOURCE = "onMessage";
    public static final String ON_REQUEST_RESOURCE = "onRequest";
    public static final String ON_ERROR_RESOURCE = "onError";
    public static final String COUNTDOWN_LATCH = "count_down_latch";
    public static final String SERVICE_NAME = "service_name";

    public static final BString CONNECTION_CONFIG_SECURE_SOCKET = StringUtils.fromString("secureSocket");
    public static final BString KEY = StringUtils.fromString("key");
    public static final BString CERT = StringUtils.fromString("cert");
    public static final BString PROTOCOL = StringUtils.fromString("protocol");
    public static final BString PROTOCOL_NAME = StringUtils.fromString("name");
    public static final String PKCS12 = "PKCS12";
    public static final BString KEY_STORE_PASS = StringUtils.fromString("password");
    public static final BString KEY_STORE_PATH = StringUtils.fromString("path");

    // Error messages and logs.
    public static final String THREAD_INTERRUPTED_ERROR =
            "internal error occurred. The current thread got interrupted.";
    public static final String PRODUCER_ERROR = "error while publishing message to subject ";

    // Service annotation fields.
    public static final String SUBSCRIPTION_CONFIG = "ServiceConfig";
    public static final BString QUEUE_NAME = StringUtils.fromString("queueName");
    public static final BString SUBJECT = StringUtils.fromString("subject");
    public static final BString AUTO_ACK = StringUtils.fromString("autoAck");
    public static final BString PENDING_LIMITS = StringUtils.fromString("pendingLimits");
    public static final BString MAX_MESSAGES = StringUtils.fromString("maxMessages");
    public static final BString MAX_BYTES = StringUtils.fromString("maxBytes");

    public static final BString CERT_FILE = StringUtils.fromString("certFile");
    public static final BString KEY_FILE = StringUtils.fromString("keyFile");
    public static final BString KEY_PASSWORD = StringUtils.fromString("keyPassword");
    public static final String NATIVE_DATA_PUBLIC_KEY_CERTIFICATE = "NATIVE_DATA_PUBLIC_KEY_CERTIFICATE";
    public static final String NATIVE_DATA_PRIVATE_KEY = "NATIVE_DATA_PRIVATE_KEY";

    // Payload related constants
    public static final String PARAM_ANNOTATION_PREFIX = "$param$.";
    public static final BString PARAM_PAYLOAD_ANNOTATION_NAME = StringUtils.fromString(
            getModule().toString() + ":Payload");
    public static final String TYPE_CHECKER_OBJECT_NAME = "TypeChecker";
    public static final String IS_ANYDATA_MESSAGE = "isAnydataMessage";

    private Constants() {
    }
}
