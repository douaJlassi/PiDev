package services;

import entities.Publication;
import entities.Client;
import utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PublicationService implements CRUD<Publication> {

    private Connection cnx;

    public PublicationService() {
        cnx = MyDBConnexion.getInstance().getConnection();
    }

    // CREATE
    @Override
    public void insertOne(Publication p) throws SQLException {

        String req = "INSERT INTO publication(content, datePublication, client_id) VALUES (?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, p.getContent());
        ps.setTimestamp(2, new Timestamp(p.getDatePublication().getTime()));
        // ps.setInt(3, p.getClient().getClientID()); // FK

        ps.executeUpdate();
    }

    // UPDATE
    @Override
    public void updateOne(Publication p) throws SQLException {

        String req = "UPDATE publication SET content=?, datePublication=?, client_id=? WHERE publicationID=?";

        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setString(1, p.getContent());
        ps.setTimestamp(2, new Timestamp(p.getDatePublication().getTime()));
       // ps.setInt(3, p.getClient().getClientID()); lezm entity Utilisateur mawjouda!
        ps.setInt(4, p.getPublicationID());

        ps.executeUpdate();
    }

    // DELETE
    @Override
    public void deleteOne(Publication p) throws SQLException {

        String req = "DELETE FROM publication WHERE publicationID=?";

        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, p.getPublicationID());

        ps.executeUpdate();
    }

    // SELECT ALL
    @Override
    public List<Publication> selectALL() throws SQLException {

        List<Publication> list = new ArrayList<>();

        String req = "SELECT * FROM publication";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {

            // minimal Client (only ID for now)
            Client c = new Client();
            // c.setClientID(rs.getInt("client_id"));

            Publication p = new Publication(
                    c,
                    rs.getInt("publicationID"),
                    rs.getString("content"),
                    new Date(rs.getTimestamp("datePublication").getTime())
            );

            list.add(p);
        }

        return list;
    }
}
