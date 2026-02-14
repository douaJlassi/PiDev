package projet.services;

import projet.entites.Hotel;
import projet.entites.vol;
import projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HotelService implements CRUD<Hotel> {
    private Connection connection;
    public HotelService() {connection= MyDBConnexion.getInstance().getConnection();}
    @Override
    public void insertOne(Hotel hotel) throws SQLException {
        String req = "INSERT INTO `services`(`idService`,`nom`,`description`,`prix`,`disponibilite`,`capacite`,`nombreEtoiles`,`localisation`,`typeChambre`,`type`) VALUES " +
                "('"+hotel.getId()+"','"+hotel.getNom()+"','"+hotel.getDescription()+"','"+hotel.getPrix()+"','"+hotel.getDisponibilite()+"','"+hotel.getCapacite()+"' ,  '"+hotel.getNbEtoiles()+"' , '"+hotel.getLocalisation()+"' , '"+hotel.getChambre() +"','hotel')";
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }
    @Override
    public void updateOne(Hotel hotel) throws SQLException {
        String req="UPDATE services SET idService='"+hotel.getId()+"',nom='"+hotel.getNom()+"',description='"+hotel.getDescription()+"'" +
                ",prix='"+hotel.getPrix()+"',disponibilite='"+hotel.getDisponibilite()+"',capacite='"+hotel.getCapacite()+"'" +
                ",nombreEtoiles='"+hotel.getNbEtoiles()+"',localisation='"+hotel.getLocalisation()+"',typeChambre='"+hotel.getChambre()+"' WHERE idService='"+hotel.getId()+"'";

        Statement stmt = connection.createStatement();
        stmt.executeUpdate(req);
    }

    @Override
    public void deleteOne(Hotel hotel) throws SQLException {
        String req = "DELETE FROM `services` WHERE `idService` = " + "'" + hotel.getId() + "'";
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
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getDouble(4),
                    rs.getBoolean(5),
                    rs.getInt(6),
                    rs.getInt(7),
                    rs.getString(8),
                    rs.getString(9)
            );

            hotelList.add(sr);
        }

        return hotelList;
    }
}
