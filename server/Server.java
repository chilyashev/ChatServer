package server;

import server.messaging.ChatMessage;

import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.*;

public class Server implements Runnable {

    ServerSocket serverSocket = null;
    private int port = 8008;
    private boolean running = true;
    private ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public Server(int port) {
        this.port = port;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        while (running) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (!running) {
                    break;
                }
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }

            String clientId = String.format("client_%s_%d", clientSocket.getInetAddress().getCanonicalHostName(), clientSocket.getPort());
            Worker worker = new Worker(clientSocket, this, clientId);
            System.err.println("New client: " + clientId);
            Context.getInstance().addClient(clientId, worker);

            threadPool.execute(worker);
        }
        threadPool.shutdown();
        System.out.println("Server Stopped.");
    }

    public synchronized void stop() {
        running = false;
        try {
            this.serverSocket.close();
        } catch (IOException e) {
            System.err.println("error closing server");
        }
    }

    public synchronized void sendChatMessage(ChatMessage msg) {

    }
}