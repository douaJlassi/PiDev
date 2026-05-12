package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDBConnexion {

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";
    private static final String URL = "jdbc:mysql://localhost:3306/pi2";

    private Connection cnx;

    //2nd Step = CREATE A STATIC VARIABLE AS THE SAME TYPE OF THE CLASS
    private static MyDBConnexion instance;

    //1st Step = MAKE CONSTRUCTOR PRIVATE
    private MyDBConnexion(){

        try {
            cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connexion etablie!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }

    }

    //3nd Step = CREATE A STATIC METHOD TO RETURN THE INSTANCE
    public static MyDBConnexion getInstance(){
        if (instance == null) instance = new MyDBConnexion();
        return instance;
    }
    public Connection getConnection() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                System.out.println("Connexion re-etablie!");
            }
        } catch (SQLException e) {
            System.err.println("Failed to reconnect: " + e.getMessage());
        }
        return cnx;
    }

    public Connection getCnx() {
        return getConnection();
    }
}
