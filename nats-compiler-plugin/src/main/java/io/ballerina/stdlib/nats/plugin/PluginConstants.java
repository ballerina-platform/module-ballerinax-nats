/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package io.ballerina.stdlib.nats.plugin;

/**
 * NATS compiler plugin constants.
 */
public class PluginConstants {
    // compiler plugin constants
    public static final String PACKAGE_PREFIX = "nats";
    public static final String REMOTE_QUALIFIER = "REMOTE";
    public static final String DIAGNOSTIC_CODE = "NATS_101";
    public static final String ON_MESSAGE_FUNC = "onMessage";
    public static final String ON_REQUEST_FUNC = "onRequest";
    public static final String ON_ERROR_FUNC = "onError";

    // parameters
    public static final String MESSAGE = "Message";
    public static final String ERROR_PARAM = "Error";

    // return types error or nil
    public static final String ERROR = "error";
    public static final String NATS_ERROR = PACKAGE_PREFIX + ":" + ERROR_PARAM;
    public static final String NIL = "?";
    public static final String ERROR_OR_NIL = ERROR + NIL;
    public static final String NIL_OR_ERROR = "()|" + ERROR;
    public static final String NATS_ERROR_OR_NIL = NATS_ERROR + NIL;
    public static final String NIL_OR_NATS_ERROR = "()|" + NATS_ERROR;

    // errors for service validations
    public static final String ON_MESSAGE_OR_ON_REQUEST = "Only one of either onMessage or onRequest is allowed.";
    public static final String NO_ON_MESSAGE_OR_ON_REQUEST =
            "Service must have either remote function onMessage or onRequest.";
    public static final String INVALID_REMOTE_FUNCTION = "Invalid remote function.";
    public static final String FUNCTION_SHOULD_BE_REMOTE = "Function must have the remote qualifier.";
    public static final String MUST_HAVE_MESSAGE = "Must have the function parameter nats:Message.";
    public static final String MUST_HAVE_MESSAGE_AND_ERROR = "Must have the function parameters nats:Message and " +
            "nats:Error.";
    public static final String INVALID_FUNCTION_PARAM = "Invalid function parameter.";
    public static final String INVALID_FUNCTION_PARAM_ERROR = INVALID_FUNCTION_PARAM + " Only nats:Error" +
            " is allowed.";
    public static final String INVALID_FUNCTION_PARAM_MESSAGE = INVALID_FUNCTION_PARAM + " Only nats:Message" +
            " is allowed.";
    public static final String ONLY_PARAMS_ALLOWED = "Invalid function parameter count. Only nats:Message is allowed.";
    public static final String ONLY_PARAMS_ALLOWED_ON_ERROR
            = "Invalid function parameter count. Only nats:Message and nats:Error are allowed.";
    public static final String INVALID_RETURN_TYPE_ERROR_OR_NIL =
            "Invalid return type. Only error? or nats:Error? is allowed.";
    public static final String INVALID_RETURN_TYPE_ANY_DATA =
            "Invalid return type. Only anydata or error is allowed.";
    static final String[] ANY_DATA_RETURN_VALUES = {ERROR, NATS_ERROR, ERROR_OR_NIL, NATS_ERROR_OR_NIL,
            NIL_OR_ERROR, NIL_OR_NATS_ERROR, "string", "int", "float", "decimal", "boolean", "xml", "anydata",
            "string|error", "int|error", "float|error", "decimal|error", "boolean|error", "xml|error", "anydata|error",
            "error|string", "error|int", "float|error", "error|decimal", "error|boolean", "error|boolean",
            "error|anydata", "string?", "anydata?", "int?", "float?", "decimal?", "xml?", "boolean?"};

    private PluginConstants() {
    }
}
