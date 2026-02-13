package projet.services;
import  projet.entites.service;
import projet.utils.MyDBConnexion;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceService implements CRUD<service> {
    private Connection connection;
    public ServiceService() {connection=MyDBConnexion.getInstance().getConnection();}
    @Override
    public void insertOne(service service) throws SQLException {
        String req = "INSERT INTO `service`(`idService`,`nom`,`description`,`prix`,`disponibilite`,`capacite`) VALUES " +
                "('"+service.getId()+"' ,'"+service.getNom()+"' ,  '"+service.getDescription()+"' , "+service.getPrix()+" , '"+service.getDisponibilite() +"' , '"+service.getCapacite()+"')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void updateOne(service service) throws SQLException {
        String req="UPDATE service SET idService='"+service.getId()+"',nom='"+service.getNom()+"',description='"+service.getDescription()+"',prix='"+service.getPrix()+"',disponibilite='"+service.getDisponibilite()+"',capacite='"+service.getCapacite()+"' WHERE idService='"+service.getId()+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
    @Override
    public void deleteOne(service service) throws SQLException {
    String req = "DELETE FROM `service` WHERE `idService` = " + "'" + service.getId() + "'";
    PreparedStatement ps = connection.prepareStatement(req);
    ps.executeUpdate();
    }

    @Override
    public List<service> selectALL() throws SQLException {
        List<service> serviceList = new ArrayList<>();

        String req = "SELECT * FROM `service`";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            service sr = new service(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6)

            );

            serviceList.add(sr);
        }

        return serviceList;
    }
}
