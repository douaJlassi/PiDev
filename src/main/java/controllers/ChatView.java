package controllers;

import entities.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import services.ServiceConversation;
import services.ServiceMessage;
import services.ServiceParticipantConversation;
import services.ServiceUtilisateur;
import utils.AudioRecorder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatView {

    @FXML
    private ListView<Conversation> listConversations;
    @FXML
    private VBox vboxMessages;
    @FXML
    private Label lblNomContact;
    @FXML
    private TextField inputMessage;
    @FXML
    private TextField searchField;
    @FXML
    private VBox chatArea;
    @FXML
    private VBox paneDefault;
    @FXML
    private ListView<Utilisateur> listAllUsers;
    @FXML
    private ToggleButton btnAll;
    @FXML
    private ToggleGroup categoryGroup;
    @FXML
    private ToggleButton btnGroups;
    @FXML
    private MenuButton btnOptions;
    @FXML
    private Circle circleStatus;
    @FXML
    private HBox inputArea;
    @FXML
    private ScrollPane scrollPaneMessages;
    @FXML
    private HBox paneQuitte;
    private ObservableList<Conversation> masterData = FXCollections.observableArrayList();
    private FilteredList<Conversation> filteredData;

    @FXML private TextField userSearchField;
    private ObservableList<Utilisateur> masterUserList = FXCollections.observableArrayList();
    private FilteredList<Utilisateur> filteredUserList;

    private ServiceConversation serConv = new ServiceConversation();
    private ServiceMessage serMsg = new ServiceMessage();
    private final ServiceUtilisateur serUser = new ServiceUtilisateur();
    private final ServiceParticipantConversation spc = new ServiceParticipantConversation();
    private int currentUserId = 12;
    private Utilisateur userConnecte;
    private boolean isGroupMode = false;
    private Set<Utilisateur> selectedUsers = new HashSet<>();
    @FXML
    private Button btnConfirmGroup;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    private static final String UPLOAD_DIR_VOICE = System.getProperty("user.dir") + File.separator + "uploadVoice" + File.separator;

    @FXML
    private Button btnAddFile;

    @FXML
    private VBox paneMediaHistory, vboxMediaList;
    @FXML private VBox paneScrollDown;
    @FXML private Label lblNewMsgBadge;
    private int newMessagesCount = 0;

    private AudioRecorder recorder = new AudioRecorder();
    private File currentAudioFile;

    @FXML
    private Button btnMic;

    @FXML
    private Button btnLocation;
    @FXML private Text profileInitials;

    @FXML
    public void initialize() {
        try {
            this.userConnecte = serUser.selectOne(currentUserId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        loadConversations();
        filteredData = new FilteredList<>(masterData, p -> true);
        listConversations.setItems(filteredData);
        searchField.textProperty().addListener((obs, oldV, newV) -> updateFilter());

        String styleNormal =
                "-fx-background-color: white;" +
                        "-fx-border-color: #E0E0E0;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 20;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 500;" +
                        "-fx-text-fill: #444;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 6, 0, 0, 2);" +
                        "-fx-min-width: 120;" +
                        "-fx-alignment: center;" +
                        "-fx-cursor: hand;";

        String styleSelected =
                "-fx-background-color: linear-gradient(to right, #00B4D8, #0077B6);" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 20;" +
                        "-fx-background-radius: 20;" +
                        "-fx-padding: 8 20;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: 600;" +
                        "-fx-text-fill: white;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 10, 0, 0, 3);" +
                        "-fx-cursor: hand;"+
                        "-fx-min-width: 120;" +
                        "-fx-alignment: center;";
        btnAll.setStyle(styleSelected);
        btnGroups.setStyle(styleNormal);
        categoryGroup.selectedToggleProperty().addListener((obs, oldV, newVal) -> {
            if (newVal == btnAll || newVal == null) {
                btnAll.setStyle(styleSelected);
                btnGroups.setStyle(styleNormal);
            } else {
                btnGroups.setStyle(styleSelected);
                btnAll.setStyle(styleNormal);
            }
            updateFilter();
        });
        listConversationStyle();
        listConversations.setStyle("-fx-background-color: transparent; -fx-selection-bar: #E0F7FA;");

        listConversations.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newVal) -> {
            if (newVal != null) {
                try {
                    serMsg.marquerCommeLu(newVal.getIdConversation(), currentUserId);
                    listConversations.refresh();
                    if (MainLayout.getInstance() != null) {
                        MainLayout.getInstance().refreshBadge();
                    }
                    paneDefault.setVisible(false);
                    paneDefault.setManaged(false);
                    chatArea.setVisible(true);
                    chatArea.setManaged(true);

                    lblNomContact.setText(serConv.getNomAffichage(newVal, currentUserId));
                    chargerHistorique(newVal.getIdConversation());
                    afficherDiscussion(newVal);

                } catch (SQLException e) { e.printStackTrace(); }
            }
        });

        scrollPaneMessages.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() < 0.9) {
                paneScrollDown.setVisible(true);
                paneScrollDown.setManaged(true);
            } else {
                paneScrollDown.setVisible(false);
                paneScrollDown.setManaged(false);
                newMessagesCount = 0;
                lblNewMsgBadge.setVisible(false);
            }
        });
    }

    private void loadConversations() {
        try {
            masterData.setAll(serConv.selectByUser(currentUserId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @FXML private StackPane headerAvatarPane;
    private void afficherDiscussion(Conversation conv) {
        vboxMessages.getChildren().clear();

        try {
            String nomAafficher = serConv.getNomAffichage(conv, currentUserId);
            lblNomContact.setText(nomAafficher);
            String initials = getInitials(nomAafficher);

            headerAvatarPane.getChildren().clear();

            if (conv.getTypeConversation() == TypeConversation.GROUPE) {
                Circle cBack = new Circle(15, Color.web("#bdc3c7"));
                cBack.setStroke(Color.WHITE);
                cBack.setStrokeWidth(2);

                Circle cFront = new Circle(15, Color.web("#10A5A5"));
                cFront.setStroke(Color.WHITE);
                cFront.setStrokeWidth(2);

                Text tGroup = new Text(initials.substring(0, 1).toUpperCase());
                tGroup.setFill(Color.WHITE);
                tGroup.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");

                StackPane frontGroup = new StackPane(cFront, tGroup);
                headerAvatarPane.getChildren().addAll(cBack, frontGroup);

                StackPane.setAlignment(cBack, Pos.TOP_RIGHT);
                StackPane.setAlignment(frontGroup, Pos.BOTTOM_LEFT);

                circleStatus.setVisible(false);
                circleStatus.setManaged(false);
            } else {
                Circle circle = new Circle(21, Color.web("#10A5A5"));
                Text txt = new Text(initials);
                txt.setFill(Color.WHITE);
                txt.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                headerAvatarPane.getChildren().addAll(circle, txt, circleStatus);

                circleStatus.setVisible(true);
                circleStatus.setManaged(true);
                circleStatus.setOpacity(1);
                circleStatus.setTranslateX(14);
                circleStatus.setTranslateY(14);
            }

            mettreAJourStatut(conv);
            configurerMenuOptions(conv);

            List<Messages> messages = serMsg.selectByConversation(conv.getIdConversation());
            for (Messages m : messages) {
                boolean isMoi = (m.getExpediteur().getIdUtilisateur() == currentUserId);
                renderMessage(m, isMoi);
            }

            Platform.runLater(() -> scrollPaneMessages.setVvalue(1.0));

            boolean active = spc.isUserActiveInConversation(currentUserId, conv.getIdConversation());
            if (active) {
                inputArea.setVisible(true);
                inputArea.setManaged(true);
                paneQuitte.setVisible(false);
                paneQuitte.setManaged(false);
            } else {
                inputArea.setVisible(false);
                inputArea.setManaged(false);
                paneQuitte.setVisible(true);
                paneQuitte.setManaged(true);
            }

            handleCloseMedia();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleNewChat() {
        isGroupMode = false;
        btnConfirmGroup.setVisible(false);
        btnConfirmGroup.setManaged(false);
        prepareUserSelectionList();
    }

    public void handleNewGroup() {
        isGroupMode = true;
        selectedUsers.clear();
        btnConfirmGroup.setVisible(true);
        btnConfirmGroup.setManaged(true);
        prepareUserSelectionList();
    }

    private void prepareUserSelectionList() {
        chatArea.setVisible(false);
        chatArea.setManaged(false);
        paneDefault.setVisible(true);
        paneDefault.setManaged(true);

        userSearchField.setVisible(true);
        userSearchField.setManaged(true);
        listAllUsers.setVisible(true);
        listAllUsers.setManaged(true);

        try {
            List<Utilisateur> allUsers = serUser.selectALL();
            allUsers.removeIf(u -> u.getIdUtilisateur() == currentUserId);

            masterUserList.setAll(allUsers);
            filteredUserList = new FilteredList<>(masterUserList, p -> true);
            listAllUsers.setItems(filteredUserList);

            userSearchField.textProperty().addListener((obs, oldV, newV) -> {
                filteredUserList.setPredicate(user -> {
                    if (newV == null || newV.isEmpty()) return true;
                    String filter = newV.toLowerCase();
                    return user.getNom().toLowerCase().contains(filter) ||
                            user.getPrenom().toLowerCase().contains(filter) ||
                            user.getEmail().toLowerCase().contains(filter);
                });
            });

            listAllUsers.setCellFactory(lv -> new ListCell<Utilisateur>() {
                @Override
                protected void updateItem(Utilisateur user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setGraphic(null);
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        HBox container = new HBox(15);
                        container.setAlignment(Pos.CENTER_LEFT);
                        container.setPadding(new Insets(10, 15, 10, 15));
                        container.setCursor(Cursor.HAND);

                        String styleBase = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;";
                        container.setStyle(styleBase);

                        StackPane avatarPane = new StackPane();
                        Circle circle = new Circle(18, Color.web("#10A5A5"));

                        String initialesText = (user.getPrenom().substring(0, 1) + user.getNom().substring(0, 1)).toUpperCase();
                        javafx.scene.text.Text txtInitiales = new javafx.scene.text.Text(initialesText);
                        txtInitiales.setFill(Color.WHITE);
                        txtInitiales.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

                        avatarPane.getChildren().addAll(circle, txtInitiales);

                        VBox infoBox = new VBox(2);
                        Label lblName = new Label(user.getPrenom() + " " + user.getNom());
                        lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-weight: bold; -fx-font-size: 14px;");

                        Label lblEmail = new Label(user.getEmail());
                        lblEmail.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

                        infoBox.getChildren().addAll(lblName, lblEmail);
                        HBox.setHgrow(infoBox, Priority.ALWAYS);

                        if (isGroupMode) {
                            CheckBox cb = new CheckBox();
                            cb.setStyle("-fx-accent: #10A5A5;"); // Checkbox turquoise
                            cb.setSelected(selectedUsers.contains(user));

                            cb.setOnAction(e -> {
                                if (cb.isSelected()) selectedUsers.add(user);
                                else selectedUsers.remove(user);
                            });

                            container.getChildren().addAll(cb, avatarPane, infoBox);
                        } else {
                            container.getChildren().addAll(avatarPane, infoBox);
                        }

                        container.setOnMouseEntered(e -> container.setStyle("-fx-background-color: #f4fbfc; -fx-background-radius: 10; -fx-border-color: #10A5A5; -fx-border-width: 0 0 1 0;"));
                        container.setOnMouseExited(e -> container.setStyle(styleBase));

                        setGraphic(container);
                    }
                }
            });

            listAllUsers.setOnMouseClicked(event -> {
                Utilisateur selection = listAllUsers.getSelectionModel().getSelectedItem();
                if (selection != null && !isGroupMode) {
                    creerOuOuvrirDiscussion(selection);
                }
            });

        } catch (SQLException e) {
            System.err.println("Erreur chargement utilisateurs : " + e.getMessage());
        }
    }
    public void handleConfirmGroupSelection() throws SQLException {
        if (selectedUsers.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Action requise");
            alert.setHeaderText("Aucun membre sélectionné");
            alert.setContentText("Veuillez choisir au moins un membre pour créer un groupe.");
            styliserBoiteDialogue(alert.getDialogPane(), "#e67e22");
            alert.showAndWait();
            return;
        }

        Set<Integer> targetMemberIds = selectedUsers.stream()
                .map(Utilisateur::getIdUtilisateur)
                .collect(Collectors.toSet());
        targetMemberIds.add(currentUserId);

        Integer existingGroupId = spc.findExistingGroupWithMembers(targetMemberIds);

        if (existingGroupId != null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Discussion existante");
            alert.setContentText("Un groupe avec ces membres existe déjà. Redirection en cours...");
            styliserBoiteDialogue(alert.getDialogPane(), "#10A5A5");
            alert.showAndWait();

            Conversation existingConv = serConv.selectOne(existingGroupId);

            loadConversations();
            afficherDiscussion(existingConv);
            listConversations.getSelectionModel().select(existingConv);
            resetInterfaceRecherche();
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Nouveau Groupe");
        dialog.setTitle("Rehletna - Création de groupe");
        dialog.setHeaderText("Donnez un titre à votre discussion de groupe");
        dialog.setContentText("Nom du groupe :");
        styliserBoiteDialogue(dialog.getDialogPane(), "#10A5A5");

        dialog.showAndWait().ifPresent(groupTitle -> {
            if (groupTitle.trim().isEmpty()) return;
            try {
                Conversation group = new Conversation();
                group.setTypeConversation(TypeConversation.GROUPE);
                group.setTitre(groupTitle.trim());
                group.setDateCreation(LocalDateTime.now());
                serConv.insertOne(group);
                spc.insertOne(new ParticipantConversation(serUser.selectOne(currentUserId), group, LocalDateTime.now()));
                for (Utilisateur member : selectedUsers) {
                    spc.insertOne(new ParticipantConversation(member, group, LocalDateTime.now()));
                }

                resetInterfaceRecherche();
                loadConversations();
                afficherDiscussion(group);
                listConversations.getSelectionModel().select(group);

            } catch (SQLException e) {
                e.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR, "Erreur lors de la création.");
                styliserBoiteDialogue(error.getDialogPane(), "#e74c3c");
                error.show();
            }
        });
    }

    private void resetInterfaceRecherche() {
        userSearchField.clear();
        userSearchField.setVisible(false);
        userSearchField.setManaged(false);
        btnConfirmGroup.setVisible(false);
        btnConfirmGroup.setManaged(false);
        listAllUsers.setVisible(false);
        listAllUsers.setManaged(false);
        selectedUsers.clear();
    }
    private void styliserBoiteDialogue(DialogPane dp, String primaryColor) {
        dp.setStyle("-fx-background-color: white; " +
                "-fx-border-color: " + primaryColor + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10;");
        Platform.runLater(() -> {
            Node headerPanel = dp.lookup(".header-panel");
            if (headerPanel != null) {
                headerPanel.setStyle("-fx-background-color: #0D3B66; -fx-background-radius: 10 10 0 0;");
                Label headerLabel = (Label) dp.lookup(".header-panel > .label");
                if (headerLabel != null) headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            }
        });
        Button okBtn = (Button) dp.lookupButton(ButtonType.OK);
        if (okBtn != null) {
            okBtn.setStyle("-fx-background-color: #10A5A5; -fx-text-fill: white; -fx-background-radius: 20; -fx-cursor: hand;");
        }

        Button cancelBtn = (Button) dp.lookupButton(ButtonType.CANCEL);
        if (cancelBtn != null) {
            cancelBtn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d; -fx-background-radius: 20; -fx-cursor: hand;");
        }
    }

    private void creerOuOuvrirDiscussion(Utilisateur destinataire) {
        try {
            Conversation existing = serConv.findPrivateChat(currentUserId, destinataire.getIdUtilisateur());

            if (existing != null) {
                loadConversations();
                afficherDiscussion(existing);
                listConversations.getSelectionModel().select(existing);

                userSearchField.clear();
                userSearchField.setVisible(false);
                userSearchField.setManaged(false);
                listAllUsers.setVisible(false);
                listAllUsers.setManaged(false);
                return;
            }else{
                Conversation newConv = new Conversation();
                newConv.setTypeConversation(TypeConversation.PRIVEE);
                newConv.setDateCreation(LocalDateTime.now());
                serConv.insertOne(newConv);

                ServiceParticipantConversation spc = new ServiceParticipantConversation();
                spc.insertOne(new ParticipantConversation(serUser.selectOne(currentUserId), newConv, LocalDateTime.now()));
                spc.insertOne(new ParticipantConversation(destinataire, newConv, LocalDateTime.now()));
                userSearchField.clear();
                userSearchField.setVisible(false);
                userSearchField.setManaged(false);

                listAllUsers.setVisible(false);
                listAllUsers.setManaged(false);
                loadConversations();
                afficherDiscussion(newConv);

                listConversations.getSelectionModel().select(newConv);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFilterChange() {
        updateFilter();
    }

    private void updateFilter() {
        String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        boolean filterByGroupOnly = btnGroups.isSelected();
        filteredData.setPredicate(conversation -> {
            if (filterByGroupOnly && conversation.getTypeConversation() != TypeConversation.GROUPE) {
                return false;
            }
            if (searchText.isEmpty()) {
                return true;
            }
            String nameToSearch = serConv.getNomAffichage(conversation, currentUserId).toLowerCase();
            return nameToSearch.contains(searchText);
        });
    }
    private void renderMessage(Messages msg, boolean isMoi) {
        HBox lineContainer = new HBox(10);
        lineContainer.setPadding(new Insets(8, 15, 8, 15));

        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(35, 35);
        avatarContainer.setMaxSize(35, 35);

        String senderName = msg.getExpediteur().getPrenom() + " " + msg.getExpediteur().getNom();
        String initials = getInitials(senderName);

        Circle circle = new Circle(17.5, Color.web("#10A5A5"));
        Text txt = new Text(initials);
        txt.setFill(Color.WHITE);
        txt.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        avatarContainer.getChildren().addAll(circle, txt);

        if (msg.isDeleted()) {
            HBox bubbleDeleted = new HBox(8);
            bubbleDeleted.setPadding(new Insets(10, 14, 10, 14));
            bubbleDeleted.setAlignment(Pos.CENTER_LEFT);
            bubbleDeleted.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.25); " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: rgba(150,150,150,0.3); " +
                            "-fx-border-style: dashed; " +
                            "-fx-border-radius: 12;"
            );
            Label icon = new Label("🚫");
            icon.setStyle("-fx-font-size: 13;");
            Label lbl = new Label("Ce message a été supprimé");
            lbl.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic; -fx-font-size: 12px;");
            bubbleDeleted.getChildren().addAll(icon, lbl);

            StackPane stack = new StackPane(bubbleDeleted);
            lineContainer.setAlignment(isMoi ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            lineContainer.getChildren().addAll(isMoi ? stack : avatarContainer, isMoi ? avatarContainer : stack);
            vboxMessages.getChildren().add(lineContainer);
            return;
        }

        Node visualContent;

        if (msg.getTypeMessage() == TypeMessage.IMAGE && msg.getUrlFichier() != null) {
            try {
                File file = new File(UPLOAD_DIR + msg.getUrlFichier());
                ImageView imgView = new ImageView(new Image(file.toURI().toString()));
                imgView.setFitWidth(240);
                imgView.setPreserveRatio(true);
                imgView.setCursor(Cursor.HAND);

                Rectangle clip = new Rectangle(240, 160);
                clip.setArcWidth(14); clip.setArcHeight(14);
                imgView.setClip(clip);

                imgView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0, 0, 2);");
                imgView.setOnMouseClicked(e -> { try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ex) {} });
                visualContent = imgView;
            } catch (Exception e) { visualContent = new Label("[Image introuvable]"); }
        }

        else if (msg.getTypeMessage() == TypeMessage.FICHIER && msg.getUrlFichier() != null) {
            HBox fileBox = new HBox(12);
            fileBox.setAlignment(Pos.CENTER_LEFT);
            fileBox.setPadding(new Insets(10, 14, 10, 14));
            fileBox.setCursor(Cursor.HAND);
            fileBox.setStyle(
                    "-fx-background-color: " + (isMoi ? "rgba(255,255,255,0.12)" : "rgba(16,165,165,0.08)") + "; " +
                            "-fx-background-radius: 10;"
            );

            StackPane iconCircle = new StackPane();
            iconCircle.setPrefSize(38, 38);
            iconCircle.setStyle(
                    "-fx-background-color: " + (isMoi ? "rgba(255,255,255,0.2)" : "#E0F7FA") + "; " +
                            "-fx-background-radius: 50;"
            );
            Label icon = new Label("📄");
            icon.setStyle("-fx-font-size: 18;");
            iconCircle.getChildren().add(icon);

            VBox fileInfo = new VBox(2);
            Label fileName = new Label(msg.getContenu());
            fileName.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: bold; " +
                            "-fx-text-fill: " + (isMoi ? "white" : "#2c3e50") + ";"
            );
            Label fileLabel = new Label("Appuyer pour ouvrir");
            fileLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (isMoi ? "rgba(255,255,255,0.6)" : "#7f8c8d") + ";");
            fileInfo.getChildren().addAll(fileName, fileLabel);

            fileBox.getChildren().addAll(iconCircle, fileInfo);
            fileBox.setOnMouseClicked(e -> { try { java.awt.Desktop.getDesktop().open(new File(UPLOAD_DIR + msg.getUrlFichier())); } catch (Exception ex) {} });
            visualContent = fileBox;
        }

        else if (msg.getTypeMessage() == TypeMessage.LOCATION && msg.getUrlFichier() != null) {
            String[] parts = msg.getUrlFichier().split("\\|");
            String coords = parts[0];
            String imageName = (parts.length > 1) ? parts[1] : null;

            VBox locContainer = new VBox(8);
            locContainer.setAlignment(Pos.CENTER);
            locContainer.setCursor(Cursor.HAND);

            if (imageName != null) {
                try {
                    File imgFile = new File(UPLOAD_DIR + imageName);
                    if (imgFile.exists()) {
                        ImageView mapView = new ImageView(new Image(imgFile.toURI().toString()));
                        mapView.setFitWidth(250);
                        mapView.setPreserveRatio(true);
                        Rectangle clip = new Rectangle();
                        clip.setWidth(250); clip.setHeight(150);
                        clip.setArcHeight(20); clip.setArcWidth(20);
                        mapView.setClip(clip);
                        locContainer.getChildren().add(mapView);
                    }
                } catch (Exception e) { System.err.println("Erreur chargement image carte : " + e.getMessage()); }
            }

            Label lblLink = new Label("📍 Voir l'emplacement sur Maps");
            lblLink.setUnderline(true);
            lblLink.setFont(Font.font("System", FontWeight.BOLD, 12));
            lblLink.setStyle("-fx-text-fill: " + (isMoi ? "white" : "#0D3B66") + ";");
            locContainer.getChildren().add(lblLink);

            locContainer.setOnMouseClicked(e -> {
                try {
                    String url = "https://www.google.com/maps/search/?api=1&query=" + coords;
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            visualContent = locContainer;
        }

        else if (msg.getTypeMessage() == TypeMessage.AUDIO && msg.getUrlFichier() != null) {
            HBox audioBox = new HBox(12);
            audioBox.setAlignment(Pos.CENTER_LEFT);
            audioBox.setPadding(new Insets(10, 16, 10, 12));

            Button btnPlay = new Button("▶");
            btnPlay.setPrefSize(38, 38);
            btnPlay.setStyle(
                    "-fx-background-color: " + (isMoi ? "rgba(255,255,255,0.25)" : "#10A5A5") + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 50; " +
                            "-fx-font-size: 13px; " +
                            "-fx-cursor: hand;"
            );

            HBox waveBox = new HBox(3);
            waveBox.setAlignment(Pos.CENTER);
            int[] heights = {8, 14, 20, 16, 10, 18, 12, 22, 14, 9, 17, 13};
            List<Rectangle> bars = new java.util.ArrayList<>();

            for (int h : heights) {
                Rectangle bar = new Rectangle(3, h);
                bar.setArcWidth(3); bar.setArcHeight(3);
                bar.setFill(Color.web(isMoi ? "rgba(255,255,255,0.7)" : "#10A5A5"));
                bars.add(bar);
                waveBox.getChildren().add(bar);
            }

            audioBox.setOnMouseEntered(ev -> {
                for (int i = 0; i < bars.size(); i++) {
                    Rectangle bar = bars.get(i);
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(
                            javafx.util.Duration.millis(300 + i * 40), bar
                    );
                    st.setFromY(1.0); st.setToY(1.5);
                    st.setCycleCount(javafx.animation.Animation.INDEFINITE);
                    st.setAutoReverse(true);
                    st.play();
                    bar.setUserData(st);
                }
            });

            audioBox.setOnMouseExited(ev -> {
                for (Rectangle bar : bars) {
                    if (bar.getUserData() instanceof javafx.animation.ScaleTransition) {
                        ((javafx.animation.ScaleTransition) bar.getUserData()).stop();
                        bar.setScaleY(1.0);
                    }
                }
            });

            Label lblAudio = new Label("Message vocal");
            lblAudio.setStyle(
                    "-fx-font-size: 11px; " +
                            "-fx-text-fill: " + (isMoi ? "rgba(255,255,255,0.6)" : "#7f8c8d") + ";"
            );

            VBox waveAndLabel = new VBox(4, waveBox, lblAudio);
            waveAndLabel.setAlignment(Pos.CENTER_LEFT);

            btnPlay.setOnAction(e -> {
                try {
                    File file = new File(UPLOAD_DIR_VOICE, msg.getUrlFichier().trim());
                    if (!file.exists()) {
                        System.err.println("❌ Fichier AUDIO introuvable !");
                        System.err.println("Chemin tenté : " + file.getAbsolutePath());
                        return;
                    }
                    javafx.scene.media.Media hit = new javafx.scene.media.Media(file.toURI().toString());
                    javafx.scene.media.MediaPlayer mediaPlayer = new javafx.scene.media.MediaPlayer(hit);
                    if (btnPlay.getText().equals("▶")) {
                        mediaPlayer.play(); btnPlay.setText("⏸");
                        mediaPlayer.setOnEndOfMedia(() -> btnPlay.setText("▶"));
                    } else {
                        mediaPlayer.stop(); btnPlay.setText("▶");
                    }
                } catch (Exception ex) { System.err.println("Erreur lecture audio"); }
            });

            audioBox.getChildren().addAll(btnPlay, waveAndLabel);
            visualContent = audioBox;
        }

        else {
            Label lblContent = new Label(msg.getContenu());
            lblContent.setWrapText(true);
            lblContent.setFont(new Font("System", 14));
            visualContent = lblContent;
        }

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(450);
        bubble.setPadding(new Insets(8, 12, 8, 12));

        Label lblSenderName = new Label(msg.getExpediteur().getPrenom() + " " + msg.getExpediteur().getNom());
        lblSenderName.setFont(Font.font("System", FontWeight.BOLD, 11));

        HBox topPart = new HBox(10);
        topPart.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(visualContent, Priority.ALWAYS);

        MenuButton msgMenu = new MenuButton();
        Label menuIcon = new Label("⋮"); menuIcon.setStyle("-fx-font-size: 16;");
        msgMenu.setGraphic(menuIcon);
        msgMenu.setStyle("-fx-background-color: transparent; -fx-mark-color: transparent; -fx-cursor: hand;");
        MenuItem editItem = new MenuItem("Modifier ✏️");
        MenuItem deleteItem = new MenuItem("Supprimer 🗑️");
        msgMenu.getItems().addAll(editItem, deleteItem);
        if (msg.getTypeMessage() != TypeMessage.TEXTE) editItem.setDisable(true);
        deleteItem.setOnAction(e -> handleSupprimerMessage(msg, lineContainer));
        Node finalVisualContent = visualContent;
        editItem.setOnAction(e -> { if (finalVisualContent instanceof Label) handleModifierMessage(msg, (Label) finalVisualContent); });

        topPart.getChildren().addAll(visualContent, msgMenu);

        HBox footerPart = new HBox(8);
        footerPart.setAlignment(Pos.CENTER_RIGHT);
        Label lblTime = new Label(msg.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")));
        lblTime.setFont(new Font("System", 9));
        footerPart.getChildren().add(lblTime);

        bubble.getChildren().addAll(lblSenderName, topPart, footerPart);
        StackPane bubbleStack = new StackPane(bubble);

        if (isMoi) {
            lineContainer.setAlignment(Pos.CENTER_RIGHT);
            lblSenderName.setStyle("-fx-text-fill: #8ECAE6;");
            bubble.setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #0D3B66, #0a2f55); " +
                            "-fx-background-radius: 18 18 4 18; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 8, 0, 0, 3);"
            );

            if (visualContent instanceof Label) ((Label) visualContent).setStyle("-fx-text-fill: white;");
            if (visualContent instanceof HBox) {
                ((HBox) visualContent).lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
            }

            lblTime.setStyle("-fx-text-fill: rgba(255,255,255,0.5);");
            menuIcon.setStyle("-fx-text-fill: rgba(255,255,255,0.8);");

            Label lblStatus = new Label(msg.isLu() ? "✓✓" : "✓");
            lblStatus.setStyle(msg.isLu()
                    ? "-fx-text-fill: #10A5A5; -fx-font-weight: bold;"
                    : "-fx-text-fill: rgba(255,255,255,0.4);"
            );
            footerPart.getChildren().add(lblStatus);
            lineContainer.getChildren().addAll(bubbleStack, avatarContainer);
        }

        else {
            lineContainer.setAlignment(Pos.CENTER_LEFT);
            lblSenderName.setStyle("-fx-text-fill: #10A5A5;");
            bubble.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 4 18 18 18; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
            );

            if (visualContent instanceof Label) ((Label) visualContent).setStyle("-fx-text-fill: #2c3e50;");
            lblTime.setStyle("-fx-text-fill: #7f8c8d;");
            msgMenu.setVisible(false);

            Button btnReact = new Button();
            Label emojiIcon = new Label("☺");
            emojiIcon.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 20; -fx-text-fill: #7f8c8d;");
            btnReact.setGraphic(emojiIcon);
            btnReact.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 50; " +
                            "-fx-border-color: #eee; " +
                            "-fx-border-radius: 50; " +
                            "-fx-cursor: hand; " +
                            "-fx-padding: 5;"
            );
            btnReact.setOnAction(e -> showReactionMenu(btnReact, msg, bubbleStack));

            lineContainer.getChildren().addAll(avatarContainer, bubbleStack, btnReact);
        }

        if (msg.getReaction() != null && !msg.getReaction().isEmpty()) {
            afficherBadgeReaction(bubbleStack, msg.getReaction(), isMoi);
        }

        vboxMessages.getChildren().add(lineContainer);

        if (!isMoi && scrollPaneMessages.getVvalue() < 0.9) {
            newMessagesCount++;
            lblNewMsgBadge.setText(String.valueOf(newMessagesCount));
            lblNewMsgBadge.setVisible(true);
        }
    }

    @FXML
    private void scrollToBottom() {
        scrollPaneMessages.setVvalue(1.0);
        newMessagesCount = 0;
        lblNewMsgBadge.setVisible(false);
        paneScrollDown.setVisible(false);
        paneScrollDown.setManaged(false);
    }
    private void handleSupprimerMessage(Messages msg, HBox lineContainer) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText("Supprimer ce message ?");
        confirm.setContentText("Le message sera supprimé pour tout le monde.");
        styliserBoiteDialogue(confirm.getDialogPane(), "#e74c3c");

        if (confirm.showAndWait().get() == ButtonType.OK) {
            try {
                serMsg.deleteOne(msg);
                Conversation current = listConversations.getSelectionModel().getSelectedItem();
                chargerHistorique(current.getIdConversation());
                listConversations.refresh();

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleModifierMessage(Messages msg, Label lblContent) {
        HBox parent = (HBox) lblContent.getParent();
        int index = parent.getChildren().indexOf(lblContent);

        TextField editField = new TextField(msg.getContenu());
        HBox.setHgrow(editField, Priority.ALWAYS);
        editField.setMaxWidth(Double.MAX_VALUE);
        editField.setPrefHeight(30);
        editField.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.15); " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 14px; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: rgba(255, 255, 255, 0.4); " +
                        "-fx-border-radius: 10; " +
                        "-fx-padding: 5 10 5 10;"
        );
        editField.setPrefWidth(lblContent.getWidth());

        parent.getChildren().set(index, editField);
        editField.requestFocus();
        editField.selectAll();
        editField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                validerModificationInline(msg, lblContent, editField, parent, index);
            } else if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                parent.getChildren().set(index, lblContent);
            }
        });

        editField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && parent.getChildren().contains(editField)) {
                validerModificationInline(msg, lblContent, editField, parent, index);
            }
        });
    }

    private void validerModificationInline(Messages msg, Label lblContent, TextField editField, HBox parent, int index) {
        String newText = editField.getText().trim();

        if (!newText.isEmpty() && !newText.equals(msg.getContenu())) {
            try {
                msg.setContenu(newText);
                serMsg.updateOne(msg);
                lblContent.setText(newText);
                listConversations.refresh();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        parent.getChildren().set(index, lblContent);
    }

    private void chargerHistorique(int idConversation) {
        vboxMessages.getChildren().clear();
        try {
            List<Messages> historique = serMsg.selectByConversation(idConversation);

            for (Messages m : historique) {
                boolean isCurrentUser = (m.getExpediteur().getIdUtilisateur() == currentUserId);
                renderMessage(m, isCurrentUser);
            }
            scrollPaneMessages.setVvalue(1.0);

        } catch (SQLException e) {
            System.err.println("Erreur chargement messages : " + e.getMessage());
        }
    }

    private void listConversationStyle() {
        listConversations.setCellFactory(lv -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation conv, boolean empty) {
                super.updateItem(conv, empty);

                if (empty || conv == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox cellRoot = new HBox(12);
                    cellRoot.setAlignment(Pos.CENTER_LEFT);
                    cellRoot.setPadding(new Insets(10, 15, 10, 15));
                    cellRoot.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0; -fx-cursor: hand;");

                    StackPane avatarContainer = new StackPane();
                    avatarContainer.setPrefSize(45, 45);
                    avatarContainer.setMaxSize(45, 45);

                    String displayName = serConv.getNomAffichage(conv, currentUserId);
                    String initials = getInitials(displayName);

                    if (conv.getTypeConversation() == TypeConversation.GROUPE) {
                        Circle cBack = new Circle(15, Color.web("#bdc3c7"));
                        cBack.setStroke(Color.WHITE);
                        cBack.setStrokeWidth(2);

                        Circle cFront = new Circle(15, Color.web("#10A5A5"));
                        cFront.setStroke(Color.WHITE);
                        cFront.setStrokeWidth(2);

                        Text tGroup = new Text(initials.substring(0, 1).toUpperCase());
                        tGroup.setFill(Color.WHITE);
                        tGroup.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");

                        StackPane frontGroup = new StackPane(cFront, tGroup);

                        avatarContainer.getChildren().addAll(cBack, frontGroup);

                        StackPane.setAlignment(cBack, Pos.TOP_RIGHT);
                        StackPane.setMargin(cBack, new Insets(2, 2, 0, 0));
                        StackPane.setAlignment(frontGroup, Pos.BOTTOM_LEFT);
                        StackPane.setMargin(frontGroup, new Insets(0, 0, 2, 2));

                    } else {
                        Circle circle = new Circle(20, Color.web("#10A5A5"));
                        Text txt = new Text(initials);
                        txt.setFill(Color.WHITE);
                        txt.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
                        avatarContainer.getChildren().addAll(circle, txt);
                    }
                    VBox textContainer = new VBox(3);
                    HBox.setHgrow(textContainer, Priority.ALWAYS);

                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER_LEFT);

                    Label lblName = new Label(displayName);
                    lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-size: 14px; -fx-font-weight: bold;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label lblTime = new Label();
                    lblTime.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10px;");
                    topRow.getChildren().addAll(lblName, spacer, lblTime);

                    Label lblLastMsg = new Label();
                    lblLastMsg.setEllipsisString("...");
                    lblLastMsg.setMaxWidth(180);
                    try {
                        Messages last = serMsg.selectLastMessage(conv.getIdConversation());

                        if (last != null) {
                            lblLastMsg.setText(last.getContenu());
                            lblTime.setText(last.getDateEnvoi().format(DateTimeFormatter.ofPattern("HH:mm")));

                            if (!last.isLu() && last.getExpediteur().getIdUtilisateur() != currentUserId) {
                                lblLastMsg.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold;");
                                lblName.setStyle("-fx-text-fill: #10A5A5; -fx-font-weight: bold;");
                            } else {
                                lblLastMsg.setStyle("-fx-text-fill: #7f8c8d;");
                            }
                        } else {
                            lblLastMsg.setText("Aucun message...");
                            lblLastMsg.setStyle("-fx-text-fill: #10A5A5; -fx-font-style: italic; -fx-font-size: 12px;");
                            lblTime.setText(conv.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM")));
                        }
                    } catch (SQLException e) {
                        lblLastMsg.setText("Erreur...");
                    }

                    textContainer.getChildren().addAll(topRow, lblLastMsg);
                    cellRoot.getChildren().addAll(avatarContainer, textContainer);
                    setGraphic(cellRoot);

                    cellRoot.setOnMouseEntered(e -> cellRoot.setStyle("-fx-background-color: #f4fbfc; -fx-border-color: #10A5A5; -fx-border-width: 0 0 1 0;"));
                    cellRoot.setOnMouseExited(e -> cellRoot.setStyle("-fx-background-color: transparent; -fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;"));
                }
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] words = name.split(" ");
        if (words.length >= 2) {
            return (words[0].substring(0, 1) + words[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    @FXML
    private void handleSendMessage() {
        String text = inputMessage.getText();

        if (text == null || text.trim().isEmpty()) {
            inputMessage.setStyle("-fx-border-color: #e74c3c; -fx-background-radius: 20; -fx-border-radius: 20;");
            return;
        }

        if (text.length() > 1000) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Message trop long (max 1000).");
            styliserAlerte(alert);
            alert.show();
            return;
        }

        Conversation currentConv = listConversations.getSelectionModel().getSelectedItem();
        if (currentConv == null) return;

        try {
            Messages m = new Messages();
            m.setContenu(text.trim());
            m.setDateEnvoi(LocalDateTime.now());
            m.setLu(false);
            m.setConversation(currentConv);
            m.setTypeMessage(TypeMessage.TEXTE);
            m.setUrlFichier(null);
            Utilisateur moi = new Utilisateur();
            moi.setIdUtilisateur(currentUserId);
            m.setExpediteur(userConnecte);

            serMsg.insertOne(m);
            renderMessage(m, true);
            inputMessage.clear();
            inputMessage.setStyle("-fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #E0E0E0;");
            Platform.runLater(() -> scrollPaneMessages.setVvalue(1.0));
            loadConversations();
            listConversations.getSelectionModel().select(currentConv);

        } catch (SQLException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'envoi : " + e.getMessage());
            alert.show();
        }
    }
    private void styliserAlerte(Alert alert) {
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #0D3B66; -fx-border-width: 2;");
    }
    private void configurerMenuOptions(Conversation conv) {
        btnOptions.getItems().clear();

        MenuItem itemMedia = creerMenuItem("📎  Voir les fichiers", null);
        itemMedia.setOnAction(e -> handleShowMedia());

        MenuItem itemSupprimer = creerMenuItem("🗑   Supprimer la discussion", "menu-item-danger");
        itemSupprimer.setOnAction(e -> handleSupprimerConversation(conv));

        btnOptions.getItems().add(itemMedia);

        if (conv.getTypeConversation() == TypeConversation.GROUPE) {

            MenuItem itemModifier = creerMenuItem("✏️  Modifier le nom", null);
            itemModifier.setOnAction(e -> handleModifierNomGroupe(conv));

            MenuItem itemVoirMembres = creerMenuItem("👥  Voir les membres", null);
            itemVoirMembres.setOnAction(e -> handleVoirMembres(conv));

            MenuItem itemAjouter = creerMenuItem("👤  Ajouter un membre", null);
            itemAjouter.setOnAction(e -> handleAjouterMembre(conv));

            MenuItem itemQuitter = creerMenuItem("🚪  Quitter le groupe", "menu-item-warning");
            itemQuitter.setOnAction(e -> handleQuitterGroupe(conv));

            btnOptions.getItems().addAll(
                    itemModifier,
                    itemVoirMembres,
                    itemAjouter,
                    new SeparatorMenuItem(),
                    itemQuitter,
                    new SeparatorMenuItem(),
                    itemSupprimer
            );
        } else {
            btnOptions.getItems().add(itemSupprimer);
        }
    }

    private MenuItem creerMenuItem(String texte, String styleClass) {
        MenuItem item = new MenuItem(texte);
        if (styleClass != null) {
            item.getStyleClass().add(styleClass);
        }
        return item;
    }

    private void handleModifierNomGroupe(Conversation conv) {
        TextInputDialog dialog = new TextInputDialog(conv.getTitre());
        dialog.setTitle("Rehletna - Gestion de groupe");
        dialog.setHeaderText("Modifier le nom du groupe");
        dialog.setContentText("Nouveau nom :");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #10A5A5; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10;"
        );
        Node headerPanel = dialogPane.lookup(".header-panel");
        if (headerPanel != null) {
            headerPanel.setStyle("-fx-background-color: #0077B6; -fx-background-radius: 10 10 0 0;");
            Label headerLabel = (Label) dialogPane.lookup(".header-panel > .label");
            if (headerLabel != null) {
                headerLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
            }
        }
        TextField textField = dialog.getEditor();
        textField.setStyle(
                "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-border-color: #bdc3c7; " +
                        "-fx-padding: 5 10 5 10;"
        );
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        String btnStyle = "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;";
        okButton.setStyle(btnStyle + "-fx-background-color: #10A5A5; -fx-text-fill: white;");
        cancelButton.setStyle(btnStyle + "-fx-background-color: #ecf0f1; -fx-text-fill: #7f8c8d;");
        dialog.showAndWait().ifPresent(newTitle -> {
            if (!newTitle.trim().isEmpty() && !newTitle.equals(conv.getTitre())) {
                try {
                    conv.setTitre(newTitle.trim());
                    serConv.updateOne(conv);
                    lblNomContact.setText(newTitle.trim());
                    listConversations.refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleVoirMembres(Conversation conv) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Membres du groupe");
        dialog.setHeaderText("Liste des participants de " + conv.getTitre());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(300);

        try {
            ServiceParticipantConversation spc = new ServiceParticipantConversation();
            List<Utilisateur> membres = spc.getParticipantsByConversation(conv.getIdConversation());

            for (Utilisateur u : membres) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                Label name = new Label(u.getPrenom() + " " + u.getNom());
                Label role = new Label("(" + u.getRole() + ")");
                role.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");
                row.getChildren().addAll(name, role);
                content.getChildren().add(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-border-color: #0D3B66; -fx-border-width: 2;");

        dialog.showAndWait();
    }

    private void handleQuitterGroupe(Conversation conv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Quitter la conversation");
        alert.setHeaderText("Voulez-vous vraiment quitter " + conv.getTitre() + " ?");
        alert.setContentText("Vous pourrez toujours voir les anciens messages, mais vous ne pourrez plus écrire.");
        if (alert.showAndWait().get() == ButtonType.OK) {
            try {

                spc.quitterConversation(currentUserId, conv.getIdConversation());
                inputArea.setVisible(false);
                inputArea.setManaged(false);
                paneQuitte.setVisible(true);
                paneQuitte.setManaged(true);
                loadConversations();
            }catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleAjouterMembre(Conversation conv) {
        Dialog<Utilisateur> dialog = new Dialog<>();
        dialog.setTitle("Rehletna - Ajouter un membre");
        dialog.setHeaderText("Choisissez un utilisateur pour l'ajouter à " + conv.getTitre());

        ListView<Utilisateur> userListView = new ListView<>();
        userListView.setPrefSize(300, 400);

        userListView.setStyle("-fx-background-color: transparent; -fx-selection-bar: #E0F7FA;");
        userListView.setCellFactory(lv -> new ListCell<Utilisateur>() {
            @Override
            protected void updateItem(Utilisateur user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    VBox box = new VBox(2);
                    box.setPadding(new Insets(8, 10, 8, 10));
                    box.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;");

                    Label lblName = new Label(user.getPrenom() + " " + user.getNom());
                    lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-weight: bold;");

                    Label lblEmail = new Label(user.getEmail());
                    lblEmail.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");

                    box.getChildren().addAll(lblName, lblEmail);
                    setGraphic(box);
                }
            }
        });
        try {
            List<Utilisateur> allUsers = serUser.selectALL();
            userListView.getItems().setAll(allUsers);
        } catch (SQLException e) { e.printStackTrace(); }
        userListView.setOnMouseClicked(event -> {
            Utilisateur selectedUser = userListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirmation");
                confirm.setHeaderText("Ajouter " + selectedUser.getPrenom() + " au groupe ?");
                confirm.setContentText("Cet utilisateur pourra voir l'historique du groupe.");

                confirm.getDialogPane().setStyle("-fx-border-color: #10A5A5; -fx-border-width: 2;");

                if (confirm.showAndWait().get() == ButtonType.OK) {
                    try {
                        ServiceParticipantConversation spc = new ServiceParticipantConversation();
                        spc.insertOne(new ParticipantConversation(selectedUser, conv, LocalDateTime.now()));
                        dialog.close();

                        new Alert(Alert.AlertType.INFORMATION, selectedUser.getPrenom() + " a rejoint le groupe !").show();
                    } catch (SQLException e) {
                        new Alert(Alert.AlertType.ERROR, "Cet utilisateur est déjà dans le groupe.").show();
                    }
                }
            }
        });
        VBox dialogContent = new VBox(userListView);
        dialogContent.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        DialogPane dp = dialog.getDialogPane();
        dp.setStyle("-fx-background-color: white; -fx-border-color: #0D3B66; -fx-border-width: 2;");

        dialog.showAndWait();
    }

    private void handleSupprimerConversation(Conversation conv) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Rehletna - Suppression");
        alert.setHeaderText("Supprimer la discussion ?");
        alert.setContentText("Tous les messages seront définitivement perdus pour tout le monde.");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-border-color: #e74c3c; -fx-border-width: 2; -fx-border-radius: 10; -fx-background-radius: 10;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        okButton.setText("Supprimer");
        okButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 20; -fx-font-weight: bold;");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                serConv.deleteOne(conv);
                masterData.remove(conv);
                chatArea.setVisible(false);
                chatArea.setManaged(false);
                paneDefault.setVisible(true);
                paneDefault.setManaged(true);

                System.out.println("Conversation supprimée !");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private void mettreAJourStatut(Conversation conv) {
        if (conv.getTypeConversation() == TypeConversation.GROUPE) {
            circleStatus.setVisible(false);
            circleStatus.setManaged(false);
        } else {
            circleStatus.setVisible(true);
            circleStatus.setManaged(true);
            boolean estEnligne = true;
            if (estEnligne) {
                circleStatus.setFill(Color.web("#2ecc71"));
            } else {
                circleStatus.setFill(Color.web("#bdc3c7"));
            }
        }
    }

    private void initUploadFolder() {
        File directory = new File(UPLOAD_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
    private String copierFichier(File sourceFile) throws IOException {
        initUploadFolder();
        String fileName = System.currentTimeMillis() + "_" + sourceFile.getName();
        File destFile = new File(UPLOAD_DIR + fileName);
        java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath());

        return fileName;
    }

    private final List<String> imageExtensions = List.of(".png", ".jpg", ".jpeg", ".gif", ".bmp");

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Rehletna - Envoyer un fichier");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Documents PDF", "*.pdf")
        );

        File selectedFile = fileChooser.showOpenDialog(btnAddFile.getScene().getWindow());

        if (selectedFile != null) {
            try {
                File directory = new File(UPLOAD_DIR);
                if (!directory.exists()) directory.mkdirs();

                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                String uniqueName = System.currentTimeMillis() + "_" + selectedFile.getName().replaceAll("\\s+", "_");
                File destinationFile = new File(UPLOAD_DIR + uniqueName);

                Files.copy(selectedFile.toPath(), destinationFile.toPath());
                TypeMessage type = imageExtensions.contains(extension.toLowerCase()) ? TypeMessage.IMAGE : TypeMessage.FICHIER;

                Conversation currentConv = listConversations.getSelectionModel().getSelectedItem();
                if (currentConv == null) return;

                Messages m = new Messages();
                m.setContenu(selectedFile.getName());
                m.setDateEnvoi(LocalDateTime.now());
                m.setLu(false);
                m.setTypeMessage(type);
                m.setUrlFichier(uniqueName);
                m.setConversation(currentConv);

                Utilisateur moi = new Utilisateur();
                moi.setIdUtilisateur(currentUserId);
                m.setExpediteur(moi);

                serMsg.insertOne(m);

                renderMessage(m, true);

                loadConversations();
                listConversations.getSelectionModel().select(currentConv);

                Platform.runLater(() -> scrollPaneMessages.setVvalue(1.0));

                System.out.println("Fichier sauvegardé dans : " + destinationFile.getAbsolutePath());

            } catch (java.io.IOException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur lors de la copie du fichier : " + e.getMessage()).show();
            } catch (SQLException e) {
                new Alert(Alert.AlertType.ERROR, "Erreur base de données : " + e.getMessage()).show();
            }
        }
    }

    @FXML
    private void handleShowMedia() {
        paneMediaHistory.setVisible(true);
        paneMediaHistory.setManaged(true);
        vboxMediaList.getChildren().clear();
        vboxMediaList.setSpacing(15);

        Conversation currentConv = listConversations.getSelectionModel().getSelectedItem();
        if (currentConv == null) return;

        try {
            List<Messages> medias = serMsg.getMediaHistory(currentConv.getIdConversation());
            if (medias.isEmpty()) {
                VBox emptyState = new VBox(15);
                emptyState.setAlignment(Pos.CENTER);
                emptyState.setPadding(new Insets(50, 20, 0, 20));

                Label iconEmpty = new Label("📂");
                iconEmpty.setStyle("-fx-font-size: 40; -fx-text-fill: #bdc3c7;");

                Label lblEmpty = new Label("Aucun fichier trouvé");
                lblEmpty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14; -fx-font-style: italic;");

                emptyState.getChildren().addAll(iconEmpty, lblEmpty);
                vboxMediaList.getChildren().add(emptyState);
                return;
            }
            for (Messages m : medias) {
                HBox card = new HBox(12);
                card.setAlignment(Pos.CENTER_LEFT);
                card.setPadding(new Insets(12));

                card.setStyle("-fx-background-color: #F8FAFB; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-color: #ECF0F1; " +
                        "-fx-border-radius: 10; " +
                        "-fx-cursor: hand;");

                card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #E0F7FA; -fx-background-radius: 10; -fx-border-color: #10A5A5; -fx-border-radius: 10; -fx-cursor: hand;"));
                card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #F8FAFB; -fx-background-radius: 10; -fx-border-color: #ECF0F1; -fx-border-radius: 10;"));

                Node preview;
                if (m.getTypeMessage() == TypeMessage.IMAGE) {
                    try {
                        File file = new File(UPLOAD_DIR + m.getUrlFichier());
                        ImageView iv = new ImageView(new Image(file.toURI().toString()));
                        iv.setFitWidth(45); iv.setFitHeight(45); iv.setPreserveRatio(true);
                        Rectangle clip = new Rectangle(45, 45);
                        clip.setArcWidth(10); clip.setArcHeight(10);
                        iv.setClip(clip);
                        preview = iv;
                    } catch (Exception e) {
                        preview = new Label("🖼️");
                    }
                } else {
                    Label fileIcon = new Label("📄");
                    fileIcon.setStyle("-fx-font-size: 24; -fx-text-fill: #0D3B66;");
                    preview = fileIcon;
                }

                VBox info = new VBox(3);
                Label name = new Label(m.getContenu());
                name.setStyle("-fx-font-weight: bold; -fx-text-fill: #0D3B66; -fx-font-size: 13;");
                name.setWrapText(true);
                name.setMaxWidth(160);

                Label date = new Label(m.getDateEnvoi().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
                date.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 10;");

                info.getChildren().addAll(name, date);

                card.getChildren().addAll(preview, info);

                card.setOnMouseClicked(e -> {
                    try { java.awt.Desktop.getDesktop().open(new File(UPLOAD_DIR + m.getUrlFichier())); } catch (Exception ex) {}
                });

                vboxMediaList.getChildren().add(card);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML
    private void handleCloseMedia() {
        paneMediaHistory.setVisible(false);
        paneMediaHistory.setManaged(false);
    }

    private void showReactionMenu(Button source, Messages msg, StackPane bubble) {
        HBox emojiBar = new HBox(15);
        emojiBar.setAlignment(Pos.CENTER);
        emojiBar.setPadding(new Insets(10, 20, 10, 20));

        emojiBar.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 40; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 15, 0, 0, 5);"
        );

        String[] emojis = {"❤️", "😂", "😮", "😢", "👍", "🙏"};

        javafx.stage.Popup popup = new javafx.stage.Popup();
        popup.setAutoHide(true);
        popup.getContent().add(emojiBar);

        for (String e : emojis) {
            Label lblEmoji = new Label(e);
            lblEmoji.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 25; -fx-cursor: hand;");

            lblEmoji.setOnMouseEntered(ev -> {
                lblEmoji.setScaleX(1.4);
                lblEmoji.setScaleY(1.4);
                lblEmoji.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 25; -fx-cursor: hand; -fx-background-color: #E0F7FA; -fx-background-radius: 50;");
            });

            lblEmoji.setOnMouseExited(ev -> {
                lblEmoji.setScaleX(1.0);
                lblEmoji.setScaleY(1.0);
                lblEmoji.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 25; -fx-cursor: hand; -fx-background-color: transparent;");
            });

            lblEmoji.setOnMouseClicked(ev -> {
                try {
                    serMsg.updateReaction(msg.getIdMessage(), e);
                    msg.setReaction(e);

                    boolean isMoi = (msg.getExpediteur().getIdUtilisateur() == currentUserId);

                    afficherBadgeReaction(bubble, e, isMoi);

                    popup.hide();
                } catch (SQLException ex) { ex.printStackTrace(); }
            });

            emojiBar.getChildren().add(lblEmoji);
        }

        double x = source.localToScreen(source.getBoundsInLocal()).getMinX();
        double y = source.localToScreen(source.getBoundsInLocal()).getMinY();
        popup.show(source, x - 100, y - 60);
    }

    private void afficherBadgeReaction(StackPane stack, String emoji, boolean isMoi) {
        stack.getChildren().removeIf(n -> n instanceof Label && "reaction".equals(n.getAccessibleText()));

        Label badge = new Label(emoji);
        badge.setAccessibleText("reaction");

        badge.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #2c3e50; " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 3 6 3 6; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-family: 'Segoe UI Emoji'; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 1);"
        );

        stack.setAlignment(badge, isMoi ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);

        badge.setTranslateY(10);
        badge.setTranslateX(isMoi ? -10 : 10);

        stack.getChildren().add(badge);
    }



    @FXML
    private void startRecording() {
        String fileName = "voice_" + System.currentTimeMillis() + ".wav";
        currentAudioFile = new File(UPLOAD_DIR_VOICE + fileName);

        btnMic.setStyle("-fx-background-color: #c0392b; -fx-background-radius: 50; -fx-scale-x: 1.2; -fx-scale-y: 1.2;");

        recorder.start(currentAudioFile);
    }

    @FXML
    private void stopRecording() {
        // 1. Arrêter le micro physiquement
        recorder.stop();
        btnMic.setStyle("-fx-background-color: #0077B6; -fx-background-radius: 50;");

        // 2. Récupérer la conversation actuelle
        Conversation currentConv = listConversations.getSelectionModel().getSelectedItem();
        if (currentConv == null) return;

        // 3. CRÉATION IMMÉDIATE DE L'OBJET (en mémoire)
        Messages m = new Messages();
        m.setContenu("🎤 Message Vocal");
        m.setTypeMessage(TypeMessage.AUDIO);
        m.setUrlFichier(currentAudioFile.getName());
        m.setDateEnvoi(LocalDateTime.now());
        m.setConversation(currentConv);
        m.setExpediteur(userConnecte);

        // 4. AFFICHAGE INSTANTANÉ (On n'attend pas la DB !)
        renderMessage(m, true);
        Platform.runLater(() -> scrollPaneMessages.setVvalue(1.0));

        // 5. SAUVEGARDE EN ARRIÈRE-PLAN (Thread séparé)
        new Thread(() -> {
            try {
                // Petit délai de sécurité (200ms suffisent) pour que le fichier .wav soit fini d'écrire
                Thread.sleep(200);

                // Insertion en base de données
                serMsg.insertOne(m);

                // Mise à jour de la liste de gauche (le dernier message) sans clignotement
                Platform.runLater(() -> {
                    loadConversations();
                    listConversations.getSelectionModel().select(currentConv);
                    listConversations.refresh();
                });

            } catch (Exception e) {
                System.err.println("Erreur lors de la sauvegarde du vocal : " + e.getMessage());
            }
        }).start();
    }

    @FXML
    private void handleSendLocation() {
        Stage mapStage = new Stage();
        mapStage.setTitle("Rehletna - Partager ma position");

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        engine.load(getClass().getResource("/html/map.html").toExternalForm());

        Button btnValider = new Button("📍 Envoyer ma position actuelle");
        btnValider.setStyle("-fx-background-color: #10A5A5; -fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold;");
        btnValider.setPadding(new Insets(12, 30, 12, 30));

        btnValider.setOnAction(e -> {
            Object lat = engine.executeScript("window.selectedLat");
            Object lon = engine.executeScript("window.selectedLon");

            if (lat != null && !lat.toString().equals("null")) {
                WritableImage snapshot = webView.snapshot(new SnapshotParameters(), null);
                String imgName = "map_" + System.currentTimeMillis() + ".png";
                try {
                    File file = new File(UPLOAD_DIR + imgName);
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot, null), "png", file);
                    envoyerMessageLocation(lat.toString() + "," + lon.toString() + "|" + imgName);
                    mapStage.close();
                } catch (Exception ex) { ex.printStackTrace(); }
            } else {
                new Alert(Alert.AlertType.WARNING, "GPS non prêt. Cliquez sur la carte.").show();
            }
        });

        VBox layout = new VBox(webView, btnValider);
        VBox.setVgrow(webView, Priority.ALWAYS);
        layout.setAlignment(Pos.CENTER);
        mapStage.setScene(new Scene(layout, 700, 550));

        mapStage.show();

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                engine.executeScript("fixMap()");
            }
        });
    }

    private void envoyerMessageLocation(String coords) {
        try {
            Messages m = new Messages();
            m.setContenu("📍 Position partagée");
            m.setTypeMessage(TypeMessage.LOCATION);
            m.setUrlFichier(coords);
            m.setDateEnvoi(LocalDateTime.now());
            m.setConversation(listConversations.getSelectionModel().getSelectedItem());
            m.setExpediteur(userConnecte);

            serMsg.insertOne(m);
            renderMessage(m, true);
            loadConversations();
        } catch (SQLException e) { e.printStackTrace(); }
    }

}