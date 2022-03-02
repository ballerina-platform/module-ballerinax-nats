import ballerinax/kafka;

service kafka:Service on new kafka:Listener("localhost:9090") {
    int x = 5;
    string y = "xx";
        remote function onMessage(nats:Message message) returns nats:Error? {

        }
}
