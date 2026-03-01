package projet.services;
import  projet.entites.service;
import projet.entites.vol;
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
        String req = "INSERT INTO `services`(`nom`,`description`,`prix`,`disponibilite`,`capacite`,`imgUrl`) VALUES " +
                "('"+service.getNom()+"' ,  '"+service.getDescription()+"' , "+service.getPrix()+" , '"+service.getDisponibilite() +"' , '"+service.getCapacite()+"' , '"+service.getImage()+"')";
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
                    rs.getString(16),
                    rs.getString(15)

            );

            serviceList.add(sr);
        }

        return serviceList;
    }
    public service selectByNom(String nom) throws SQLException {
        String req = "SELECT * FROM `services` WHERE `nom` = '" + nom + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        service sr = null;
        while (rs.next()) {

            sr = new service(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    "none",
                    rs.getString(15)
            );

        }
        return sr;
    }
    public int getId(String nom) throws SQLException {
        String req = "SELECT idService FROM `services` WHERE `nom` = '" + nom + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);
        int id = 0;
        while (rs.next()) {
            id = rs.getInt(1);
        }
        return id;
    }
    public service selectById(int id) throws SQLException {
        String req = "SELECT * FROM `services` WHERE `id` = '" + id + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        service sr = null;
        while (rs.next()) {

            sr = new service(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    "none",
                    rs.getString(15)
            );

        }
        return sr;
    }
    public String getServiceName(int id) throws SQLException {
        String req = "SELECT nom FROM `services` WHERE `id` = '" + id + "'";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        return rs.getString(1);
    }
    public void DecrementCapacite(int id) throws SQLException {
        String req = "UPDATE services SET capacite = capacite - 1, " +
                "disponibilite = CASE " +
                "WHEN capacite - 1 <= 0 THEN 'false' " +
                "WHEN disponibilite = 'false' THEN 'false' " +
                "ELSE 'true' END " +
                "WHERE idService = "+id+" AND capacite > 0";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
    public void IncrementCapacite(int id) throws SQLException {
        String req = "UPDATE services SET capacite=capacite+1,disponibilite = CASE WHEN capacite+1 > 0 THEN 'true'  ELSE 'false'" +
                " END WHERE `idService` = " + "'" + id+ "'";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
}