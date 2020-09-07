Ballerina NATS Library
===================

[![Build](https://github.com/ballerina-platform/module-ballerinax-nats/workflows/Build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/actions?query=workflow%3ABuild)
[![Daily build](https://github.com/ballerina-platform/module-ballerinax-nats/workflows/Daily%20build/badge.svg)](https://github.com/ballerina-platform/module-ballerinax-nats/actions?query=workflow%3A%22Daily+build)
[![GitHub Last Commit](https://img.shields.io/github/last-commit/ballerina-platform/module-ballerina-sql.svg)](https://github.com/ballerina-platform/module-ballerina-sql/commits/master)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

The NATS library is one of the standard library modules of the<a target="_blank" href="https://ballerina.io/"> Ballerina
</a> language.

For more information on the operations supported by the module, which include the below, go to [The NATS Module](https://ballerina.io/swan-lake/learn/api-docs/ballerina/nats/).

- Point to point communication (Queues)
- Pub/Sub (Topics)
- Request/Reply

For example demonstrations of the usage, go to [Ballerina By Examples](https://ballerina.io/swan-lake/learn/by-example/nats-basic-client.html).

## Building from the Source

### Setting Up the Prerequisites

1. Download and install Java SE Development Kit (JDK) version 8 (from one of the following locations).

   * [Oracle](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
   
   * [OpenJDK](http://openjdk.java.net/install/index.html)
   
        > **Note:** Set the JAVA_HOME environment variable to the path name of the directory into which you installed JDK.
     
2. Download and install Docker as follows. (The NATS library is tested with a docker-based integration test environment. 
The before suite initializes the docker container before executing the tests).
   
   * Installing Docker on Linux
   
        > **Note:** These commands retrieve content from the `get.docker.com` website in a quiet output-document mode and installs it.
   
          wget -qO- https://get.docker.com/ | sh
   
   * For instructions on installing Docker on Mac, go to <a target="_blank" href="https://docs.docker.com/docker-for-mac/">Get Started with Docker for Mac</a>.
  
   * For information on installing Docker on Windows, goo to <a target="_blank" href="https://docs.docker.com/docker-for-windows/">Get Started with Docker for Windows</a>.

### Building the Source

Execute the commands below to build from source.

1. To build the library:
        
        ./gradlew clean build

2. To run the integration tests:

        ./gradlew clean test

3. To build the module without the tests:

        ./gradlew clean build -x test

4. To debug the tests:

        ./gradlew clean build -Pdebug=<port>

## Contributing to Ballerina

As an open source project, Ballerina welcomes contributions from the community. 

You can also check for [open issues](https://github.com/ballerina-platform/module-ballerinax-nats/issues) that interest
 you. We look forward to receiving your contributions.

For more information, go to the [contribution guidelines](https://github.com/ballerina-platform/ballerina-lang/blob/master/CONTRIBUTING.md).

## Code of Conduct

All contributors are encouraged to read the [Ballerina Code of Conduct](https://ballerina.io/code-of-conduct).

## Useful Links

* Discuss about code changes of the Ballerina project in [ballerina-dev@googlegroups.com](mailto:ballerina-dev@googlegroups.com).
* Chat live with us via our [Slack channel](https://ballerina.io/community/slack/).
* Post all technical questions on Stack Overflow with the [#ballerina](https://stackoverflow.com/questions/tagged/ballerina) tag.
