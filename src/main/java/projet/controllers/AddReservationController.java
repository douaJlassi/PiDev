package projet.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import projet.entites.reservation;
import projet.services.ReservationService;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

public class AddReservationController {

    @FXML
    private TextField tfModePaiement;
    @FXML
    private DatePicker dpDateReservation;
    @FXML
    private VBox errorContainer;
    @FXML
    private TextField tfNom;
    private int idService;
    public void setIdService(int id){this.idService = id;}

@FXML
void AddReservation(ActionEvent event)
{
    String nom = tfNom.getText();
    String modePaiement = tfModePaiement.getText();
    LocalDate dateReservationValue = dpDateReservation.getValue();
    java.sql.Date sqlDateArrive = java.sql.Date.valueOf(dateReservationValue);
    String statut="non acceptee";
    System.out.println(idService);
    ReservationService service = new ReservationService();
    reservation reservation = new reservation(statut,sqlDateArrive,idService,modePaiement,nom);
    try {
        service.insertOne(reservation);
        handleBack();
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Parent root = loader.load();
            tfModePaiement.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
