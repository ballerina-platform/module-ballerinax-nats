// Copyright (c) 2019 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/crypto;

# Configurations related to creating a NATS streaming subscription.
#
# + url - The NATS Broker URL. For a clustered use case, provide the URLs as a string array
# + connectionName - Name of the connection (this is optional)
# + retryConfig - Configurations related to connection reconnect attempts
# + ping - Configurations related to pinging the server
# + auth - Configurations related to authentication
# + inboxPrefix - The connection's inbox prefix, which all inboxes will start with
# + noEcho - Turns off echoing. This prevents the server from echoing messages back to the connection if it
#            has subscriptions on the subject being published to
# + secureSocket - Configurations related to SSL/TLS
public type ConnectionConfig record {|
  string|string[] url = DEFAULT_URL;
  string connectionName = "ballerina-nats";
  RetryConfig retryConfig?;
  Ping ping?;
  Credentials|Tokens auth?;
  string inboxPrefix = "_INBOX.";
  boolean noEcho = false;
  SecureSocket secureSocket?;
|};

# Configurations related to token based authentication.
#
# + token - The token for token-based authentication
public type Tokens record {|
  string token;
|};

# Configurations related to basic authentication.
#
# + username - The username for basic authentication
# + password - The password for basic authentication
public type Credentials record {|
  string username;
  string password;
|};

# Configurations related to facilitating a secure communication with a remote HTTP endpoint.
#
# + trustStore - Configurations associated with the TrustStore
# + keyStore - Configurations associated with the KeyStore
# + protocol - The standard name of the requested protocol
public type SecureSocket record {|
    crypto:TrustStore trustStore?;
    crypto:KeyStore keyStore?;
    string protocol = "TLS";
|};

# Configurations related to pinging the server
#
# + pingInterval - The interval (in seconds) between the attempts of pinging the server
# + maxPingsOut - The maximum number of pings the client can have in flight. The default value is two
public type Ping record {|
  decimal pingInterval = 120;
  int maxPingsOut = 2;
|};

# Configurations related to connection reconnect attempts.
#
# + maxReconnect - Maximum number of reconnect attempts. The reconnect state is triggered when an already established
#                  connection is lost. During the initial connection attempt, the client will cycle
#                  over its server list one time regardless of the `maxReconnects` value that is set.
#                  Use 0 to turn off auto reconnecting.
#                  Use -1 to turn on infinite reconnects.
# + reconnectWait - The time(in seconds) to wait between the reconnect attempts to reconnect to the same server
# + connectionTimeout - The timeout (in seconds) for the connection attempts
public type RetryConfig record {|
  int maxReconnect = 60;
  decimal reconnectWait = 2;
  decimal connectionTimeout = 2;
|};

# Represents the message, which a NATS server sends to its subscribed services.
#
# + content - The message content
# + replyTo - The `replyTo` subject of the message
# + subject - The subject to which the message was sent to
public type Message record {|
    byte[] content;
    string subject;
    string replyTo?;
|};

# The configurations for the NATS basic subscription.
#
# + subject - Name of the subject
# + queueName - Name of the queue group
# + pendingLimits - Parameters to set limits on the maximum number of pending messages
#                   or maximum size of pending messages
public type ServiceConfigData record {|
    string subject;
    string queueName?;
    PendingLimits pendingLimits?;
|};

# The configurations to set limits on the maximum number of messages or maximum size of messages this consumer will
# hold before it starts to drop new messages waiting for the resource functions to drain the queue.
# Setting a value less than or equal to 0 will disable this check.
#
# + maxMessages - Maximum number of pending messages retrieved and held by the consumer service.
#                 The default value is 65536
# + maxBytes - Total size of pending messages in bytes retrieved and held by the consumer service.
#              The default value is 67108864
public type PendingLimits record {|
    int maxMessages;
    int maxBytes;
|};
