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
5. [Listening for Messages](#4-listening-for-messages)
6. [Samples](#5-samples)
    * 5.1. [Publish-Subscribe](#51-publish-subscribe)
    * 5.2. [Request-Reply](#52-request-reply)
    * 5.3. [Queue Groups](#53-queue-groups)

## 1. Overview
This specification elaborates on the usage of NATS library client and services/listener. NATS makes it easy for applications to communicate by sending and receiving messages. These messages are addressed by subjects and do not depend on network location.
Data is encoded and framed as a message and sent by a publisher. The message is received, decoded, and processed by one or more subscribers.
More on NATS server concepts can be found [here](https://docs.nats.io/nats-concepts/overview).  

## 2. Connection
Connections with the NATS server can be established through the NATS library client and the listener. There are multiple ways to connect. 

1. Connect to a local server on the default port.
```ballerina
   // Connecting using the NATS client.
   nats:Client natsClient = check new(nats:DEFAULT_URL);
   
   // Connecting using the NATS listener.
   nats:Listener natsListener = check new(nats:DEFAULT_URL);
```

2. Connect to one or more servers using a URL.
```ballerina
   // Connecting using the NATS client.
   nats:Client natsClient = check new(["nats://serverone:4222", "nats://servertwo:4222"]);

   // Connecting using the NATS listener.
   nats:Listener natsListener = check new(["nats://serverone:4222", "nats://servertwo:4222"]);
```

3. Connect to one or more servers with a custom configuration.
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

NATS is a publish-subscribe messaging system based on subjects. NATS also supports the request-reply pattern with its core communication mechanism, publish and subscribe. A request is published on a given subject with a reply subject, and responders listen on that subject and send responses to the reply subject. Messages are composed of a subject, a payload in the form of a byte array, as well as an optional 'replyTo' address field. Once connected, publishing is accomplished via one of three methods.

```ballerina
   // Publish with the subject and the message content.
   string message = "hello world";
   nats:Error? result = 
         natsClient->publishMessage({ content: message.toBytes(), subject: "demo.nats.basic"});

   // Publish as a request that expects a reply.
   string message = "hello world";
   nats:Message|nats:Error reqReply = 
         natsClient->requestMessage({ content: message.toBytes(), subject: "demo.nats.basic"}, 5);

   // Publish messages with a `replyTo` subject.
   string message = "hello world";
   nats:Error? result = natsClient->publish({ content: message.toBytes(), subject: "demo.nats.basic",
                                                    replyTo: "demo.reply" });
```

## 4. Listening for Messages

Subscribers listening on a subject receive messages published on that subject. If the subscriber is not actively listening on the subject, the message is not received. Subscribers can use the wildcard tokens such as * and > to match a single token or to match the tail of a subject. The subject to listen can be given as the service name or in the service config. 

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
