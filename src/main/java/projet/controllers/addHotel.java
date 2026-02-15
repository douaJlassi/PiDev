package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import projet.entites.Hotel;
import projet.services.HotelService;

import java.sql.SQLException;

public class addHotel {

    @FXML
    private CheckBox cbDisponibilite;

    @FXML
    private TextField tfCapacite;

    @FXML
    private TextField tfChambre;

    @FXML
    private TextField tfDescription;

    @FXML
    private TextField tfLocalisation;

    @FXML
    private TextField tfNbEtoiles;

    @FXML
    private TextField tfNom;

    @FXML
    private TextField tfPrix;
@FXML
    public void addHotel(ActionEvent event) throws SQLException {
    String nom=tfNom.getText();
    String description=tfDescription.getText();
    String localisation=tfLocalisation.getText();
    int nbEtoiles=Integer.parseInt(tfNbEtoiles.getText());
    int capacite=Integer.parseInt(tfCapacite.getText());
    double prix=Double.parseDouble(tfPrix.getText());
    String chambre=tfChambre.getText();
    boolean disponibilite=cbDisponibilite.isSelected();
    HotelService hs = new HotelService();
    Hotel hotel=new Hotel(nom,description,prix,disponibilite,capacite,nbEtoiles,localisation,chambre);
    try {
        hs.insertOne(hotel);
    }catch (SQLException e){
        System.out.println(e.getMessage());
    }
}
}
