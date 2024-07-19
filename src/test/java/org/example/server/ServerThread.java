package org.example.server;

import org.json.JSONObject;

import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.sql.SQLException;
import java.util.regex.Pattern;


public class ServerThread {
    private Socket socket;

    public ServerThread(Socket socket) {
        this.socket = socket;

    }

    public JSONObject readUserInput(BufferedReader input) throws IOException {
        String clientInput;
        StringBuilder clientIn = new StringBuilder();

        String regex = "^\\{.*\\}$";
        Pattern pattern = Pattern.compile(regex);

        // iterate until the end of the json data
        while ((clientInput = input.readLine()) != null) {
            if (pattern.matcher(clientInput).matches()) {
                clientIn.append(clientInput);
                break;
            }
            clientIn.append(clientInput);

            if (clientInput.equals("}")) {
                break;
            }
        }

        // converting into a json format
        JSONObject jsonObject = new JSONObject(clientIn.toString());
        return jsonObject;
    }

    public void start() throws IOException, MessagingException {

        System.out.println("Thread started");

        try (
                BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
        ){

            // get user input
            JSONObject clientRequest;
            while ((clientRequest = this.readUserInput(input)) != null) {
                System.out.println(socket.getInetAddress().getHostAddress() + " - - " + clientRequest.toString());

                Controller exec = new Controller(clientRequest);

                String response = exec.run().toString();

                // send  back to client
                output.println(response);
            }

        } catch (IOException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            socket.close();
        }

        // start a thread
}
}
