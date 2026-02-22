package controllers;

import entities.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import services.ServiceConversation;
import services.ServiceMessage;
import services.ServiceParticipantConversation;
import services.ServiceUtilisateur;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDateTime;
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

    @FXML private TextField userSearchField; // Le nouveau champ
    private ObservableList<Utilisateur> masterUserList = FXCollections.observableArrayList();
    private FilteredList<Utilisateur> filteredUserList;

    private ServiceConversation serConv = new ServiceConversation();
    private ServiceMessage serMsg = new ServiceMessage();
    private final ServiceUtilisateur serUser = new ServiceUtilisateur();
    private final ServiceParticipantConversation spc = new ServiceParticipantConversation();
    private int currentUserId = 11;
    private Utilisateur userConnecte;
    private boolean isGroupMode = false;
    private Set<Utilisateur> selectedUsers = new HashSet<>();
    @FXML
    private Button btnConfirmGroup;

    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";
    @FXML
    private Button btnAddFile;

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

        String styleNormal = "-fx-background-color: white; -fx-border-color: #10A5A5; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: #0D3B66; -fx-cursor: hand;";
        String styleSelected = "-fx-background-color: #10A5A5; -fx-border-color: #10A5A5; -fx-border-radius: 20; -fx-background-radius: 20; -fx-text-fill: white; -fx-cursor: hand;";
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
    }

    private void loadConversations() {
        try {
            masterData.setAll(serConv.selectByUser(currentUserId));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void afficherDiscussion(Conversation conv) {
        vboxMessages.getChildren().clear();

        try {
            String nomAafficher = serConv.getNomAffichage(conv, currentUserId);
            lblNomContact.setText(nomAafficher);
            mettreAJourStatut(conv);
            configurerMenuOptions(conv);

            if (conv.getTypeConversation() == TypeConversation.GROUPE) {
                circleStatus.setVisible(false);
                circleStatus.setManaged(false);
                circleStatus.setOpacity(0);
            } else {
                circleStatus.setVisible(true);
                circleStatus.setManaged(true);
                circleStatus.setOpacity(1);
            }

            List<Message> messages = serMsg.selectByConversation(conv.getIdConversation());
            for (Message m : messages) {
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
                    } else {
                        HBox container = new HBox(10);
                        container.setAlignment(Pos.CENTER_LEFT);
                        container.setPadding(new Insets(10, 15, 10, 15));
                        container.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0;");

                        VBox infoBox = new VBox(2);
                        Label lblName = new Label(user.getPrenom() + " " + user.getNom());
                        lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-weight: bold;");
                        Label lblEmail = new Label(user.getEmail());
                        lblEmail.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
                        infoBox.getChildren().addAll(lblName, lblEmail);

                        if (isGroupMode) {
                            CheckBox cb = new CheckBox();
                            cb.setSelected(selectedUsers.contains(user));
                            cb.setOnAction(e -> {
                                if (cb.isSelected()) selectedUsers.add(user);
                                else selectedUsers.remove(user);
                            });
                            container.getChildren().addAll(cb, infoBox);
                        } else {
                            container.getChildren().add(infoBox);
                            container.setCursor(Cursor.HAND);
                        }
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

        } catch (SQLException e) { e.printStackTrace(); }
    }
    public void handleConfirmGroupSelection() throws SQLException {
        // 1. Contrôle de saisie
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
            Alert alert = new Alert(Alert.AlertType.INFORMATION); // Information est plus doux que Warning ici
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

    private void renderMessage(Message msg, boolean isCurrentUser) {
        HBox lineContainer = new HBox(10);
        lineContainer.setPadding(new Insets(8, 15, 8, 15));

        ImageView avatarView = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream("/images/default_user.png"));
            avatarView.setImage(img);
        } catch (Exception e) {
            System.err.println("Avatar introuvable");
        }
        avatarView.setFitHeight(35);
        avatarView.setFitWidth(35);
        avatarView.setClip(new Circle(17.5, 17.5, 17.5));

        VBox bubble = new VBox(3);
        bubble.setMaxWidth(450);
        bubble.setPadding(new Insets(8, 12, 8, 12));

        Label lblSenderName = new Label(msg.getExpediteur().getPrenom() + " " + msg.getExpediteur().getNom());
        lblSenderName.setFont(Font.font("System", FontWeight.BOLD, 11));

        Node visualContent;

        if (msg.getTypeMessage() == TypeMessage.IMAGE && msg.getUrlFichier() != null) {
            try {
                File file = new File(UPLOAD_DIR + msg.getUrlFichier());
                Image img = new Image(file.toURI().toString());
                ImageView imgView = new ImageView(img);
                imgView.setFitWidth(250);
                imgView.setPreserveRatio(true);
                imgView.setCursor(Cursor.HAND);
                imgView.setOnMouseClicked(e -> {
                    try { java.awt.Desktop.getDesktop().open(file); } catch (Exception ex) {}
                });
                visualContent = imgView;
            } catch (Exception e) {
                visualContent = new Label("[Image introuvable]");
            }
        } else if (msg.getTypeMessage() == TypeMessage.FICHIER && msg.getUrlFichier() != null) {
            HBox fileBox = new HBox(10);
            fileBox.setAlignment(Pos.CENTER_LEFT);
            fileBox.setPadding(new Insets(8)); // Un peu plus de padding pour le confort
            fileBox.setCursor(Cursor.HAND);

            Label icon = new Label("📄");
            icon.setStyle("-fx-font-size: 18;");
            Label fileName = new Label(msg.getContenu());
            fileName.setUnderline(true);
            fileName.setWrapText(true);
            fileName.setMaxWidth(300);

            fileBox.getChildren().addAll(icon, fileName);

            fileBox.setOnMouseClicked(e -> {
                try {
                    java.awt.Desktop.getDesktop().open(new File(UPLOAD_DIR + msg.getUrlFichier()));
                } catch (Exception ex) {
                    System.err.println("Impossible d'ouvrir le fichier : " + ex.getMessage());
                }
            });
            visualContent = fileBox;
        } else {
            Label lblContent = new Label(msg.getContenu());
            lblContent.setWrapText(true);
            lblContent.setFont(new Font("System", 14));
            visualContent = lblContent;
        }

        HBox topPart = new HBox(10);
        topPart.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(visualContent, Priority.ALWAYS);

        MenuButton msgMenu = new MenuButton();
        Label menuIcon = new Label("⋮");
        menuIcon.setStyle("-fx-font-size: 16;");
        msgMenu.setGraphic(menuIcon);
        msgMenu.setStyle("-fx-background-color: transparent; -fx-mark-color: transparent; -fx-cursor: hand;");

        MenuItem editItem = new MenuItem("Modifier ✏️");
        MenuItem deleteItem = new MenuItem("Supprimer 🗑️");
        msgMenu.getItems().addAll(editItem, deleteItem);
        if (msg.getTypeMessage() != TypeMessage.TEXTE) editItem.setDisable(true);

        deleteItem.setOnAction(e -> handleSupprimerMessage(msg, lineContainer));
        Node finalVisualContent = visualContent;
        editItem.setOnAction(e -> {
            if (finalVisualContent instanceof Label) handleModifierMessage(msg, (Label) finalVisualContent);
        });

        topPart.getChildren().addAll(visualContent, msgMenu);

        HBox footerPart = new HBox(8);
        footerPart.setAlignment(Pos.CENTER_RIGHT);
        String time = msg.getDateEnvoi().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        Label lblTime = new Label(time);
        lblTime.setFont(new Font("System", 9));
        footerPart.getChildren().add(lblTime);

        if (isCurrentUser) {
            lineContainer.setAlignment(Pos.CENTER_RIGHT);
            lblSenderName.setStyle("-fx-text-fill: #8ECAE6;");
            bubble.setStyle("-fx-background-color: #0D3B66; -fx-background-radius: 15 15 0 15; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");

            if (visualContent instanceof Label) ((Label) visualContent).setStyle("-fx-text-fill: white;");
            if (visualContent instanceof HBox) {
                visualContent.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 5;");
                ((HBox) visualContent).lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: white;"));
            }

            lblTime.setStyle("-fx-text-fill: #bdc3c7;");
            menuIcon.setStyle("-fx-text-fill: white;");

            Label lblStatus = new Label(msg.isLu() ? "✓✓" : "✓");
            lblStatus.setStyle(msg.isLu() ? "-fx-text-fill: #10A5A5; -fx-font-weight: bold;" : "-fx-text-fill: #bdc3c7;");
            footerPart.getChildren().add(lblStatus);
            lineContainer.getChildren().addAll(bubble, avatarView);
        } else {
            lineContainer.setAlignment(Pos.CENTER_LEFT);
            lblSenderName.setStyle("-fx-text-fill: #10A5A5;");
            bubble.setStyle("-fx-background-color: white; -fx-background-radius: 15 15 15 0; " +
                    "-fx-border-color: #E0E0E0; -fx-border-width: 0.5; -fx-border-radius: 15 15 15 0; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

            if (visualContent instanceof Label) ((Label) visualContent).setStyle("-fx-text-fill: #2c3e50;");
            if (visualContent instanceof HBox) {
                visualContent.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #eee; -fx-border-width: 1;");
                ((HBox) visualContent).lookupAll(".label").forEach(n -> n.setStyle("-fx-text-fill: #0D3B66;"));
            }

            lblTime.setStyle("-fx-text-fill: #7f8c8d;");
            msgMenu.setVisible(false);
            lineContainer.getChildren().addAll(avatarView, bubble);
        }

        bubble.getChildren().addAll(lblSenderName, topPart, footerPart);
        vboxMessages.getChildren().add(lineContainer);
    }
    private void handleSupprimerMessage(Message msg, HBox UIElement) {
        try {
            serMsg.deleteOne(msg);
            vboxMessages.getChildren().remove(UIElement);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void handleModifierMessage(Message msg, Label lblContent) {
        TextInputDialog dialog = new TextInputDialog(msg.getContenu());
        dialog.setTitle("Rehletna - Modification");
        dialog.setHeaderText("Modifier votre message");
        dialog.setContentText("Nouveau contenu :");
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
        dialog.showAndWait().ifPresent(newText -> {
            if (!newText.trim().isEmpty() && !newText.equals(msg.getContenu())) {
                try {
                    msg.setContenu(newText);
                    serMsg.updateOne(msg);
                    lblContent.setText(newText);
                    listConversations.refresh();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void chargerHistorique(int idConversation) {
        vboxMessages.getChildren().clear();
        try {
            List<Message> historique = serMsg.selectByConversation(idConversation);

            for (Message m : historique) {
                boolean isCurrentUser = (m.getExpediteur().getIdUtilisateur() == currentUserId);
                renderMessage(m, isCurrentUser);
            }
            scrollPaneMessages.setVvalue(1.0);

        } catch (SQLException e) {
            System.err.println("Erreur chargement messages : " + e.getMessage());
        }
    }
    //ListView
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
                    VBox container = new VBox(5);
                    container.setStyle("-fx-border-color: #f1f2f6; -fx-border-width: 0 0 1 0; -fx-background-color: transparent;");
                    container.setPadding(new Insets(12, 15, 12, 15));

                    HBox topRow = new HBox();
                    topRow.setAlignment(Pos.CENTER_LEFT);

                    Label lblName = new Label();
                    lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-size: 15px; -fx-font-weight: bold;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label lblTime = new Label();
                    lblTime.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");

                    topRow.getChildren().addAll(lblName, spacer, lblTime);

                    Label lblLastMsg = new Label();
                    lblLastMsg.setPrefWidth(220);
                    lblLastMsg.setEllipsisString("...");

                    container.getChildren().addAll(topRow, lblLastMsg);

                    try {
                        lblName.setText(serConv.getNomAffichage(conv, currentUserId));

                        Message last = serMsg.selectLastMessage(conv.getIdConversation());

                        if (last != null) {
                            lblLastMsg.setText(last.getContenu());
                            lblTime.setText(last.getDateEnvoi().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));

                            if (!last.isLu() && last.getExpediteur().getIdUtilisateur() != currentUserId) {
                                lblLastMsg.setStyle("-fx-text-fill: #2c3e50; -fx-font-weight: bold; -fx-font-size: 13px;");
                                lblTime.setStyle("-fx-text-fill: #10A5A5; -fx-font-weight: bold;");
                                lblName.setStyle("-fx-text-fill: #10A5A5; -fx-font-size: 15px; -fx-font-weight: bold;");
                            } else {
                                lblLastMsg.setStyle("-fx-text-fill: #7f8c8d; -fx-font-weight: normal; -fx-font-size: 13px;");
                                lblTime.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 11px;");
                                lblName.setStyle("-fx-text-fill: #0D3B66; -fx-font-size: 15px; -fx-font-weight: bold;");
                            }
                        } else {
                            lblLastMsg.setText("Aucun message");
                            lblLastMsg.setStyle("-fx-text-fill: #bdc3c7; -fx-font-italic: true;");
                        }

                    } catch (SQLException e) {
                        lblName.setText("Erreur");
                    }

                    setGraphic(container);
                }
            }
        });
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
            Message m = new Message();
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

        MenuItem itemSupprimer = new MenuItem(" Supprimer la discussion");
        itemSupprimer.setGraphic(new Label("🗑️"));
        itemSupprimer.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        itemSupprimer.setOnAction(e -> handleSupprimerConversation(conv));

        if (conv.getTypeConversation() == TypeConversation.GROUPE) {
            MenuItem itemModifier = new MenuItem(" Modifier le nom");
            itemModifier.setGraphic(new Label("✏️"));
            itemModifier.setOnAction(e -> handleModifierNomGroupe(conv));

            MenuItem itemVoirMembres = new MenuItem(" Voir les membres");
            itemVoirMembres.setGraphic(new Label("👥"));
            itemVoirMembres.setOnAction(e -> handleVoirMembres(conv));

            MenuItem itemAjouter = new MenuItem(" Ajouter un membre");
            itemAjouter.setGraphic(new Label("👤+"));
            itemAjouter.setOnAction(e -> handleAjouterMembre(conv));

            MenuItem itemQuitter = new MenuItem(" Quitter le groupe");
            itemQuitter.setGraphic(new Label("🚪"));
            itemQuitter.setStyle("-fx-text-fill: #f39c12;");
            itemQuitter.setOnAction(e -> handleQuitterGroupe(conv));
            btnOptions.getItems().addAll(itemModifier, itemVoirMembres, itemAjouter, itemQuitter, new SeparatorMenuItem(), itemSupprimer);
        } else {
            btnOptions.getItems().add(itemSupprimer);
        }
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

                Message m = new Message();
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
}