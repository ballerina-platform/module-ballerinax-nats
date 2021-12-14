# Specification: Ballerina NATS Library

_Owners_: @aashikam @shafreenAnfar  
_Reviewers_: @shafreenAnfar  
_Created_: 2020/10/28 
_Updated_: 2021/11/29  
_Issue_: [#2214](https://github.com/ballerina-platform/ballerina-standard-library/issues/2214)

# Introduction
This is the specification for NATS standard library which is used to send and receive messages by connecting to the NATS server. 
This library is programmed in the [Ballerina programming language](https://ballerina.io/), which is an open-source programming language for the cloud
that makes it easier to use, combine, and create network services.

# Contents

1. [Overview](#1-overview)
2. [Connection](#2-connection)
3. [Publishing](#3-publishing)
5. [Subscribing](#4-subscribing)
6. [Samples](#5-samples)
    * 5.1. [Publish-Subscribe](#51-publish-subscribe)
    * 5.2. [Request-Reply](#52-request-reply)
    * 5.3. [Queue Groups](#53-queue-groups)

## 1. Overview
This specification elaborates on the usage of NATS library client and services/listener. NATS makes it easy for applications to communicate by sending and receiving messages. These messages are addressed by subjects and do not depend on network location.
Data is encoded and framed as a message and sent by a publisher. The message is received, decoded, and processed by one or more subscribers.
More on NATS server concepts can be found [here](https://docs.nats.io/nats-concepts/overview).  

## 2. Connection
Fundamentally, each `nats:Client` and `nats:Listener` initialization represents a single network connection to the NATS server. There are multiple ways to connect. 

1. Initialize the `nats:Client`/`nats:Listener` with the constant value `nats:DEFAULT_URL`. This connects the client to a local server on the default port.
```ballerina
   // Connecting using the NATS client.
   nats:Client natsClient = check new(nats:DEFAULT_URL);
   
   // Connecting using the NATS listener.
   nats:Listener natsListener = check new(nats:DEFAULT_URL);
```

2. Initialize the `nats:Client`/`nats:Listener` to a remote instance with a custom URL or a cluster of servers using multiple URLs. 
```ballerina
   // Connecting using the NATS client.
   nats:Client natsClient = check new("nats://serverone:4222");

   // Connecting using the NATS listener.
   nats:Listener natsListener = check new(["nats://serverone:4222", "nats://servertwo:4222"]);
```

3. Initialize the `nats:Client`/`nats:Listener` with custom configurations.

**Configurations available for connections:**

`nats:ConnectionConfiguration`
- `string` connectionName - The name of the connection. The default value is "ballerina-nats".
- `nats:RetryConfig` retryConfig - The configurations related to connection reconnect attempts.
  - `int` maxReconnect - Maximum number of reconnect attempts. The `reconnect` state is triggered when an already established connection is lost. During the initial connection attempt, the client will cycle over its server list one time regardless of the `maxReconnects` value that is set.
     Use 0 to turn off auto reconnecting. Use -1 to turn on infinite reconnects. The default value is 60.
  - `decimal` reconnectWait - The time (in seconds) to wait between the attempts to reconnect to the same server. Default value is 2.
  - `decimal` connectionTimeout - The timeout (in seconds) for the connection attempts. The default value is 2.
- `nats:Ping` ping - The configurations related to pinging the server.
  - `decimal` pingInterval - The interval (in seconds) between the attempts of pinging the server. The default value is 2.
  - `int` maxPingsOut - The maximum number of pings the client can have in flight. The default value is 2.
- `nats:Credentials|nats:Tokens` auth - The configurations related to authentication. More details in Secured connections section.
- `string` inboxPrefix - The connection's inbox prefix, which all inboxes will start with. The default value is "_INBOX".
- `boolean` noEcho - Turns off echoing. This prevents the server from echoing messages back to the connection if it has subscriptions on the subject being published to. The default value is false.
- `nats:SecureSocket` secureSocket - The configurations related to SSL/TLS. More details in Secured connections section.

```ballerina
   // See the API docs for the complete list of supported configurations. 
   nats:ConnectionConfiguration config = {
      connectionName: "my-nats",
      noEcho: true
   };
   
   // Connecting using the NATS client.
   nats:Client natsClient = check new(["nats://serverone:4222", nats:DEFAULT_URL], config);

   // Connecting using the NATS listener.
   nats:Listener natsListener = check new("nats://serverone:4222", config);
```

4. Secured connections.

Connections can be secured using following approaches. All the given approaches are supported by both the client and the listener.

**Configurations available for basic authentication:**

`nats:Credentials`
- `string` username - The username for basic authentication.
- `string` password - The password for basic authentication.

**Configurations available for token-based authentication:**

`nats:Tokens`
- `string` token - The token value for token-based authentication.

**Configurations available for ssl/tls:**

`nats:SecureSocket`
- `crypto:TrustStore|string` cert - Configurations associated with `crypto:TrustStore` or single certificate file that the client trusts.
- `crypto:KeyStore|nats:CertKey` key - Configurations associated with `crypto:KeyStore` or combination of certificate and private key of the client.
- `record` protocol - SSL/TLS protocol related options.

```ballerina
   // Connect using username/password credentials. 
   nats:Client natsClient = check new(nats:DEFAULT_URL,
      auth = {
          username: "alice",
          password: "alice@123"
      }
   );
   
   // Connecting using token based authentication.
   nats:Tokens myToken = { token: "MyToken" };
   nats:Listener natsListener = check new(nats:DEFAULT_URL, auth = myToken);
   
   // Connect with SSL/TLS enabled. 
   nats:SecureSocket secured = {
      cert: "../resource/path/to/public.crt"
   };
   nats:Listener natsListener = check new("nats://serverone:4222", secureSocket = secured);
```

## 3. Publishing

NATS is a publish-subscribe messaging system based on subjects. NATS also supports the request-reply pattern with its core communication mechanism, publish and subscribe. Messages are composed of a subject, a payload in the form of a byte array, as well as an optional 'replyTo' address field. Once connected, all outgoing messages are sent through the `nets:Client` object using either the `publishMessage` method or the `requestMessage` method. When publishing you can specify a reply to subject which can be retrieved by the receiver to respond. The `requestMessage` method will handle this behavior itself.

1. Publishing a message using `publishMessage`. This publishes message content in the form of a byte array to the given subject.
```ballerina
   string message = "hello world";
   nats:Error? result = 
         natsClient->publishMessage({ content: message.toBytes(), subject: "demo.nats.basic"});
```

2. Publishing a message using `publishMessage` with a `replyTo` subject. The reply to subject can be retrieved by the receiver to respond.
```ballerina
   string message = "hello world";
   nats:Error? result = natsClient->publishMessage({ content: message.toBytes(), subject: "demo.nats.basic",
                                                    replyTo: "demo.reply" });
```

3. Sending a request using `requestMessage`. This publishes data to a given subject and waits for a response. The replyTo is reserved for internal use as the address for the server to respond to the client with the consumer's reply.
```ballerina
   string message = "hello world";
   nats:Message|nats:Error reqReply = 
         natsClient->requestMessage({ content: message.toBytes(), subject: "demo.nats.basic"}, 5);
```

## 4. Subscribing

Subscribers listening on a subject receive messages published on that subject. If the subscriber is not actively listening on the subject, the message is not received. Subscribers can use the wildcard tokens such as * and > to match a single token or to match the tail of a subject. The subject to listen can be given as the service name or in the service config. To subscribe to a subject a `nats:Service` should be attached to an initialized `nats:Listener`. The `nats:Listener` creates the connection with the NATS server and the `nats:Service` will be asynchronously listening to messages. Multiple services can attach to the same `nats:Listener` and share the connection.

**Configurations available for services:**

`nats:ServiceConfig`
- `string` subject - Name of the subject.
- `string` queueName - Name of the queue group. 
- `nats:PendingLimits` pendingLimits - The configurations to set the limit on the maximum number of messages or maximum size of messages this consumer will hold before it starts to drop new messages waiting for the resource functions to drain the queue. Setting a value less than or equal to 0 will disable this check.
  - `int` maxMessages - Maximum number of pending messages retrieved and held by the consumer service. The default value is 65536.
  - `int` maxBytes - Total size of pending messages in bytes retrieved and held by the consumer service. The default value is 67108864.

```ballerina
   // Listen to incoming messages with the `onMessage` remote method.
   @nats:ServiceConfig {
       subject: "demo.example.*"
   }
   service nats:Service on new nats:Listener(nats:DEFAULT_URL) {
   
       remote function onMessage(nats:Message message) {
         // Do something with the message. 
       }
   }

   // Listen to incoming messages and reply directly with the `onRequest` remote method.
   service "demo.bbe" on new nats:Listener(nats:DEFAULT_URL) {
   
       // The returned message will be published to the replyTo subject of the consumed message
       remote function onRequest(nats:Message message) returns string? {
           return "Reply Message";
       }
   }
```

## 5. Samples

### 5.1. Publish-Subscribe
* Publisher 
```ballerina
import ballerinax/nats;

public function main() returns error? {
    string message = "Hello from Ballerina";
    nats:Client natsClient = check new(nats:DEFAULT_URL);
    check natsClient->publishMessage({content: message.toBytes(), subject: "demo.bbe"});
    check natsClient.close();
}
```
* Subscriber
```ballerina
import ballerina/log;
import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

service "demo.bbe" on subscription {

    remote function onMessage(nats:Message message) returns error? {
        string|error messageContent = string:fromBytes(message.content);
        if messageContent is string {
            log:printInfo("Received message: " + messageContent);
        }
    }
}
```

### 5.2. Request-Reply
* Publisher
```ballerina
import ballerina/io;
import ballerinax/nats;

public function main() returns error? {
    string message = "Hello from Ballerina";
    nats:Client natsClient = check new(nats:DEFAULT_URL);
    nats:Message reply = check natsClient->requestMessage({content: message.toBytes(), 
                                 subject: "demo.bbe"});
    string replyContent = check string:fromBytes(reply.content);
    io:println("Reply message: " + replyContent);
    check natsClient.close();
}
```

* Subscriber
```ballerina
   import ballerina/log;
   import ballerinax/nats;
   
   listener nats:Listener subscription = new(nats:DEFAULT_URL);
   service "demo.bbe" on subscription {
   
       remote function onRequest(nats:Message message) returns string {
           string|error messageContent = string:fromBytes(message.content);
           if (messageContent is string) {
               log:printInfo("Received message: " + messageContent);
           }
           return "Hello Back!";
       }
   }
```

### 5.3. Queue Groups

NATS provides a built-in load balancing feature called distributed queues. Using queue subscribers will balance message delivery across a group of subscribers.

* Publisher
```ballerina
import ballerinax/nats;

public function main() returns error? {
    string message = "Hello from Ballerina";
    nats:Client natsClient = check new(nats:DEFAULT_URL);
    check natsClient->publishMessage({content: message.toBytes(), subject: "demo.bbe"});
    check natsClient.close();
}
```
* Subscriber
```ballerina
import ballerina/log;
import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

@ServiceConfig {
    queueName: "queue-group-1"
}
service "demo.bbe" on subscription {

    remote function onMessage(nats:Message message) returns error? {
        string|error messageContent = string:fromBytes(message.content);
        if messageContent is string {
            log:printInfo("Received message: " + messageContent);
        }
    }
}
```
