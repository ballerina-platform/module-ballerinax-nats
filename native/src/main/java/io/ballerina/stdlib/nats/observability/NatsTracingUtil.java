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

package io.ballerina.stdlib.nats.observability;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.observability.ObserveUtils;
import io.ballerina.runtime.observability.ObserverContext;
import io.ballerina.stdlib.nats.Constants;
import io.nats.client.Connection;

/**
 * Providing metrics functionality to NATS.
 *
 * @since 1.1.0
 */
public class NatsTracingUtil {

    public static void traceResourceInvocation(Environment environment, String url, String subject) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        ObserverContext observerContext = ObserveUtils.getObserverContextOfCurrentFrame(environment);
        if (observerContext == null) {
            observerContext = new ObserverContext();
            ObserveUtils.setObserverContextToCurrentFrame(environment, observerContext);
        }
        setTags(observerContext, url, subject);
    }

    public static void traceResourceInvocation(Environment environment, BObject clientObj, String subject) {
        if (!ObserveUtils.isTracingEnabled()) {
            return;
        }
        Connection connection = (Connection) clientObj.getNativeData(Constants.NATS_CONNECTION);
        traceResourceInvocation(environment, connection.getConnectedUrl(), subject);
    }

    private static void setTags(ObserverContext observerContext, String url, String subject) {
        observerContext.addTag(NatsObservabilityConstants.TAG_URL, url);
        observerContext.addTag(NatsObservabilityConstants.TAG_SUBJECT, subject);
    }

    private NatsTracingUtil() {
    }
}
