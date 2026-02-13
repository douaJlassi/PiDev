package projet.services;
import projet.entites.service;
import  projet.entites.vol;
import projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VolService implements CRUD<vol>{
    private Connection connection;
    public VolService() {connection= MyDBConnexion.getInstance().getConnection();}
    @Override
    public void insertOne(vol vol) throws SQLException {
        String req = "INSERT INTO `services`(`idService`,`nom`,`description`,`prix`,`disponibilite`,`capacite`,`numeroVol`,`villeDepart`,`villeArrivee`,`dateDepart`,`dateArrive`,`type`) VALUES " +
                "('"+vol.getId()+"','"+vol.getNom()+"','"+vol.getDescription()+"','"+vol.getPrix()+"','"+vol.getDisponibilite()+"','"+vol.getCapacite()+"' ,  '"+vol.getNumeroVol()+"' , '"+vol.getVilleDepart()+"' , '"+vol.getVilleArrivee() +"' , '"+vol.getDateDepart()+"' , '"+vol.getDateArrivee()+"','vol')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
    @Override
    public void updateOne(vol vol) throws SQLException {
        String req="UPDATE services SET idService='"+vol.getId()+"',nom='"+vol.getNom()+"',description='"+vol.getDescription()+"'" +
                ",prix='"+vol.getPrix()+"',disponibilite='"+vol.getDisponibilite()+"',capacite='"+vol.getCapacite()+"'" +
                ",numeroVol='"+vol.getNumeroVol()+"',villeDepart='"+vol.getVilleDepart()+"',villeArrivee='"+vol.getVilleDepart()+"'" +
                ",dateDepart='"+vol.getDateDepart()+"',dateArrive='"+vol.getDateArrivee()+"' WHERE idService='"+vol.getId()+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(vol vol) throws SQLException {
        String req = "DELETE FROM `services` WHERE `idService` = " + "'" + vol.getId() + "'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.executeUpdate();
    }

    @Override
    public List<vol> selectALL() throws SQLException {
        List<vol> volList = new ArrayList<>();

        String req = "SELECT * FROM `services` WHERE `type` = 'vol' ";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            vol sr = new vol(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getDate(13),
                    rs.getDate(14)
                    );

            volList.add(sr);
        }

        return volList;
    }
}
