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
    public static final String PACKAGE_ORG = "ballerinax";
    public static final String ON_MESSAGE_FUNC = "onMessage";
    public static final String ON_REQUEST_FUNC = "onRequest";
    public static final String ON_ERROR_FUNC = "onError";

    // parameters
    public static final String MESSAGE = "Message";
    public static final String ERROR_PARAM = "Error";

    // return types error or nil
    public static final String ERROR = "error";
    static final String[] ANY_DATA_RETURN_VALUES = {"string", "int", "float", "decimal", "boolean", "xml", "anydata"};

    /**
     * Compilation errors.
     */
    enum CompilationErrors {
        ON_MESSAGE_OR_ON_REQUEST("Only one of either onMessage or onRequest is allowed.", "NATS_101"),
        NO_ON_MESSAGE_OR_ON_REQUEST("Service must have either remote method onMessage or onRequest.",
                "NATS_102"),
        INVALID_RESOURCE_FUNCTION("Resource functions not allowed", "NATS_103"),
        FUNCTION_SHOULD_BE_REMOTE("Method must have the remote qualifier.", "NATS_104"),
        MUST_HAVE_MESSAGE_OR_ANYDATA("Must have the method parameter nats:Message or anydata.", "NATS_105"),
        MUST_HAVE_MESSAGE_AND_ERROR("Must have the method parameters nats:Message and nats:Error.",
                "NATS_106"),
        INVALID_FUNCTION("Resource functions are not allowed.", "NATS_107"),
        INVALID_FUNCTION_PARAM_MESSAGE("Invalid method parameter. Only nats:Message or anydata is allowed.",
                "NATS_108"),
        INVALID_FUNCTION_PARAM_MESSAGE_OR_ANYDATA("Invalid method parameter. Only nats:Message or anydata is allowed.",
                "NATS_109"),
        INVALID_FUNCTION_PARAM_ANYDATA("Invalid method parameter. Only anydata is allowed.",
                "NATS_110"),
        DATA_BINDING_ALREADY_EXISTS("Invalid second parameter. If the first parameter is of type anydata, " +
                "a second parameter is not allowed.", "NATS111"),
        INVALID_FUNCTION_PARAM_ERROR("Invalid method parameter. Only nats:Error is allowed.",
                "NATS_112"),
        ONLY_PARAMS_ALLOWED("Invalid method parameter count. Only nats:Message and/or anydata is allowed.",
                "NATS_113"),
        ONLY_PARAMS_ALLOWED_ON_ERROR("Invalid method parameter count. Only nats:Message and nats:Error are allowed.",
                "NATS_114"),
        INVALID_RETURN_TYPE_ERROR_OR_NIL("Invalid return type. Only error? or nats:Error? is allowed.",
                "NATS_115"),
        INVALID_RETURN_TYPE_ANY_DATA("Invalid return type. Only anydata or error is allowed.",
                "NATS_116"),
        INVALID_MULTIPLE_LISTENERS("Multiple listener attachments. Only one nats:Listener is allowed.",
                "NATS_117"),
        INVALID_ANNOTATION_NUMBER("Only one service config annotation is allowed.",
                "NATS_118"),
        NO_ANNOTATION("No @nats:ServiceConfig{} annotation is found.",
                "NATS_119"),
        INVALID_ANNOTATION("Invalid service config annotation. Only @nats:ServiceConfig{} is allowed.",
                "NATS_120"),
        INVALID_SERVICE_ATTACH_POINT("Invalid service attach point. Only string literals are allowed.",
                "NATS_121"),
        TEMPLATE_CODE_GENERATION_HINT("Template generation for empty service", "NATS_122");

        private final String error;
        private final String errorCode;

        CompilationErrors(String error, String errorCode) {
            this.error = error;
            this.errorCode = errorCode;
        }

        String getError() {
            return error;
        }
        String getErrorCode() {
            return errorCode;
        }
    }

    private PluginConstants() {
    }
}
