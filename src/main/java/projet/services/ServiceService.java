package projet.services;
import  projet.entites.service;
import projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements CRUD<String,service> {
    private Connection connection;
    private String nom;
    public ServiceService() {connection=MyDBConnexion.getInstance().getConnection();}
    @Override
    public void insertOne(service service) throws SQLException {
        String req = "INSERT INTO `services`(`nom`,`description`,`prix`,`disponibilite`,`capacite`) VALUES " +
                "('"+service.getNom()+"' ,  '"+service.getDescription()+"' , "+service.getPrix()+" , '"+service.getDisponibilite() +"' , '"+service.getCapacite()+"')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
        nom=service.getNom();
    }

    @Override
    public void updateOne(String nomservice,service serviceUpdated) throws SQLException {
        String req="UPDATE services SET nom='"+serviceUpdated.getNom()+"',description='"+serviceUpdated.getDescription()+"',prix='"+serviceUpdated.getPrix()+"',disponibilite='"+serviceUpdated.getDisponibilite()+"',capacite='"+serviceUpdated.getCapacite()+"' WHERE nom='"+nomservice+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
    @Override
    public void deleteOne(service service) throws SQLException {
        String req = "DELETE FROM `services` WHERE `nom` = " + "'" + service.getNom() + "'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.executeUpdate();
    }

    @Override
    public List<service> selectALL() throws SQLException {
        List<service> serviceList = new ArrayList<>();

        String req = "SELECT * FROM `services`";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            service sr = new service(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(15)

            );

            serviceList.add(sr);
        }

        return serviceList;
    }
}