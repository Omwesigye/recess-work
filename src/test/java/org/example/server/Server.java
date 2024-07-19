package org.example.server;

import java.io.IOException;
import java.net.*;

public class Server {
    public static void main(String[] args) throws IOException {
        // define server default port
        int port = 6000;

        try (// start server
             ServerSocket socket = new ServerSocket(port)) {

            System.out.println("waiting for connection");

            while (true) {
                Socket sock = socket.accept();

                System.out.println("New client accepted");

                ServerThread serverThread = new ServerThread(sock);
                serverThread.start();

            }
            // accept incoming connection  constantly and create a thread

        } catch (Exception e) {
            e.printStackTrace();
     }
    }
}
