// Copyright (c) 2022, WSO2 LLC. (https://www.wso2.com) All Rights Reserved.
// 
// WSO2 LLC. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
// 
// http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.

# Determines the properties for a stream.
#
# + name - A name for the stream
# + description - A short description of the purpose of this stream 
# + subjects - Which NATS subject/subjects to populate this stream with. Supports wildcards. Defaults to just the
#              configured stream name
# + retentionPolicy - How message retention is considered, Limits (default), Interest or WorkQueue
# + maxConsumers - How many consumers can be defined for the stream, -1 for unlimited
# + maxMsgs - How large the stream may become in total messages before the configured discard policy takes action
# + maxMsgsPerSubject - Maximum amount of messages to retain per subject 
# + maxBytes - How large the stream may become in total bytes before the configured discard policy takes action
# + maxAge - Maximum age of any message in the stream, expressed in seconds  
# + maxMsgSize - The largest message that will be accepted by the Stream  
# + storageType - The type of storage backend, File and Memory
# + replicas - The number of replicas to store this message on
# + discardPolicy - When a stream has reached its configured maxBytes or maxMsgs, this policy takes action
public type StreamConfiguration record {|
   string name?;
   string description?;
   string|string[] subjects?;
   RetentionPolicy retentionPolicy?;
   float maxConsumers?;
   float maxMsgs?;
   float maxMsgsPerSubject?;
   float maxBytes?;
   decimal maxAge?;
   float maxMsgSize?;
   StorageType storageType?;
   int replicas?;
   boolean noAck?;
   DiscardPolicy discardPolicy?;
|};
 
# How message retention is considered, Limits (default), Interest or WorkQueue
public enum RetentionPolicy {
   LIMITS,
   INTEREST,
   WORKQUEUE
}
 
# The type of storage backend, File and Memory
public enum StorageType {
   FILE,
   MEMORY
}
 
# When a Stream has reached its configured maxMsgs or maxBytes, this policy kicks in. 
# New refuses new messages or Old (default) deletes old messages to make space
public enum DiscardPolicy {
   NEW,
   OLD
}
 
# The configurations for the NATS JetStream subscription.
#
# + subject - Name of the subject
# + queueName - Name of the queue group
public type JetStreamServiceConfigData record {|
    string subject;
    string queueName?;
    boolean autoAck = true;
|};

# A message consumed from a stream.
#
# + subject - Subject of the message  
# + content - Payload of the message
public type JetStreamMessage record {|
   string subject;
   byte[] content;
|};
