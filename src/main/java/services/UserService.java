package services;

import entities.user;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements CRUD<String, user>{
    private Connection connection;
    private String nom;
    public UserService() {connection= MyDBConnexion.getInstance().getConnection();}

    @Override
    public void insertOne(user user) throws SQLException {
        String req = "INSERT INTO `users`(`nom`,`prenom`,`type`) VALUES " +
                "('"+user.getNom()+"','"+user.getPrenom()+"','"+user.getType()+"')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void updateOne(String s, user user) throws SQLException {
        String req="UPDATE users SET nom='"+user.getNom()+"',prenom='"+user.getPrenom()+"'" +
                ",type='"+user.getType()+"' WHERE nom='"+s+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(user user) throws SQLException {
        String req = "DELETE FROM `users` WHERE `nom` = " + "'" + user.getNom() + "'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.executeUpdate();
    }

    @Override
    public List<user> selectALL() throws SQLException {
        List<user> List = new ArrayList<>();

        String req = "SELECT * FROM `users`";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            user sr = new user(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4)

            );

          List.add(sr);
        }

        return List;
    }
}
