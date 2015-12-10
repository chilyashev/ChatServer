package server.ui.screens;

import server.Context;
import server.Message;
import server.MessageAware;
import server.Server;
import server.messaging.ChatMessage;
import server.ui.AbstractScreen;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.sql.rowset.serial.SerialRef;

/**
 * Главен екран за управление на сървъра
 */
public class MainController extends AbstractScreen implements MessageAware {

    Context context;

    // FXML controls
    @FXML
    private Button startStopButton;
    @FXML
    private Label errorLabel;
    @FXML
    private ListView<Message> logOutputList;
    @FXML
    TextField sendToAllField;
    @FXML
    public TextField portField;
    // eo FXML controls

    private boolean serverRunning = false;

    private Server server;
    private Thread serverThread;

    public MainController() {
        context = Context.getInstance();
    }


    @Override
    public void init() {
        // crap
        logOutputList.setCellFactory(new Callback<ListView<Message>,
                                             ListCell<Message>>() {
                                         @Override
                                         public ListCell<Message> call(ListView<Message> list) {
                                             return new ColorRectCell();
                                         }
                                     }
        );
    }

    /**
     * Извиква се при затваряне на прозореца. Служи за доубиване на сървъра
     */
    @Override
    public void close() {
        if (serverRunning) {
            serverRunning = false;
            server.stop();
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Извиква се при натискане на Старт/Стоп бутона. Пуска или спира сървъра
     * @param actionEvent JavaFX event
     */
    public void startStopAction(ActionEvent actionEvent) {
        // Ако сървърът не е пуснат, трябва да го пуснем
        if (!serverRunning) {
            try {
                // Проверява дали въведеното в полето за порт е валидно число и в разрешения интервал
                int port = Integer.parseInt(portField.getText());
                if (port < 1024 || port > 65535) {
                    throw new NumberFormatException();
                }
                // Създава сървъра
                server = new Server(port, this);

                // Пуска сървъра
                serverThread = new Thread(server);
                serverThread.start();
                logInfo("Стартиране...");
                serverRunning = true;
                startStopButton.setText("Стоп");
            } catch (NumberFormatException ex) {
                errorLabel.setText("Невалиден порт. Валидните стойности са между 1024 и 65535");
            }
        } else { // Ако е пуснат, го убиваме
            serverRunning = false;
            server.stop();
            try {
                serverThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startStopButton.setText("Старт");
        }
    }

    /**
     * Логва информация в главния прозорец на сървъра
     * @param s информация
     */
    private void logInfo(String s) {
        logOutputList.getItems().add(new Message(Message.TYPE_INFO, s));
        logOutputList.scrollTo(logOutputList.getItems().size() - 1);
    }

    /**
     * Логва стандартно съобщение в екрана на сървъра
     * @param message Стандартно съобщение
     */
    private void logMessage(Message message) {
        logOutputList.getItems().add(message);
        logOutputList.scrollTo(logOutputList.getItems().size() - 1);
    }

    /**
     * Извиква се, когато нещо се случи в друга нишка
     * @param message Нещо
     */
    @Override
    public synchronized void onMessage(Message message) {
        Platform.runLater(new Runnable() {
            @Override
            public synchronized void run() {
                logMessage(message);
                //logOutputList.getItems().add(new Message(Message.TYPE_ERROR, "Received invalid message from the server!"));
            }
        });
    }

    @Override
    public void sessionStatusChange(boolean closed) {
        System.err.println("Session status changed. Closed: " + closed);
    }

    /**
     * Изпраща до всички
     * @param actionEvent JavaFX event
     */
    public void sendToAll(ActionEvent actionEvent) {
        String msgText = sendToAllField.getText();
        ChatMessage msg = new ChatMessage();
        msg.setMessage(msgText);
        msg.setSenderName("SERVER");
        server.broadcast(msg);
    }


    static class ColorRectCell extends ListCell<Message> {
        @Override
        public void updateItem(Message item, boolean empty) {
            super.updateItem(item, empty);
            Rectangle rect = new Rectangle(20, 20);
            if (item != null) {
                rect.setFill(Color.web(item.getColor()));
                /*setGraphic(rect);*/
                Label label = new Label(item.toString());
                HBox box = new HBox();
                box.getChildren().addAll(rect, label);
                setGraphic(box);
            }
        }
    }


}
