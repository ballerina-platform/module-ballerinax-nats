import ballerinax/nats;

listener nats:Listener subscription = new;

@nats:ServiceConfig {
    subject: "demo.bbe.*"
}
service nats:Service on subscription {

    remote function onMessage(nats:Message message) {
    }
}
