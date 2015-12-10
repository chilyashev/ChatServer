package server;

import server.messaging.ChatMessage;

import java.net.*;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.io.*;

/**
 * Класът на сървъра.
 */
public class Server implements Runnable {

    /**
     * Сървър
     */
    ServerSocket serverSocket = null;
    private int port;

    private MessageAware messageReceiver;

    private boolean running = true;
    private ThreadPoolExecutor threadPool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    /**
     * Създава сървъра
     * @param port
     * @param messageReceiver
     */
    public Server(int port, MessageAware messageReceiver) {
        this.port = port;
        this.messageReceiver = messageReceiver;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        logInfo(String.format("Сървърът е стартиран на порт %d.", port));
        // (Уж) Безкраен цикъл, който очаква нови връзки.
        while (running) {
            Socket clientSocket = null; // за комуникация с клиента
            try {
                clientSocket = serverSocket.accept(); // заспива и чака връзка
            } catch (IOException e) {
                if (!running) { // проверяваме дали все още трябва да работи. Ако не трябва, излизаме от цикъла.
                    break;
                }
                logError("Грешка: " + e.getMessage());
                throw new RuntimeException(
                        "Error accepting client connection", e);
            }

            String clientId = String.format("%s:%d", clientSocket.getInetAddress().getCanonicalHostName(), clientSocket.getPort());

            // Създаваме обработваща нишка
            Worker worker = new Worker(clientSocket, this, clientId);
            logInfo("Нова връзка: " + clientId);
            threadPool.execute(worker); // Добавяне на нишката в пула от нишки
        }
        // Ако цикълът е свършил, значи сървърът трябва да умре.
        threadPool.shutdown(); // Спираме пула от нишки
        logInfo("Сървърът е спрян.");
    }

    /**
     * Праща информация към интерфейса
     * @param msg информация
     */
    public synchronized void logInfo(String msg) {
        messageReceiver.onMessage(new Message(Message.TYPE_INFO, msg));
    }

    /**
     * Праща грешка към интерфейса
     * @param msg грешка
     */
    public synchronized void logError(String msg) {
        messageReceiver.onMessage(new Message(Message.TYPE_ERROR, msg));
    }

    /**
     * Спиране на сървъра
     */
    public synchronized void stop() {
        running = false; // трябва да умре
        try {
            this.serverSocket.close(); // затваряме сокета
        } catch (IOException e) {
            logError("Грешка при спиране на сървъра");
        }
    }

    /**
     * Изпращане на съобщение до някого
     * @param msg съобщението до някого
     * @return true, ако е изпратено и false, ако не е
     */
    public synchronized boolean sendChatMessage(ChatMessage msg) {
        Worker tmp = null;
        boolean sent = false;
        // Обикаляме всички "слушатели" (ConversationHandlerThread от клиента)
        for (Object o : Context.getInstance().getClients().values()) {
            tmp = (Worker) o;
            // Ако се намери слушател, му пеем
            if (tmp.getClient().id == msg.getReceiverId()) {
                // Изпраща съобщение към клиента
                tmp.sendMessage(String.format("msg;%d;%s;%s", msg.getSenderId(), msg.getSenderName(), msg.getMessage()));
                sent = true;
                break;
            }

        }
        return sent;
    }

    /**
     * Изпраща съобщение до всички
     * @param msg
     */
    public void broadcast(ChatMessage msg) {
        Worker tmp = null;
        for (Object o : Context.getInstance().getClients().values()) {
            tmp = (Worker) o;
            tmp.sendMessage(msg.getMessage());
        }
    }

    /**
     * Казване на всички, че някой е дошъл онлайн
     * @param id Кой?
     */
    public void broadcastOnline(int id) {
        Worker tmp = null;
        for (Object o : Context.getInstance().getClients().values()) {
            tmp = (Worker) o;
            // Не ме интересува, че съм дошъл онлайн
            if(tmp.client.getId() == id){
                continue;
            }
            tmp.sendMessage(String.format("+;%d", id));
        }
    }

    /**
     * Казване на всички, че някой вече е офлайн
     * @param id Кой?
     */
    public void broadcastOffline(int id) {
        Worker tmp = null;
        for (Object o : Context.getInstance().getClients().values()) {
            tmp = (Worker) o;
            tmp.sendMessage(String.format("-;%d", id));
        }
    }
}