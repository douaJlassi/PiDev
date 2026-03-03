package controllers;

import co.elastic.clients.elasticsearch.security.User;
import entities.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import services.*;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ReservationsController implements Initializable {
    user connectedUser=new user("achref","souli","admin");
    /*user connectedUser;
    void setConnectedUser(Person person) {
        connectedUser.setNom(person.getUsername());
        connectedUser.setPrenom("");
        connectedUser.setType(person.getRole());
    }*/

    @FXML
    private FlowPane cardsContainer;
    @FXML
    private TextField txtSearch;
    private List<reservation> allReservations;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (connectedUser.getType().equals("user")) {
            allReservations = getDummyData();
            List<reservation> filtered= allReservations.stream()
                    .filter(r ->r.getNom().equals(connectedUser.getNom()+" "+connectedUser.getPrenom()) )
                    .toList();
            renderServices(filtered);

        }
        else {
        allReservations = getDummyData();
            renderServices(allReservations);}
    }
    public void refreshServices() {
        allReservations = getDummyData();
        renderServices(allReservations);
    }

    @FXML
    private void handleSearch() {
        String query = txtSearch.getText().toLowerCase();
        List<reservation> filtered = allReservations.stream()
                .filter(s -> s.getNom().toLowerCase().contains(query)
                )
                .toList();
        renderServices(filtered);
    }

    private void renderServices(List<reservation> reservations) {
        cardsContainer.getChildren().clear();

        for (reservation r : reservations) {
            VBox card = createServiceCard(r);
            cardsContainer.getChildren().add(card);
        }
    }

    private VBox createServiceCard(reservation r) {
        ReservationService Service = new ReservationService();

        VBox card = new VBox();
        card.getStyleClass().add("service-card");
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setSpacing(10);

        VBox details = new VBox();
        details.setPadding(new Insets(10));
        details.setSpacing(5);

        Label statut = new Label("Status: " + r.getStatut());
        statut.getStyleClass().add("card-type");

        Label paiement = new Label("payment method: " + r.getModePaiement());
        paiement.getStyleClass().add("card-type");

        Label date = new Label("Reservation Date: " + r.getDateReservation().toString());
        date.getStyleClass().add("card-type");

        Label name = new Label(r.getNom());
        name.getStyleClass().add("card-title-text");
        name.setWrapText(true);

        ServiceService serv = new ServiceService();
        Label serviceNameLabel;
        try {
            String serviceName = serv.getServiceName(r.getIdService());
            serviceNameLabel = new Label("Service: " + serviceName);
            serviceNameLabel.getStyleClass().add("card-type");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


        HBox actions = new HBox();
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setSpacing(10);
        Button btnDelete = new Button("🗑");
        Button btnAccept = new Button("✅");
        Button btnRefuse = new Button("❌");
        Button btnExportPdf = new Button("📄");
        btnExportPdf.setTooltip(new javafx.scene.control.Tooltip("Export PDF"));
        btnExportPdf.getStyleClass().addAll("btn-card-action");
        btnExportPdf.setVisible(r.getStatut().equals("acceptee"));
        btnExportPdf.setManaged(r.getStatut().equals("acceptee"));
        btnAccept.setOnAction(event -> {
            try {
                int id = Service.getIdReservation(r);
                Service.validerReservation(id);
                refreshServices();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        btnRefuse.setOnAction(event -> {
            try {
                int id = Service.getIdReservation(r);
                Service.RefuserReservation(id);
                refreshServices();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        btnDelete.setOnAction(event -> {
            try {
                int id = Service.getIdReservation(r);
                Service.deletebyId(id);
                //Service.deleteOne(r);
                ServiceService service = new ServiceService();
                service.IncrementCapacite(r.getIdService());
                refreshServices();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        btnExportPdf.setOnAction(event -> {
            try {
                ServiceService serviceService = new ServiceService();
                String serviceName = serviceService.getServiceName(r.getIdService());
                service s;
                s = serviceService.selectByNom(serviceName);
                String type = serviceService.selectByNom(serviceName).getType();
                System.out.println(type);
                PdfExportService.exportReservation(r, s, type);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "PDF exported successfully!", ButtonType.OK);
                alert.show();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export PDF: " + e.getMessage(), ButtonType.OK);
                alert.show();
            }
        });
        if (connectedUser.getType().equals("user")) {
            btnAccept.setVisible(false);
            btnRefuse.setVisible(false);
        }
        btnDelete.getStyleClass().addAll("btn-card-action", "btn-card-delete");
        btnAccept.getStyleClass().addAll("btn-card-action");
        btnRefuse.getStyleClass().addAll("btn-card-action", "btn-card-delete");
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actions.getChildren().addAll(spacer, btnAccept, btnRefuse, btnDelete, btnExportPdf);

        details.getChildren().addAll(name, serviceNameLabel, statut, paiement, date, actions);
        card.getChildren().addAll(details);

        return card;
    }
    private List<reservation> getDummyData() {
        ReservationService Service = new ReservationService();
        List<reservation> list = new ArrayList<>();
        try {
            list=Service.selectALL();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}