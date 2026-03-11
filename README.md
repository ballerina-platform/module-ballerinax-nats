# Overview

[![Build](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/build-timestamped-master.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/build-timestamped-master.yml)
[![codecov](https://codecov.io/gh/ballerina-platform/module-ballerinax-nats/branch/master/graph/badge.svg)](https://codecov.io/gh/ballerina-platform/module-ballerinax-nats)
[![Trivy](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/trivy-scan.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/trivy-scan.yml)
[![GraalVM Check](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/build-with-bal-test-graalvm.yml/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/actions/workflows/build-with-bal-test-graalvm.yml)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerinax-nats.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/commits/master)

NATS is a cloud-native, open-source messaging system that provides a high-performance, lightweight, and scalable communication infrastructure for modern distributed systems. The Ballerina `ballerinax/nats` connector enables seamless integration with NATS, allowing you to build reactive and event-driven applications with ease. It supports various messaging patterns, including publish-subscribe, request-reply, and load-balanced queues, while also providing advanced features like JetStream for persistent messaging.

## Key Features

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

The Ballerina NATS library allows the use of TLS in communication. This setting expects a secure socket to be
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

## Issues and projects 

Issues and Projects tabs are disabled for this repository as this is part of the Ballerina Standard Library. To report bugs, request new features, start new discussions, view project boards, etc. please visit Ballerina Standard Library [parent repository](https://github.com/ballerina-platform/ballerina-standard-library). 

This repository only contains the source code for the library.

## Build from the source

### Set up the prerequisites

* Download and install Java SE Development Kit (JDK) version 21 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/downloads/)

   * [OpenJDK](https://adoptium.net/)

        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
2. Download and install Docker as follows. (The NATS library is tested with a docker-based integration test environment. 
The before suite initializes the docker container before executing the tests).
   
   * Installing Docker on Linux
   
        > **Note:** These commands retrieve content from the `get.docker.com` website in a quiet output-document mode and installs it.
   
          wget -qO- https://get.docker.com/ | sh
   
   * For instructions on installing Docker on Mac, go to <a target="_blank" href="https://docs.docker.com/docker-for-mac/">Get Started with Docker for Mac</a>.
  
   * For information on installing Docker on Windows, goo to <a target="_blank" href="https://docs.docker.com/docker-for-windows/">Get Started with Docker for Windows</a>.

### Build the source

Execute the commands below to build from source.

1. To build the library:
   ```    
   ./gradlew clean build
   ```

2. To run the tests:
   ```
   ./gradlew clean test
   ```
3. To build the library without the tests:
   ```
   ./gradlew clean build -x test
   ```
4. To debug package implementation:
   ```
   ./gradlew clean build -Pdebug=<port>
   ```
5. To debug the library with Ballerina language:
   ```
   ./gradlew clean build -PbalJavaDebug=<port>
   ```
6. Publish ZIP artifact to the local `.m2` repository:
   ```
   ./gradlew clean build publishToMavenLocal
   ```
7. Publish the generated artifacts to the local Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToLocalCentral=true
   ```
8. Publish the generated artifacts to the Ballerina central repository:
   ```
   ./gradlew clean build -PpublishToCentral=true
   ```

## Contribute to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful links

* For more information go to the [`nats` library](https://lib.ballerina.io/ballerinax/nats/latest).
* For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/learn/by-example/).
* Chat live with us via our [Discord server](https://discord.gg/ballerinalang).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
