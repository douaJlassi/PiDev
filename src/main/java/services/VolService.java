package services;

import entities.vol;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;

import java.util.List;

public class VolService implements CRUD<String,vol> {
    private Connection connection;
    private String NbVol;

    public VolService() {
        connection = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(vol vol) throws SQLException {
        String req = "INSERT INTO `services`(`nom`,`description`,`prix`,`disponibilite`,`capacite`,`numeroVol`,`villeDepart`,`villeArrivee`,`dateDepart`,`dateArrive`,`type`,`imgUrl`) VALUES " +
                "('" + vol.getNom() + "','" + vol.getDescription() + "','" + vol.getPrix() + "','" + vol.getDisponibilite() + "','" + vol.getCapacite() + "' ,  '" + vol.getNumeroVol() + "' , '" + vol.getVilleDepart() + "' , '" + vol.getVilleArrivee() + "' , '" + vol.getDateDepart() + "' , '" + vol.getDateArrivee() + "','vol', '"+vol.getImage()+"')";
        NbVol = vol.getNumeroVol();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);

    }

    @Override
    public void updateOne(String numvol, vol vol) throws SQLException {
        String req = "UPDATE services SET nom='" + vol.getNom() + "',description='" + vol.getDescription() + "'" +
                ",prix='" + vol.getPrix() + "',disponibilite='" + vol.getDisponibilite() + "',capacite='" + vol.getCapacite() + "'" +
                ",numeroVol='" + vol.getNumeroVol() + "',villeDepart='" + vol.getVilleDepart() + "',villeArrivee='" + vol.getVilleDepart() + "'" +
                ",dateDepart='" + vol.getDateDepart() + "',dateArrive='" + vol.getDateArrivee() + "' WHERE numeroVol='" + numvol + "'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(vol vol) throws SQLException {
        String req = "DELETE FROM `services` WHERE `numeroVol` = " + "'" + vol.getNumeroVol() + "'";
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
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(10),
                    rs.getString(11),
                    rs.getString(12),
                    rs.getDate(13),
                    rs.getDate(14),
                    rs.getString(16),
                    rs.getString(15)
            );

            volList.add(sr);
        }

        return volList;
    }

    public vol selectByNom(String nom) throws SQLException {
        String req = "SELECT * FROM `services` WHERE `nom` = '" + nom + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        vol sr = null;
        while (rs.next()) {

            sr = new vol(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(10),
                    rs.getString(11),
                    rs.getString(12),
                    rs.getDate(13),
                    rs.getDate(14),
                    rs.getString(16),
                    rs.getString(15)
            );

        }
        return sr;
    }
    public vol selectById(int id) throws SQLException {
        String req = "SELECT * FROM `services` WHERE `idService` = '" + id + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        vol sr = null;
        while (rs.next()) {

            sr = new vol(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(10),
                    rs.getString(11),
                    rs.getString(12),
                    rs.getDate(13),
                    rs.getDate(14),
                    rs.getString(16),
                    rs.getString(15)
            );

        }
        return sr;
    }



}