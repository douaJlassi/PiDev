package services;

import entities.vol;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;

import java.util.List;

public class VolService implements CRUDservices<String,vol> {
    private Connection connection;
    private String NbVol;

    public VolService() {
        connection = MyDBConnexion.getInstance().getConnection();
    }

    @Override
    public void insertOne(vol vol) throws SQLException {
        String req = "INSERT INTO `services`(`nom`,`description`,`prix`,`disponibilite`,`capacite`,`numero_vol`,`ville_depart`,`ville_arrivee`,`date_depart`,`date_arrive`,`type`,`img_url`) VALUES " +
                "('" + vol.getNom() + "','" + vol.getDescription() + "','" + vol.getPrix() + "','" + vol.getDisponibilite() + "','" + vol.getCapacite() + "' ,  '" + vol.getNumeroVol() + "' , '" + vol.getVilleDepart() + "' , '" + vol.getVilleArrivee() + "' , '" + vol.getDateDepart() + "' , '" + vol.getDateArrivee() + "','vol', '"+vol.getImage()+"')";
        NbVol = vol.getNumeroVol();
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);

    }

    @Override
    public void updateOne(String numvol, vol vol) throws SQLException {
        String req = "UPDATE services SET nom='" + vol.getNom() + "',description='" + vol.getDescription() + "'" +
                ",prix='" + vol.getPrix() + "',disponibilite='" + vol.getDisponibilite() + "',capacite='" + vol.getCapacite() + "'" +
                ",numero_vol='" + vol.getNumeroVol() + "',ville_depart='" + vol.getVilleDepart() + "',ville_arrivee='" + vol.getVilleDepart() + "'" +
                ",date_depart='" + vol.getDateDepart() + "',date_arrive='" + vol.getDateArrivee() + "' WHERE numero_vol='" + numvol + "'";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(vol vol) throws SQLException {
        String req = "DELETE FROM `services` WHERE `numero_vol` = " + "'" + vol.getNumeroVol() + "'";
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
                    rs.getString(12),
                    rs.getString(13),
                    rs.getString(14),
                    rs.getDate(15),
                    rs.getDate(16),
                    rs.getString(9),
                    rs.getString(8)
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
                    rs.getString(12),
                    rs.getString(13),
                    rs.getString(14),
                    rs.getDate(15),
                    rs.getDate(16),
                    rs.getString(9),
                    rs.getString(8)
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
                    rs.getString(12),
                    rs.getString(13),
                    rs.getString(14),
                    rs.getDate(15),
                    rs.getDate(16),
                    rs.getString(9),
                    rs.getString(8)
            );

        }
        return sr;
    }



}