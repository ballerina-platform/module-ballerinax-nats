# Proposal: Data binding support for NATS

_Owners_: @shafreenAnfar @aashikam @dilanSachi     
_Reviewers_: @shafreenAnfar @aashikam  
_Created_: 2022/05/13  
_Issues_: [#2817](https://github.com/ballerina-platform/ballerina-standard-library/issues/2817) [#2781](https://github.com/ballerina-platform/ballerina-standard-library/issues/2781) [#2880](https://github.com/ballerina-platform/ballerina-standard-library/issues/2880)

## Summary

Data binding helps to access the incoming and outgoing message data in the user's desired type. Similar to the Ballerina HTTP package, subtypes of JSON, XML will be the supported types. This proposal discusses ways to provide data binding on the NATS client and consumer service side.

## Goals

- Improve user experience by adding data binding support for sending messages with `nats:Client` and receiving messages with `nats:Service`.

## Motivation

As of now, the Ballerina `nats` package does not provide direct data binding for sending and receiving messages. Only `nats:Message` is the supported data type to send and receive messages which only support `byte[]` as the message content type. Therefore, users have to do data manipulations by themselves. With this new feature, the user experience can be improved by introducing data binding to reduce the burden of developers converting byte data to the desired format as discussed in the next section.

## Description

Currently, when sending a message or a request using `nats:Client`, user has to convert the message to byte[].

```ballerina
type Person record {|
    string name;
    int age;
|};

Person person = {
    age: 10,
    name: "Harry"
};

// Publish a message 
check natsClient->publishMessage({ content: person.toString().toBytes(), subject: "demo" });

// Publish as a request that expects a reply
nats:Message replyMessage = check natsClient->requestMessage({ content: person.toString().toBytes(), 
                                                                      subject: "demo" });
```

When receiving the same message,

```ballerina
service "demo" on new nats:Listener(nats:DEFAULT_URL) {

    remote function onMessage(nats:AnydataMessage message) returns nats:Error? {
        string messageContent = check string:fromBytes(message.content);
        Person person = check value:fromJsonStringWithType(messageContent);
    }
}
```

Receiving the message as a request,
```ballerina
service "demo" on new nats:Listener(nats:DEFAULT_URL) {

    remote function onRequest(nats:AnydataMessage message) returns anydata|nats:Error? {
        string messageContent = check string:fromBytes(message.content);
        Person person = check value:fromJsonStringWithType(messageContent);
        return "New person received";
    }
}
```

Instead of this, if data binding support is introduced, user can easily send and receive the messages in the desired format.
For this purpose, we will introduce a new record for sending and receiving.
```ballerina
public type AnydataMessage record {|
    // The message content, which can of type anydata
    anydata content;
    // The subject to which the message was sent to
    string subject;
    // The `replyTo` subject of the message
    string replyTo?;
|};
```
With these, user can create user-defined subtypes of the above record to achieve data binding as shown below.
```ballerina
check natsClient->publishMessage({ content: person, subject: "demo" });

public type PersonMessage record {|
    *nats:AnydataMessage;
    Person content;
|};

PersonMessage person = check natsClient->requestMessage({ content: person, subject: "demo" });
```

```ballerina
service "demo" on new nats:Listener(nats:DEFAULT_URL) {

    remote function onMessage(PersonMessage person) returns nats:Error? {
    }
}

service "demo" on new nats:Listener(nats:DEFAULT_URL) {

    remote function onRequest(PersonMessage person) returns anydata|nats:Error? {
    }
}
```

### Consuming messages with nats:Service

`nats:Listener` connects to the NATS server and allows the `nats:Service` to subscribe to a given subject to consume messages. The messages are received by the `onMessage` remote method and the requests are received by the `onRequest` remote method in `nats:Service`.

```ballerina
remote function onMessage(nats:Message message) returns nats:Error? {}

remote function onRequest(nats:Message message) returns anydata|nats:Error? {}
```

This will be updated to accept the above-mentioned parameter types. User can create a subtype of `nats:AnydataMessage` and use it in the function signature or directly use a subtype of `anydata` to get the payload binded directly.
Therefore, following scenarios will be available for the user.

- **onMessage remote function**

```ballerina
remote function onMessage(anydata data) returns nats:Error? {}
```
```ballerina
remote function onMessage(nats:AnydataMessage message, anydata data) returns nats:Error? {}
```
- **onRequest remote function**
```ballerina
remote function onRequest(anydata data) returns anydata|nats:Error? {}
```
```ballerina
remote function onRequest(nats:AnydataMessage message, anydata data) returns anydata|nats:Error? {}
```

### Producing messages with nats:Client

- **publishMessage**

The `nats:Client` has `publishMessage(nats:Message message)` API to send data to the NATS server. This will be updates as,
```ballerina
# Publishes data to a given subject.
# ```ballerina
# check natsClient->publishMessage(message);
# ```
#
# + message - The message to be published
# + return -  `()` or else a `nats:Error` if an error occurred
isolated remote function publishMessage(AnydataMessage message) returns Error?;
```

Whatever the data type given as the value will be converted to a byte[] internally and sent to the NATS server. If the data binding fails, a `nats:Error` will be returned from the API.

- **requestMessage**

The `nats:Client` has `requestMessage(nats:Message message)` API to send a request to the NATS server. This returns the reply message or an error if it times out.

That way, user can receive reply messages in the desired type via the following way.

```ballerina
public type Person record {|
    string name;
    string age;
|};

public type PersonRecord record {|
    *nats:AnydataMessage;
    Person value;
|};

PersonRecord person = check natsClient->requestMessage(message, 5);
```

To allow this `requestMessage` function will also be updated to return the desired type,

```ballerina
isolated remote function requestMessage(nats:AnydataMessage message, decimal? duration = (), 
                                          typedesc<json|xml[]|nats:AnydataMessage> T = <>) returns T|nats:Error;
```

> With this new data binding improvement, the compiler plugin validation for `onMessage` and `onRequest` remote functions will also be updated to allow types other than `nats:Message`.

## Testing

- Testing the runtime data type conversions on `nats:Client` and `nats:Service`.
- Testing compiler plugin validation to accept new data types.
