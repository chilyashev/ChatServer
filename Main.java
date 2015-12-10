import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server.ChatUser;
import server.Server;
import server.ui.ScreenContainer;

import java.util.Scanner;

/**
 * Created by Mihail Chilyashev on 11/29/15.
 * All rights reserved, unless otherwise noted.
 */
public class Main extends Application {


    @Override
    public void start(Stage primaryStage) throws Exception {

        ScreenContainer mainScreen = new ScreenContainer();

        // Adding the screens
        mainScreen.loadScreen("main", "/screens/main.fxml");
//        mainScreen.loadScreen("simulation", "/screens/simulation.fxml");
//        mainScreen.loadScreen("simulation_results", "/screens/simulation_results.fxml");


        // Showing the main screen
        mainScreen.showScreen("main");

        primaryStage.setOnCloseRequest(event -> {
            mainScreen.closeScreen();
        });

        // Displaying the stage
        Group root = new Group();
        root.getChildren().addAll(mainScreen);
        Scene scene = new Scene(root, 600, 480);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Chat Server");
        primaryStage.show();

    }


    public static void main(String[] args) {
        launch(args);
    }
    public static void smain(String[] args) {
        Server server = new Server(8008, null);
        new Thread(server).start();
        Scanner s = new Scanner(System.in);

        while(!s.nextLine().equals("exit")){
            // Waiting...
        }
        server.stop();
    }
}
