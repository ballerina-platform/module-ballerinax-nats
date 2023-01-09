/*
 * Copyright (c) 2022, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
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

package io.ballerina.stdlib.nats.jetstream.client;

import io.ballerina.runtime.api.utils.StringUtils;
import io.ballerina.runtime.api.values.BDecimal;
import io.ballerina.runtime.api.values.BMap;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import io.ballerina.stdlib.nats.Constants;
import io.ballerina.stdlib.nats.Utils;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.DiscardPolicy;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;

import java.io.IOException;
import java.time.Duration;

/**
 * Extern functions of the APIs provided by the JetStreamManagement client.
 */
public class ManagementUtils {

    private ManagementUtils() {}

    public static Object addStream(BObject clientObject, BMap<BString, Object> streamConfig) {
        try {
            JetStreamManagement jetStreamManagement =
                    (JetStreamManagement) clientObject.getNativeData(Constants.JET_STREAM_MANAGEMENT);
            StreamConfiguration streamConfiguration = getStreamConfig(streamConfig);
            jetStreamManagement.addStream(streamConfiguration);
            return null;
        } catch (IOException | JetStreamApiException | IllegalStateException e) {
            String errorMsg = "Error occurred while adding the stream.";
            return Utils.createNatsError(errorMsg, e);
        }
    }

    public static Object updateStream(BObject clientObject, BMap<BString, Object> streamConfig) {
        try {
            JetStreamManagement jetStreamManagement =
                    (JetStreamManagement) clientObject.getNativeData(Constants.JET_STREAM_MANAGEMENT);
            StreamConfiguration streamConfiguration = getStreamConfig(streamConfig);
            jetStreamManagement.updateStream(streamConfiguration);
            return null;
        } catch (IOException | JetStreamApiException | IllegalStateException e) {
            String errorMsg = "Error occurred while updating the stream.";
            return Utils.createNatsError(errorMsg, e);
        }
    }

    public static Object deleteStream(BObject clientObject, BString streamName) {
        JetStreamManagement jetStreamManagement =
                (JetStreamManagement) clientObject.getNativeData(Constants.JET_STREAM_MANAGEMENT);
        try {
            jetStreamManagement.deleteStream(streamName.getValue());
            return null;
        } catch (IOException | JetStreamApiException | IllegalStateException e) {
            String errorMsg = "Error occurred while deleting the stream.";
            return Utils.createNatsError(errorMsg, e);
        }
    }

    public static Object purgeStream(BObject clientObject, BString streamName) {
        JetStreamManagement jetStreamManagement =
                (JetStreamManagement) clientObject.getNativeData(Constants.JET_STREAM_MANAGEMENT);
        try {
            jetStreamManagement.purgeStream(streamName.getValue());
            return null;
        } catch (IOException | JetStreamApiException | IllegalStateException e) {
            String errorMsg = "Error occurred while purging the stream.";
            return Utils.createNatsError(errorMsg, e);
        }
    }

    public static StreamConfiguration getStreamConfig(BMap<BString, Object> streamConfig) {
        StreamConfiguration.Builder streamConfiguration = StreamConfiguration.builder();
        if (streamConfig.containsKey(Constants.STREAM_CONFIG_SUBJECTS)) {
            streamConfiguration.subjects((streamConfig.getArrayValue(Constants.STREAM_CONFIG_SUBJECTS))
                    .getStringArray());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_NAME))) {
            streamConfiguration
                    .name(streamConfig.getStringValue(StringUtils.fromString(Constants.STREAM_CONFIG_NAME)).getValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_STORAGE))) {
            String storage =
                    streamConfig.getStringValue(StringUtils.fromString(Constants.STREAM_CONFIG_STORAGE)).getValue();
            StorageType storageType = StorageType.Memory;
            if (storage.equals(Constants.FILE_STORAGE)) {
                storageType = StorageType.File;
            }
            streamConfiguration.storageType(storageType);
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_DESC))) {
            streamConfiguration
                    .description(streamConfig.getStringValue(StringUtils.fromString(Constants.STREAM_CONFIG_DESC))
                            .getValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_RETENTION))) {
            RetentionPolicy retentionPolicy = RetentionPolicy.Limits;
            String retention =
                    streamConfig.getStringValue(StringUtils.fromString(Constants.STREAM_CONFIG_RETENTION)).getValue();
            if (retention.equals(Constants.INTEREST_RETENTION)) {
                retentionPolicy = RetentionPolicy.Interest;
            } else if (retention.equals(Constants.WORKQUEUE_RETENTION)) {
                retentionPolicy = RetentionPolicy.WorkQueue;
            }
            streamConfiguration.retentionPolicy(retentionPolicy);
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_CONSUMERS))) {
            streamConfiguration
                    .maxConsumers(streamConfig.getFloatValue(StringUtils.fromString
                            (Constants.STREAM_CONFIG_MAX_CONSUMERS)).longValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_MSGS))) {
            streamConfiguration
                    .maxMessages(streamConfig.getFloatValue(StringUtils.fromString
                            (Constants.STREAM_CONFIG_MAX_MSGS)).longValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_PER_SUBJECT))) {
            streamConfiguration
                    .maxMessagesPerSubject(streamConfig.getFloatValue(StringUtils.fromString
                            (Constants.STREAM_CONFIG_MAX_PER_SUBJECT)).longValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_BYTES))) {
            streamConfiguration
                    .maxBytes(streamConfig.getFloatValue(StringUtils.fromString
                            (Constants.STREAM_CONFIG_MAX_BYTES)).longValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_MSG_SIZE))) {
            streamConfiguration
                    .maxMsgSize(streamConfig.getFloatValue(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_MSG_SIZE))
                            .longValue());
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_AGE))) {
            streamConfiguration
                    .maxAge(Duration.ofSeconds(((BDecimal)
                            streamConfig.get(StringUtils.fromString(Constants.STREAM_CONFIG_MAX_AGE))).intValue()));
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_REPLICAS))) {
            int replicas =
                    streamConfig.getIntValue(StringUtils.fromString(Constants.STREAM_CONFIG_REPLICAS)).intValue();
            streamConfiguration.replicas(replicas);
        }

        if (streamConfig.containsKey(StringUtils.fromString(Constants.STREAM_CONFIG_DISCARD_POLICY))) {
            DiscardPolicy discardPolicy = DiscardPolicy.Old;
            String discard =
                    streamConfig.getStringValue(StringUtils.fromString(Constants.STREAM_CONFIG_DISCARD_POLICY))
                            .getValue();
            if (discard.equals(Constants.DISCARD_NEW)) {
                discardPolicy = DiscardPolicy.New;
            }
            streamConfiguration.discardPolicy(discardPolicy);
        }

        return streamConfiguration.build();
    }
}
