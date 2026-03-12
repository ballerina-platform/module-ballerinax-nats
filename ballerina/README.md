## Overview

NATS is a cloud-native, open-source messaging system that provides a high-performance, lightweight, and scalable communication infrastructure for modern distributed systems. The NATS connector enables seamless integration with NATS, making it easy to build reactive and event-driven applications. It supports various messaging patterns, including publish-subscribe, request-reply, and load-balanced queues, and provides advanced features such as JetStream for persistent messaging.

### Key Features

- Support for core NATS messaging (Pub/Sub, Request-Reply)
- Advanced persistent messaging with NATS JetStream
- Simplified production and consumption of messages
- Load-balanced message processing with queue groups
- Secure communication with TLS and various authentication methods
- GraalVM compatible for native image builds

### Basic usage

#### Set up the connection

First, you need to set up the connection with the NATS Basic server. The following ways can be used to connect to a
NATS Basic server.

1. Connect to a server using the default URL:
```ballerina
nats:Client natsClient = check new(nats:DEFAULT_URL);
```

2. Connect to a server using the URL:
```ballerina
nats:Client natsClient = check new("nats://serverone:4222");
```

3. Connect to one or more servers with custom configurations:
```ballerina
nats:ConnectionConfiguration config = {
    connectionName: "my-nats",
    noEcho: true
};
nats:Client natsClient = check new(["nats://serverone:4222",  "nats://servertwo:4222"],  config);
```

#### Publish messages

##### Publish messages to the NATS basic server

Once connected, publishing is accomplished via one of the three methods below.

1. Publish with the subject and the message content:
```ballerina
string message = "hello world";
nats:Error? result = 
    natsClient->publishMessage({ content: message.toBytes(), subject: "demo.nats.basic"});
```

2. Publish as a request that expects a reply:
```ballerina
string message = "hello world";
nats:AnydataMessage|nats:Error reqReply = 
    natsClient->requestMessage({ content: message.toBytes(), subject: "demo.nats.basic"}, 5);
```

3. Publish messages with a `replyTo` subject:
```ballerina
string message = "hello world";
nats:Error? result = natsClient->publish({ content: message.toBytes(), subject: "demo.nats.basic",
                                                    replyTo: "demo.reply" });
```

#### Listen to incoming messages

##### Listen to messages from a NATS server

1. Listen to incoming messages with the `onMessage` remote method:
```ballerina
// Binds the consumer to listen to the messages published to the 'demo.example.*' subject
@nats:ServiceConfig {
    subject: "demo.example.*"
}
service nats:Service on new nats:Listener(nats:DEFAULT_URL) {

    remote function onMessage(nats:AnydataMessage message) {
    }
}
```

2. Listen to incoming messages and reply directly with the `onRequest` remote method:
```ballerina
// Binds the consumer to listen to the messages published to the 'demo.example.*' subject
@nats:ServiceConfig {
    subject: "demo.example.*"
}
service nats:Service on new nats:Listener(nats:DEFAULT_URL) {

    // The returned message will be published to the replyTo subject of the consumed message
    remote function onRequest(nats:AnydataMessage message) returns string? {
        return "Reply Message";
    }
}
```

### Advanced usage

#### Set up TLS

The Ballerina NATS package allows the use of TLS in communication. This setting expects a secure socket to be
set in the connection configuration as shown below.

##### Configure TLS in the `nats:Listener`
```ballerina
nats:SecureSocket secured = {
    cert: {
        path: "<path>/truststore.p12",
        password: "password"
    },
    key: {
        path: "<path>/keystore.p12",
        password: "password"
    }
};
nats:Listener natsListener = check new("nats://serverone:4222", secureSocket = secured);
```

##### Configure TLS in the `nats:Client`
```ballerina
nats:SecureSocket secured = {
    cert: {
        path: "<path>/truststore.p12",
        password: "password"
    },
    key: {
        path: "<path>/keystore.p12",
        password: "password"
    }
};
nats:Client natsClient = check new("nats://serverone:4222", secureSocket = secured);
```

### Report issues

To report bugs, request new features, start new discussions, view project boards, etc., go to the [Ballerina standard library parent repository](https://github.com/ballerina-platform/ballerina-standard-library).

### Useful links

- Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
- Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.