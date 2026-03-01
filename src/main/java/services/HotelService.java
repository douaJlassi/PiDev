package services;

import entites.Hotel;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelService implements CRUD<String,Hotel> {
    private Connection connection;
    private String nom;
    public HotelService() {connection= MyDBConnexion.getInstance().getConnection();}
    @Override
    public void insertOne(Hotel hotel) throws SQLException {
        String req = "INSERT INTO `services`(`nom`,`description`,`prix`,`disponibilite`,`capacite`,`nombreEtoiles`,`localisation`,`typeChambre`,`type`,`imgUrl`) VALUES " +
                "('"+hotel.getNom()+"','"+hotel.getDescription()+"','"+hotel.getPrix()+"','"+hotel.getDisponibilite()+"','"+hotel.getCapacite()+"' ,  '"+hotel.getNbEtoiles()+"' , '"+hotel.getLocalisation()+"' , '"+hotel.getChambre() +"','hotel' , '"+hotel.getImage()+"')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
        nom=hotel.getNom();
    }
    @Override
    public void updateOne(String hotelName,Hotel hotel) throws SQLException {
        String req="UPDATE services SET nom='"+hotel.getNom()+"',description='"+hotel.getDescription()+"'" +
                ",prix='"+hotel.getPrix()+"',disponibilite='"+hotel.getDisponibilite()+"',capacite='"+hotel.getCapacite()+"'" +
                ",nombreEtoiles='"+hotel.getNbEtoiles()+"',localisation='"+hotel.getLocalisation()+"',typeChambre='"+hotel.getChambre()+"' WHERE nom='"+hotelName+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(Hotel hotel) throws SQLException {
        String req = "DELETE FROM `services` WHERE `nom` = " + "'" + hotel.getNom() + "'";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.executeUpdate();
    }

    @Override
    public List<Hotel> selectALL() throws SQLException {
        List<Hotel> hotelList = new ArrayList<>();

        String req = "SELECT * FROM `services` WHERE `type`='hotel' ";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            Hotel sr = new Hotel(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(15),
                    rs.getInt(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getString(15)
            );

            hotelList.add(sr);
        }

        return hotelList;
    }

    public Hotel selectOne(String hotelName) throws SQLException {
        String req = "SELECT * FROM `services` WHERE `nom` = " + "'" + hotelName + "'";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);

        Hotel sr = null;
        while (rs.next()) {

            sr = new Hotel(
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getString(15),
                    rs.getInt(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getString(15)
            );

        }
        return sr;
    }

    public int NbHotels() throws SQLException {
        List<Hotel> hotelList = new ArrayList<>();

        String req = "SELECT COUNT(*) FROM `services` WHERE `type`='hotel' ";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(req);
        return rs.getInt(1);
    }


}

