package org.example.client;

import java.io.IOException;

public class Client {
    // define hostname and port number
    String hostname;
    int port;

    public Client(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public ClientInstance setClientInstance() throws IOException {
        User user = new User();
        ClientInstance clientInstance = new ClientInstance(hostname, port, user);
        clientInstance.start();
        return clientInstance;
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", 6000);

        // create a new client instance
        client.setClientInstance();
}

}
