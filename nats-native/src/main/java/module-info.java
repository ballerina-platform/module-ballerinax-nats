module io.ballerina.stdlib.nats {
    requires jnats;
    requires io.ballerina.jvm;
    requires org.slf4j;
    requires java-nats-streaming;
    exports org.ballerinalang.nats.basic;
    exports org.ballerinalang.nats.connection;
    exports org.ballerinalang.nats.observability;
    exports org.ballerinalang.nats.streaming;
}
