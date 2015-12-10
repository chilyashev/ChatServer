package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Mihail Chilyashev on 11/29/15.
 * All rights reserved, unless otherwise noted.
 */
public class DBSettings {
    public static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    public static final String DB_URL = "jdbc:mysql://localhost/chat";

    public static final String USER = "java";
    public static final String PASS = "123";

    public static Connection getConnection() {
        System.out.println("Connecting to database...");
        try {
            Class.forName("com.mysql.jdbc.Driver");
            return  DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

}
