package com.mycompany.mutiprocess.message_system;

import com.google.gson.Gson;
import com.mycompany.mutiprocess.ms_client.Message;
import com.mycompany.mutiprocess.ms_client.MessageType;
import com.mycompany.mutiprocess.ms_client.MsClient;
import com.mycompany.mutiprocess.ms_client.MsClientImpl;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class MsServer {

    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final int MS_PORT = 8081;
    private MessageSystem messageSystem;

    private final Gson gson = new Gson();

    public static void main(String[] args) {
        new MsServer().start();
    }

    private void start() {
        messageSystem = new MessageSystemImpl();

        try (ServerSocket serverSocket = new ServerSocket(MS_PORT)) {
            while (!Thread.currentThread().isInterrupted()) {
                logger.info("waiting for client connection");
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> clientHandler(clientSocket));
            }
        } catch (Exception ex) {
            logger.error("error", ex);
        }
        executor.shutdown();
    }

    private void clientHandler(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            MsClient msClient = null;

            while (true) {
                String input = in.readLine();
                if (input != null) {
                    Message message = gson.fromJson(input, Message.class);
                    if (message.getType() == MessageType.REGISTER_CLIENT) {

                        msClient = new MsClientImpl(message.getFromClientId(), message.getFrom());
                        messageSystem.addClient(msClient, clientSocket);
                    } else if (message.getType() == MessageType.REMOVE_CLIENT) {
                        messageSystem.removeClient(msClient);
                        break;
                    } else {
                        messageSystem.newMessage(message);
                    }
                }
            }
            clientSocket.close();
        } catch (Exception ex) {
            logger.error("error", ex);
        }
    }
}
