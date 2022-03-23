import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

service "demo" on subscription {
	remote function onMessage(nats:Message message) returns nats:Error? {

	}
}
