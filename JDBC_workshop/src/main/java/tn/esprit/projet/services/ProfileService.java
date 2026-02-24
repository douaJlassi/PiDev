package tn.esprit.projet.services;

import tn.esprit.projet.entities.Profile;
import tn.esprit.projet.utils.MyDBConnexion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProfileService implements CRUD<Profile> {

    private Connection cnx;

    public ProfileService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    @Override
    public void insertOne(Profile profile) throws SQLException {
        String req = "INSERT INTO profile (image, member_premium, language, id_user, coins) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);

        ps.setBytes(1, profile.getImage());
        ps.setString(2, profile.getMemberPremium());
        ps.setString(3, profile.getLanguage());
        ps.setInt(4, profile.getIdUser());
        ps.setInt(5, profile.getCoins());

        ps.executeUpdate();

        // Get generated ID
        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            profile.setId(rs.getInt(1));
        }
    }

    @Override
    public void updateOne(Profile profile) throws SQLException {
        String req = "UPDATE profile SET image=?, member_premium=?, language=?, coins=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);

        ps.setBytes(1, profile.getImage());
        ps.setString(2, profile.getMemberPremium());
        ps.setString(3, profile.getLanguage());
        ps.setInt(4, profile.getCoins());
        ps.setInt(5, profile.getId());

        System.out.println("Executing update for profile ID: " + profile.getId());
        System.out.println("  Setting member_premium='" + profile.getMemberPremium() + "'");
        System.out.println("  Setting language='" + profile.getLanguage() + "'");
        System.out.println("  Setting coins=" + profile.getCoins());
        System.out.println("  Image present: " + (profile.getImage() != null));

        int rowsAffected = ps.executeUpdate();
        System.out.println("Rows affected: " + rowsAffected);

        if (rowsAffected == 0) {
            System.err.println("WARNING: No rows were updated for profile ID: " + profile.getId());
        }
    }

    @Override
    public void deleteOne(Profile profile) throws SQLException {
        String req = "DELETE FROM profile WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, profile.getId());
        ps.executeUpdate();
    }

    @Override
    public List<Profile> selectALL() throws SQLException {
        List<Profile> profileList = new ArrayList<>();
        String req = "SELECT * FROM profile";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Profile p = new Profile(
                    rs.getInt("id"),
                    rs.getBytes("image"),
                    rs.getString("member_premium"),
                    rs.getString("language"),
                    rs.getInt("id_user"),
                    rs.getInt("coins")
            );
            profileList.add(p);
        }
        return profileList;
    }

    public Profile getProfileByUserId(int userId) throws SQLException {
        String req = "SELECT * FROM profile WHERE id_user=?";
        PreparedStatement ps = cnx.prepareStatement(req);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return new Profile(
                    rs.getInt("id"),
                    rs.getBytes("image"),
                    rs.getString("member_premium"),
                    rs.getString("language"),
                    rs.getInt("id_user"),
                    rs.getInt("coins")
            );
        }
        return null;
    }

    public void add(Profile profile) throws SQLException {
        insertOne(profile);
    }

}