package server;

import server.messaging.ChatMessage;
import server.messaging.InvalidMessageException;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

public class Worker implements Runnable {
    private String clientId;

    Socket clientSocket = null;
    Server server;

    BufferedReader input;
    BufferedWriter output;

    public Worker(Socket sock, Server server, String clientId) {
        clientSocket = sock;
        this.server = server;
        this.clientId = clientId;
    }

    @Override
    public void run() {

        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            // Login
            output.write("OK:" + clientId);
            output.newLine();
            output.flush();

            String username = input.readLine();
            String password = input.readLine();

            Connection con = DBSettings.getConnection();
            assert con != null;
            con.setAutoCommit(false);
            PreparedStatement stmt = con.prepareStatement("SELECT * FROM `user` WHERE username=? AND password=? LIMIT 0,1");
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet res = stmt.executeQuery();

            ChatUser clit = null;

            while (res.next()) {
                clit = new ChatUser();
                clit.id = res.getInt("id");
                clit.name = res.getString("name");
                clit.friends = new ArrayList<>();
            }

            if (clit != null) {
                sendMessage("OK:" + clit.id);
            } else {
                sendInvalidUser();
                input.close();
                output.close();
                clientSocket.close();
                Context.getInstance().removeClient(clientId);
                return;
            }

            String line;
            while ((line = input.readLine()) != null) {
                if (clientSocket.isClosed()) {
                    break;
                }
                System.out.println("Client " + clientId + " says: " + line);
                ChatMessage msg = new ChatMessage();

                String msgData[] = line/*.substring(0, 16)*/.split(";");

                int msgType = 0, sender = 0, receiver = 0;
                // 0001;0004;0005;cockcockcockkokokokokokokokokokokokoko
                try {
                    msgType = Integer.parseInt(msgData[0]);
                    sender = Integer.parseInt(msgData[1]);

                    msg.setType(msgType);
                    msg.setSenderId(sender);

                    switch (msg.getType()) {
                        case ChatMessage.TYPE_MSG:
                            if (line.length() < 16) {
                                sendErrorToUser("Invalid message!");
                                break;
                            }
                            String msgText = line.substring(15);
                            receiver = Integer.parseInt(msgData[2]);

                            System.err.printf("%s, %s, %s, %s\n", msgType, sender, receiver, msgText);
                            msg.setReceiverId(receiver);
                            msg.setMessage(msgText);
                            msg.setSenderName(clit.name);
                            msg.validate();

                            server.sendChatMessage(msg);
                            //sendToReceiver(String.format("Sending from %s to %d", clit.name, msg.getReceiverId()));
                            sendMessage("You said: " + msg.getMessage());
                            break;
                        case ChatMessage.TYPE_GET_FRIENDS:
                            clit.loadFriends();
                            stmt = con.prepareStatement("SELECT u.id, u.name\n" +
                                    "FROM user_friends uf\n" +
                                    "  INNER JOIN user u ON u.id = uf.friend_id\n" +
                                    "WHERE uf.user_id = ?\n");
                            stmt.setInt(1, clit.id);
                            res = stmt.executeQuery();

                            ChatUser fr;
                            clit.friends = new ArrayList<>();
                            while (res.next()) {
                                fr = new ChatUser();
                                fr.id = res.getInt("id");
                                fr.name = res.getString("name");
                                clit.friends.add(fr);
                            }

                            sendMessage("start_friends");
                            for (ChatUser friend : clit.friends) {
                                sendMessage(String.format("%d:%s", friend.id, friend.name));
                            }
                            sendMessage("end_friends");
                            break;
                        default:
                            sendErrorToUser("Невалидно съобщение");
                            break;
                    }
                    System.err.println("!?");
                    //msg.send();
                } catch (NumberFormatException | InvalidMessageException e) {
                    sendErrorToUser("Невалидно съобщение");
                    break;
                }
            }

            Context.getInstance().removeClient(clientId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void sendInvalidUser() {
        sendMessage("Невалиден потребител.");
        logError("Невалиден потребител. Убиваме връзката.");
        /*try {
            clientSocket.close();
        } catch (IOException e) {
            logError("Нещо се прецака при убиването на връзката");
            e.printStackTrace();
        }*/

    }

    private void logError(String s) {

    }

    private void sendErrorToUser(String message) {
        sendMessage(message);
        //logError(String.format("[%s (id=%d)] Грешка: %s", getName(), getId(), message));
        try {
            clientSocket.close();
        } catch (IOException e) {
            //logError("Нещо се прецака при убиването на връзката");
            e.printStackTrace();
        }

    }

    public void sendMessage(String msg) {
        try {
            output.write(msg, 0, msg.length());
            output.newLine();
            output.flush();
            System.out.println("send> " + msg);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

}