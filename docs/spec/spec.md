# Specification: Ballerina NATS Library

_Owners_: @aashikam @shafreenAnfar  
_Reviewers_: @shafreenAnfar  
_Created_: 2020/10/28  
_Updated_: 2022/02/17  
_Edition_: Swan Lake  
_Issue_: [#2214](https://github.com/ballerina-platform/ballerina-standard-library/issues/2214)

# Introduction
This is the specification for the NATS standard library of [Ballerina language](https://ballerina.io/), which provides NATS client functionalities to produce and consume messages by connecting to the NATS server.

The NATS library specification has evolved and may continue to evolve in the future. The released versions of the specification can be found under the relevant GitHub tag.

If you have any feedback or suggestions about the library, start a discussion via a [GitHub issue](https://github.com/ballerina-platform/ballerina-standard-library/issues) or in the [Slack channel](https://ballerina.io/community/). Based on the outcome of the discussion, the specification and implementation can be updated. Community feedback is always welcome. Any accepted proposal, which affects the specification is stored under `/docs/proposals`. Proposals under discussion can be found with the label `type/proposal` in GitHub.

The conforming implementation of the specification is released to Ballerina central. Any deviation from the specification is considered a bug.

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
- `nats:Client`: Represents a single network connection. All outgoing messages are sent through the `nets:Client` object using either the `publishMessage` method or the `requestMessage` method.
```ballerina
    # Initializes the NATS client.
    #
    # + url - The NATS Broker URL. For a clustered use case, provide the URLs as a string array
    # + config - The connection configurations
    public isolated function init(string|string[] url, *ConnectionConfiguration config) returns Error?;
```

- `nats:Listener`: Represents a single network connection. A subscription service should be bound to a listener in order to receive messages.
```ballerina
    # Initializes the NATS listener.
    #
    # + url - The NATS Broker URL. For a clustered use case, provide the URLs as a string array
    # + config - The connection configurations
    public isolated function init(string|string[] url, *ConnectionConfiguration config) returns Error?;
```

**Configurations available for initializing the NATS client and listener:** 

- Connection related configurations:

```ballerina
    # Configurations related to initializing the NATS client and listener.
    public type ConnectionConfiguration record {|
        # The name of the connection. The default value is "ballerina-nats".
        string connectionName = "ballerina-nats";
        # The configurations related to connection reconnect attempts.
        RetryConfig retryConfig?;
        # The configurations related to pinging the server.
        Ping ping?;
        # The configurations related to authentication. 
        Credentials|Tokens auth?;
        # The connection's inbox prefix, which all inboxes will start with. The default value is "_INBOX".
        string inboxPrefix = "_INBOX.";
        # Turns off echoing. This prevents the server from echoing messages back to the connection if it has 
        # subscriptions on the subject being published to. The default value is false.
        boolean noEcho = false;
        # The configurations related to SSL/TLS. More details in Secured connections section.
        SecureSocket secureSocket?;
    |};
```

- Configurations related to token based authentication:
```ballerina
    public type Tokens record {|
        # The token for token-based authentication.
        string token;
    |};
```

- Configurations related to basic authentication:
```ballerina
    public type Credentials record {|
        # The username for basic authentication.
        string username;
        # The password for basic authentication.
        string password;
    |};
```

- Configurations related to facilitating a secure communication:
```ballerina
    public type SecureSocket record {|
        # Configurations associated with `crypto:TrustStore` or single certificate file that the client trusts.
        crypto:TrustStore|string cert;
        # Configurations associated with `crypto:KeyStore` or combination of certificate and private key of the client.
        crypto:KeyStore|CertKey key?;
        # SSL/TLS protocol related options.
        record {|
            Protocol name;
        |} protocol?;
    |};
```

- Combination of certificate, private key and private key password if encrypted:
```ballerina
    public type CertKey record {|
        # A file containing the certificate.
        string certFile;
        # A file containing the private key in PKCS8 format. 
        string keyFile;
        # Password of the private key if it is encrypte.
        string keyPassword?;
    |};
```

- Represents protocol options:
```ballerina
    public enum Protocol {
        SSL,
        TLS,
        DTLS
    }
```

- Configurations related to pinging the server:
```ballerina
    public type Ping record {|
        # The interval (in seconds) between the attempts of pinging the server.
        decimal pingInterval = 120;
        # The maximum number of pings the client can have in flight.
        int maxPingsOut = 2;
    |};
```

- Configurations related to connection reconnect attempts:
```ballerina
    public type RetryConfig record {|
        # Maximum number of reconnect attempts. The reconnect state is triggered when an already established 
        # connection is lost. During the initial connection attempt, the client will cycle 
        # over its server list one time regardless of the `maxReconnects` value that is set. 
        # Use 0 to turn off auto reconnecting. Use -1 to turn on infinite reconnects.
        int maxReconnect = 60;
        # The time(in seconds) to wait between the reconnect attempts to reconnect to the same server. 
        decimal reconnectWait = 2;
        # The timeout (in seconds) for the connection attempts. 
        decimal connectionTimeout = 2;
    |};
```

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

NATS is a publish-subscribe messaging system based on subjects. NATS also supports the request-reply pattern with its core communication mechanism, publish and subscribe. Messages are composed of a subject, a payload in the form of a byte array, as well as an optional 'replyTo' address field. When publishing you can specify a reply to subject which can be retrieved by the receiver to respond. The `requestMessage` method will handle this behavior itself.

```ballerina
    # Represents the message, which a NATS server sends to its subscribed services.
    public type Message record {|
        # The message content in the form of a byte array. 
        byte[] content;
        # The subject to which the message was sent to. 
        string subject;
        # The `replyTo` subject of the message. 
        string replyTo?;
    |};
```

- `publishMessage`:
```ballerina
    # Publishes data to a given subject.
    #
    # + message - The message to be published
    # + return -  `()` or else a `nats:Error` if an error occurred
    isolated remote function publishMessage(Message message) returns Error?;
```

- `requestMessage`:
```ballerina
    # Publishes data to a given subject and waits for a response.
    #
    # + message - The message to be published
    # + duration - The time (in seconds) to wait for the response
    # + return -  The response or else a `nats:Error` if an error occurred
    isolated remote function requestMessage(Message message, decimal? duration = ())
            returns Message|Error;
```

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

Subscribers listening on a subject receive messages published on that subject. If the subscriber is not actively listening on the subject, the message is not received. Subscribers can use the wildcard tokens such as `*` and `>` to match a single token or to match the tail of a subject. The subject to listen can be given as the service name or in the service config. To subscribe to a subject a `nats:Service` should be attached to an initialized `nats:Listener`. The `nats:Listener` creates the connection with the NATS server and the `nats:Service` will be asynchronously listening to messages. Multiple services can attach to the same `nats:Listener` and share the connection.

**Configurations available for services:**
```ballerina
    public type ServiceConfig record {|
        # Name of the subject. 
        string subject;
        # Name of the queue group. 
        string queueName?;
        # Parameters to set limits on the maximum number of pending messages or maximum size of pending messages.
        PendingLimits pendingLimits?;
    |};

    # The configurations to set limits on the maximum number of messages or maximum size of messages this consumer will
    # hold before it starts to drop new messages waiting for the resource functions to drain the queue.
    # Setting a value less than or equal to 0 will disable this check.
    public type PendingLimits record {|
        # Maximum number of pending messages retrieved and held by the consumer service. The default value is 65536. 
        int maxMessages;
        # Total size of pending messages in bytes retrieved and held by the consumer service. 
        # The default value is 67108864.
        int maxBytes;
    |};
```

- Attach the service to the listener directly.

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

- Attach the service dynamically.
```ballerina
   // Create a service object 
   nats:Service listenerService =
   @nats:ServiceConfig {
      subject: "demo.example.*"
   }
   service object {
      remote function onMessage(nats:Message message) {
            // Do something with the message. 
          }
      }
   };
```

**The Listener has the following functions to manage a service:** 

* `attach()` - can be used to attach a service to the listener dynamically.
```ballerina
    # Binds a service to the `nats:Listener`.
    # 
    # + s - The type descriptor of the service
    # + name - The name of the service
    # + return - `()` or else a `nats:Error` upon failure to attach
    public isolated function attach(Service s, string[]|string? name = ()) returns error?;
```

* `detach()` - can be used to detach a service from the listener.
```ballerina
   # Stops consuming messages and detaches the service from the `nats:Listener`.
   # 
   # + s - The type descriptor of the service
   # + return - `()` or else a `nats:Error` upon failure to detach
   public isolated function detach(Service s) returns error?;
```

* `start()` - needs to be called to start the listener.
```ballerina
   # Starts the registered services.
   #
   # + return - `()` or else a `nats:Error` upon failure to start the listener
   public isolated function 'start() returns error?;
```

* `gracefulStop()` - can be used to gracefully stop the listener from consuming messages.
```ballerina
   # Stops the `nats:Listener` gracefully.
   #
   # + return - `()` or else a `nats:Error` upon failure to stop the listener
   public isolated function gracefulStop() returns error?;
```

* `immediateStop()` - can be used to immediately stop the listener from consuming messages.
```ballerina
   # Stops the `nats:Listener` forcefully.
   # 
   # + return - `()` or else a `nats:Error` upon failure to stop the listener
   public isolated function immediateStop() returns error?;
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
