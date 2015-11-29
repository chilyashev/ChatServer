import server.Server;

import java.util.Scanner;

/**
 * Created by Mihail Chilyashev on 11/29/15.
 * All rights reserved, unless otherwise noted.
 */
public class Main {
    public static void main(String[] args) {
        Server server = new Server(8008);
        new Thread(server).start();
        Scanner s = new Scanner(System.in);

        while(!s.nextLine().equals("exit")){
            // Waiting...
        }
        server.stop();
    }
}
