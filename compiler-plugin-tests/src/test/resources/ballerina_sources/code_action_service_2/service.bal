import ballerinax/nats;

listener nats:Listener subscription = new(nats:DEFAULT_URL);

service "demo" on subscription {
    int x = 5;
    string y = "xx";
}
