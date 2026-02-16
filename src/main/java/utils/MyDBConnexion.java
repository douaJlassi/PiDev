package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final String URL = "jdbc:mysql://localhost:3306/pidev";

    private Connection connection;

    private static MyDBConnexion instance;

    private MyDBConnexion() {
        try{
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to the database");
        }catch (SQLException e){
            System.err.println(e.getMessage());
        }
    }

    public static MyDBConnexion getInstance() {
        if (instance == null) {
            instance = new MyDBConnexion();
        }
        return instance;
    }
    public Connection getConnection() {
        return connection;
    }
}