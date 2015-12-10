package server;

import server.messaging.ChatMessage;
import server.messaging.InvalidMessageException;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;

/**
 * Клас-работник. Върши работи.
 */
public class Worker implements Runnable {
    private String clientSessionId; // Сесия на клиента, за когото се отнася

    Socket clientSocket = null; // Сокет за връзка с клиента
    Server server; // Сървър, за да му казваме да праща неща

    BufferedReader input; // Четене от сокета
    BufferedWriter output; // Писане по сокета

    ChatUser client = null; // Клиента

    PreparedStatement stmt;
    Connection con;

    BufferedWriter sessOut; // Към файла с данни от сесията

    String sockId;

    public Worker(Socket sock, Server server, String sockId) {
        clientSocket = sock;
        this.server = server;
        this.sockId = sockId;
    }

    @Override
    public void run() {

        try {
            input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            output = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            /*
             * Протоколът:
             *      Всяко съобщение към сървъра започва с изпращане на сесия от клиента.
             *      Ако клиентът прати невалидна сесия (по-малко от 32 байта), считаме, че е нов и му създаваме сесия.
             *      Ако е пратена сесия, се проверява дали съществува. Ако съществува, се проверява дали не е изтекла.
             *      При получаване на валидна сесия се ъпдейтва файлът ѝ. Файлът на сесията е с име id-то ѝ.
             *      Във файла на сесията се пазят времето на последната заявка и id-то на клиента, за който е сесията.
             *      Сървърът винаги връща сесията (ако е валидна) преди да върне каквото и да е друго за текущото съобщение.
             * Протокола накратко:
             *      1. Прочита се id на сесията, изпратена от клиента
             *      2. Ако съществува такава, се проверява дали е изтекла
             *      3. Ако няма, се създава нова и се праща на клиента
            */

            // Опитваме да прочетем сесията
            clientSessionId = input.readLine();

            // Ако нещо се е омазало, затваряме връзката
            if (clientSessionId == null) {
                sendErrorToUser("Невалидна сесия!");
                return;
            }
            // Пратени са глупости, генерираме нова сесия
            if (clientSessionId.length() < 32) {

                // За сесия се избира пратеното от клиента, хоста и порта, от които се е вързал и текущото време
                byte[] sessId = String.format("%s%s%d%d",
                        clientSessionId,
                        clientSocket.getInetAddress().getCanonicalHostName(),
                        clientSocket.getPort(),
                        new Date().getTime())
                        .getBytes("UTF-8");
                // След това се взема md5 hash-а му

                clientSessionId = StringUtils.md5(sessId);
                server.logInfo("Нова сесия: " + clientSessionId);
            }

            File sessionFile = new File("./sessions/" + clientSessionId); // Файлът, в който ще се пазят данни за сесията
            // Ако няма файл за сесията, правим нов. Ако вече има сесия, проверяваме дали е изтекла.
            try {
                // Няма файл за сесията, правим го
                if (!sessionFile.exists()) {
                    if (sessionFile.createNewFile()) {
                        sessOut = new BufferedWriter(new FileWriter(sessionFile)); // Към файла
                        sessOut.write(String.format("%d", new Date().getTime())); // Записваме текущата дата, за да знаем после кога е изтекла
                        sessOut.newLine();
                        sessOut.flush();
                        sessOut.close();
                        server.logInfo("Файл за новата сесия: " + sessionFile.getAbsolutePath());
                    }
                } else { // Има файл за сесията, проверяваме дали е изтекла
                    BufferedReader buff = new BufferedReader(new FileReader(sessionFile)); // От файла
                    String date = buff.readLine(); // Първи ред - timestamp на последната заявка
                    String id = buff.readLine(); // Втори ред - id на потребителя, чиято сесия е това
                    buff.close();
                    // Ако е прочетено id, създаваме обект за клиента, за да работим после с него
                    if (id != null) {
                        client = new ChatUser();
                        client.id = Integer.parseInt(id);
                    }
                    if (date != null) { // Ако е прочетена дата
                        try {
                            long sessDelta = (new Date().getTime() - Long.parseLong(date)) / 1000; // Изминало време в секунди от създаването на сесията до тази заявка
                            server.logInfo(String.format("Сесията е отпреди: %d секунди", sessDelta));
                            // Ако сесията е изтекла, казваме, че е, пращаме грешка и затриваме файла
                            if (sessDelta > Constants.SESSION_TIME) {
                                sendInvalidSessionMessage(); // Грешка към клиента
                                sessionFile.delete(); // Изтриване на файла. TODO: да се проверява дали е изтрит наистина
                                Context.getInstance().removeClient(clientSessionId); // Премахване на клиента от активните
                                return;
                            } else {
                                // Сесията не е изтекла, затова записваме текущото време, с което удължаваме живота ѝ
                                sessOut = new BufferedWriter(new FileWriter(sessionFile));
                                sessOut.write(String.format("%d", new Date().getTime())); // Записваме текущата дата, за да знаем после кога е изтекла
                                sessOut.flush();
                                if (client != null && client.id > 0) {
                                    sessOut.newLine();
                                    sessOut.write(String.format("%d", client.id));
                                    sessOut.flush();
                                }
                                sessOut.newLine();
                                sessOut.flush();
                                sessOut.close();
                            }
                        } catch (NumberFormatException e) { // Ако се счупи четенето от файла на сесията
                            sendInvalidSessionMessage();
                            return;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            // Всичко е наред, пращаме OK и сесията
            output.write("OK:" + clientSessionId);
            output.newLine();
            output.flush();


            String line;
            if (clientSocket.isClosed()) {
                return;
            }

            // След това следва четене на типа на съобщението и реагиране според него
            if ((line = input.readLine()) != null) {
                if (clientSocket.isClosed()) {
                    return;
                }
                server.logInfo("Client " + clientSessionId + " says: " + line);
                ChatMessage msg = new ChatMessage();

                String msgData[] = line.split(";"); // Разделяме съобщението на части

                int msgType, sender, receiver;
                try {
                    msgType = Integer.parseInt(msgData[0]); // Определя типа на съобщението

                    msg.setType(msgType);

                    // Според типа на съобщението се извършват някакви действия
                    switch (msg.getType()) {
                        case ChatMessage.TYPE_PING:
                            // Нищо.
                            break;
                        // Добавяне на слушател, на който да може после да се пращат съобщения
                        case ChatMessage.TYPE_REGISTER_READER:
                            // Записване на worker-а в хеша с worker-и, за да може после да се говори с него.
                            client = new ChatUser();
                            client.id = Integer.parseInt(msgData[1]);
                            Context.getInstance().addClient(msgData[1], this);
                            sendMessage("OK");
                            server.broadcastOnline(client.id);
                            // sleep...

                            // Слушателят не праща нищо (защото е слушател...).
                            while ((line = input.readLine()) != null) {
                                server.logInfo("line: " + line);
                            }
                            break;
                        // Вход
                        case ChatMessage.TYPE_LOGIN:
                            // Четене на име и парола
                            String username = input.readLine();
                            String password = input.readLine();

                            con = DBSettings.getConnection();
                            assert con != null;
                            con.setAutoCommit(false);
                            // Проверява се дали в базата има такъв потребител.
                            stmt = con.prepareStatement("SELECT * FROM `user` WHERE username=? AND password=? LIMIT 0,1");
                            stmt.setString(1, username);
                            stmt.setString(2, password);

                            ResultSet res = stmt.executeQuery();


                            // Ако има такъв, входът е успешен и напълваме обекта за клиента
                            while (res.next()) {
                                client = new ChatUser();
                                client.id = res.getInt("id");
                                client.name = res.getString("name");
                                client.friends = new ArrayList<>();
                            }
                            stmt.close();
                            con.close();
                            // Ако входът е успешен
                            if (client != null) {
                                // Казваме на клиента, че е влязъл успешно
                                sendMessage("OK:" + client.id + ":" + client.name);
                                // Ъпдейтваме сесията
                                sessOut = new BufferedWriter(new FileWriter(sessionFile));
                                sessOut.write(String.format("%d", new Date().getTime()));
                                sessOut.newLine();
                                // Записваме id-то на потребителя
                                sessOut.write(String.format("%d", client.id));
                                sessOut.flush();
                                sessOut.close();
                            } else { // Грешно име/парола
                                sendInvalidUser();
                                input.close();
                                output.close();
                                clientSocket.close();
                                Context.getInstance().removeClient(clientSessionId);
                                return;
                            }
                            clientSocket.close();
                            break;
                        // Изпращане на съобщение от един клиент до друг
                        case ChatMessage.TYPE_MSG:
                            // Формат: 2;5;msg
                            // Тип;До;Съобщение

                            // Ако не са пратени трите части, съобщението е невалидно, няма смисъл да се продължава
                            if (msgData.length < 3) {
                                sendErrorToUser("Невалидно съобщение!");
                                break;
                            }

                            String msgText = msgData[2];
                            receiver = Integer.parseInt(msgData[1]);

                            // Прочитаме id-то на потребителя от сесията
                            BufferedReader sessIn = new BufferedReader(new FileReader(sessionFile));
                            String id;
                            sessIn.readLine(); // Прочитаме първия ред, но го игнорираме
                            id = sessIn.readLine();
                            sessIn.close();
                            server.logInfo("id: " + id);
                            // Ако в сесията няма id, тя е невалидна => грешка.
                            if (id == null) {
                                sendInvalidSessionMessage();
                                return;
                            }
                            // Ако клиентът се опитва да си пише сам, пращаме нещо тъпо и затваряме връзката
                            if (receiver == client.id) {
                                sendErrorToUser("Не може да говориш сам със себе си. Няма значение колко ти се иска и колко по-качествен ще е разговорът.");
                                return;
                            }

                            server.logInfo(String.format("%s, %s, %s, %s\n", msgType, client.id, receiver, msgText));

                            if (client.name == null || client.name.length() < 1){

                                con = DBSettings.getConnection();
                                assert con != null;
                                con.setAutoCommit(false);
                                // Вземаме името на клиента
                                stmt = con.prepareStatement("SELECT name FROM `user` WHERE `id` = ? LIMIT 0,1");
                                stmt.setInt(1, client.id);

                                ResultSet resultSet = stmt.executeQuery();


                                while (resultSet.next()) {
                                    client.name = resultSet.getString("name");
                                }
                                stmt.close();
                                con.close();
                            }

                            // Задаване на нужните стойности за полетата в съобщението
                            msg.setSenderId(client.id); // Кой праща
                            msg.setMessage(msgText); // Какво праща
                            msg.setReceiverId(receiver); // До кого праща
                            msg.setSenderName(client.name); // Как се казва този, дето праща
                            msg.validate(); // Валидно ли е съобщението?

                            // Казваме на сървъра, че трябва да изпрати съобщение
                            if (server.sendChatMessage(msg)) {
                                // Ако сървърът е успял да прати съобщението, казваме OK
                                sendMessage("OK");
                            } else { // Ако нещо се е счупило при пращането, казваме, че този потребител го няма
                                server.broadcastOffline(receiver);
                                output.write("-;"+receiver);
                                sendErrorToUser("Грешка при изпращането!");
                            }
                            clientSocket.close();
                            break;
                        // Списък с приятели
                        case ChatMessage.TYPE_GET_FRIENDS:
                            if (msgData.length < 2) {
                                sendErrorToUser("Невалидно съобщение!");
                            }
                            if (client == null) {
                                client = new ChatUser();
                            }
                            client.id = Integer.parseInt(msgData[1]);
                            con = DBSettings.getConnection();

                            // Вземане на приятелите от базата
                            stmt = con.prepareStatement("SELECT u.id, u.name\n" +
                                    "FROM user_friends uf\n" +
                                    "  INNER JOIN user u ON u.id = uf.friend_id\n" +
                                    "WHERE uf.user_id = ?\n");
                            stmt.setInt(1, client.id);
                            res = stmt.executeQuery();

                            ChatUser fr;
                            client.friends = new ArrayList<>();
                            // Попълване на списъка с приятели
                            while (res.next()) {
                                fr = new ChatUser();
                                fr.id = res.getInt("id");
                                fr.name = res.getString("name");
                                client.friends.add(fr);
                            }
                            stmt.close();
                            con.close();

                            // Изпращане на приятелите към клиента
                            sendMessage("start_friends");
                            for (ChatUser friend : client.friends) {
                                sendMessage(String.format("%d:%s", friend.id, friend.name));
                            }
                            sendMessage("end_friends");
                            clientSocket.close();

                            break;
                        default:
                            sendErrorToUser("Невалидно съобщение");
                            break;
                    }
                    server.logInfo("!?");
                } catch (NumberFormatException | InvalidMessageException e) {
                    sendErrorToUser("Невалидно съобщение");
                }
            }
            //Context.getInstance().removeClient(clientSessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendInvalidSessionMessage() {
        sendErrorToUser("Невалидна/изтекла сесия");
    }


    private void sendInvalidUser() {
        sendMessage("Невалиден потребител.");
        server.logError("Невалиден потребител. Убиваме връзката.");
        /*try {
            clientSocket.close();
        } catch (IOException e) {
            logError("Нещо се прецака при убиването на връзката");
            e.printStackTrace();
        }*/

    }


    /**
     * Праща съобщение за грешка до клиента и затваря връзката с него
     *
     * @param message грешката, която да се прати
     */
    public synchronized void sendErrorToUser(String message) {
        sendMessage(message);
        server.logError(String.format("[%s] Грешка: %s", clientSessionId, message));
        try {
            clientSocket.close();
        } catch (IOException e) {
            server.logError("Нещо се прецака при убиването на връзката");
            e.printStackTrace();
        }

    }

    public synchronized void sendMessage(String msg) {
        try {
            if (clientSocket.isClosed()) {
                return;
            }
            output.write(msg, 0, msg.length());
            output.newLine();
            output.flush();
            server.logInfo(String.format("[%s]send> %s\n", sockId, msg));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public ChatUser getClient() {
        return client;
    }

    public void setClient(ChatUser client) {
        this.client = client;
    }
}