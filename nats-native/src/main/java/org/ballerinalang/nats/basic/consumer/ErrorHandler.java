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

import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.async.Callback;
import io.ballerina.runtime.api.async.StrandMetadata;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.nats.Constants;
import org.ballerinalang.nats.Utils;
import org.ballerinalang.nats.observability.NatsMetricsReporter;
import org.ballerinalang.nats.observability.NatsObservabilityConstants;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static org.ballerinalang.nats.Constants.ON_ERROR_RESOURCE;

/**
 * Handles dispatching errors detected by the error listener and due to data binding.
 *
 * @since 1.0.0
 */
public class ErrorHandler {

    /**
     * Dispatch errors to the onError resource, if the onError resource is available.
     *
     * @param serviceObject   BObject service
     * @param msgObj          Message object
     * @param e               BError
     * @param natsMetricsReporter Nats Metrics Util
     */
    static void dispatchError(BObject serviceObject, BMap<BString, Object> msgObj, BError e, Runtime runtime,
                              NatsMetricsReporter natsMetricsReporter) {
        boolean onErrorResourcePresent = Arrays.stream(serviceObject.getType().getMethods())
                .anyMatch(resource -> resource.getName().equals(ON_ERROR_RESOURCE));
        if (onErrorResourcePresent) {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            // Strand meta data
            StrandMetadata metadata = new StrandMetadata(Utils.getModule().getOrg(),
                                                         Utils.getModule().getName(),
                                                         Utils.getModule().getVersion(), ON_ERROR_RESOURCE);
            runtime.invokeMethodAsync(serviceObject, ON_ERROR_RESOURCE, null, metadata, new ResponseCallback(
                                              countDownLatch, msgObj.getStringValue(Constants.SUBJECT).getValue(),
                                              natsMetricsReporter), msgObj, true, e, true);
            try {
                countDownLatch.await();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw Utils.createNatsError(Constants.THREAD_INTERRUPTED_ERROR);
            }
        }
    }

    private ErrorHandler() {
    }

    /**
     * Represents the callback which will be triggered upon submitting to resource.
     */
    public static class ResponseCallback implements Callback {
        private CountDownLatch countDownLatch;
        private String subject;
        private NatsMetricsReporter natsMetricsReporter;

        ResponseCallback(CountDownLatch countDownLatch, String subject,
                         NatsMetricsReporter natsMetricsReporter) {
            this.countDownLatch = countDownLatch;
            this.natsMetricsReporter = natsMetricsReporter;
            this.subject = subject;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifySuccess(Object obj) {
            countDownLatch.countDown();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_MSG_RECEIVED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void notifyFailure(io.ballerina.runtime.api.values.BError error) {
            error.printStackTrace();
            natsMetricsReporter.reportConsumerError(subject, NatsObservabilityConstants.ERROR_TYPE_ON_ERROR);
            countDownLatch.countDown();
        }
    }

}
