package controllers;

import app.Session;
import entities.Actualite;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import repositories.ActualiteRepository;
import repositories.IActualiteRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AgencyActualitesController {

    @FXML private TableView<Actualite> table;

    @FXML private TableColumn<Actualite, Actualite> colPreview;
    @FXML private TableColumn<Actualite, String> colTitle;
    @FXML private TableColumn<Actualite, Number> colClicks;
    @FXML private TableColumn<Actualite, Actualite> colEndsAt;
    @FXML private TableColumn<Actualite, Actualite> colStatus;
    @FXML private TableColumn<Actualite, Actualite> colActions;

    private final IActualiteRepository repo = new ActualiteRepository();

    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        if (!Session.isAgency()) {
            show("Access denied", "This page is for agencies only.");
            return;
        }

        setupColumns();
        load();
    }

    @FXML
    private void onRefresh() {
        load();
    }

    private void setupColumns() {

        // Preview image column
        colPreview.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colPreview.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(150);
                iv.setFitHeight(55);
                iv.setPreserveRatio(false);
                iv.setSmooth(true);
            }
            @Override
            protected void updateItem(Actualite a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) {
                    setGraphic(null);
                    return;
                }
                try {
                    iv.setImage(new Image(a.getBannerUrl(), true));
                } catch (Exception e) {
                    iv.setImage(null);
                }
                setGraphic(iv);
            }
        });

        colTitle.setCellValueFactory(param ->
                new javafx.beans.property.SimpleStringProperty(param.getValue().getTitre()));

        colClicks.setCellValueFactory(param ->
                new javafx.beans.property.SimpleIntegerProperty(param.getValue().getClickCount()));

        // EndsAt formatted
        colEndsAt.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colEndsAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Actualite a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); return; }

                if (a.getEndsAt() == null) setText("∞ (no expiry)");
                else setText(fmt.format(a.getEndsAt()));
            }
        });

        // Status column
        colStatus.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Actualite a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setText(null); return; }

                boolean expired = a.getEndsAt() != null && a.getEndsAt().isBefore(LocalDateTime.now());
                boolean live = a.isActive() && !expired;

                setText(live ? "LIVE" : (expired ? "EXPIRED" : "ARCHIVED"));
            }
        });

        // Actions column (Archive + Extend)
        colActions.setCellValueFactory(param -> new javafx.beans.property.SimpleObjectProperty<>(param.getValue()));
        colActions.setCellFactory(col -> new TableCell<>() {

            //private final Button archiveBtn = new Button("Archive");
            private final Button extendBtn  = new Button("+7 days");

            private final Button deleteBtn = new Button(); // icon only
            private final HBox box = new HBox(10, extendBtn, deleteBtn);

            {
                //archiveBtn.getStyleClass().add("btn-action-danger");
                extendBtn.getStyleClass().add("btn-ghost");

                // 🔥 Trash icon
                javafx.scene.shape.SVGPath trash = new javafx.scene.shape.SVGPath();
                trash.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                trash.setFill(javafx.scene.paint.Color.web("#EF4444"));
                trash.setScaleX(0.85);
                trash.setScaleY(0.85);

                deleteBtn.setGraphic(trash);
                deleteBtn.setTooltip(new Tooltip("Delete permanently"));
                deleteBtn.getStyleClass().add("btn-icon-only");
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 8; -fx-background-radius: 10;");

                /*archiveBtn.setOnAction(e -> {
                    Actualite a = getTableView().getItems().get(getIndex());
                    boolean ok = repo.archive(a.getIdActualite(), Session.getUserId());
                    show("Banner", ok ? "Archived ✅" : "Archive failed.");
                    load();
                });*/

                extendBtn.setOnAction(e -> {
                    Actualite a = getTableView().getItems().get(getIndex());
                    boolean ok = repo.extendEndsAtPlus7Days(a.getIdActualite(), Session.getUserId());
                    show("Banner", ok ? "Extended ✅" : "Extend failed.");
                    load();
                });

                deleteBtn.setOnAction(e -> {
                    Actualite a = getTableView().getItems().get(getIndex());

                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete banner");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Delete this banner permanently?");

                    if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

                    boolean ok = repo.delete(a.getIdActualite(), Session.getUserId());
                    show("Banner", ok ? "Deleted ✅" : "Delete failed.");
                    load();
                });
            }

            @Override
            protected void updateItem(Actualite a, boolean empty) {
                super.updateItem(a, empty);
                setGraphic(empty || a == null ? null : box);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
    }

    private void load() {
        var data = repo.findByAgency(Session.getUserId());
        table.setItems(FXCollections.observableArrayList(data));
    }

    private void show(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}