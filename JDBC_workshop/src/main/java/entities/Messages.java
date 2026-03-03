package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Messages {

    private int idMessage;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private boolean lu;
    private Conversation conversation;
    private Utilisateur expediteur;
    private TypeMessage typeMessage;
    private String urlFichier;
    private String reaction;
    private boolean isDeleted;

    public Messages() {
    }

    public Messages(int idMessage, String contenu, LocalDateTime dateEnvoi, boolean lu, Conversation conversation, Utilisateur utilisateur, TypeMessage typeMessage, String urlFichier, String reaction, boolean isDeleted) {
        this.idMessage = idMessage;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.lu = lu;
        this.conversation = conversation;
        this.expediteur = utilisateur;
        this.typeMessage = typeMessage;
        this.urlFichier = urlFichier;
        this.reaction = reaction;
        this.isDeleted = isDeleted;
    }

    public int getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(int idMessage) {
        this.idMessage = idMessage;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public boolean isLu() {
        return lu;
    }

    public void setLu(boolean lu) {
        this.lu = lu;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Utilisateur getExpediteur() {
        return expediteur;
    }
    public void setExpediteur(Utilisateur expediteur) {
        this.expediteur = expediteur;
    }

    public TypeMessage getTypeMessage() {
        return typeMessage;
    }

    public void setTypeMessage(TypeMessage typeMessage) {
        this.typeMessage = typeMessage;
    }

    public String getUrlFichier() {
        return urlFichier;
    }

    public void setUrlFichier(String urlFichier) {
        this.urlFichier = urlFichier;
    }

    public String getReaction() {
        return reaction;
    }

    public void setReaction(String reaction) {
        this.reaction = reaction;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    @Override
    public String toString() {
        return "Message{" +
                "idMessage=" + idMessage +
                ", contenu='" + contenu + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                ", lu=" + lu +
                ", conversation=" + conversation +
                ", expediteur=" + expediteur +
                ", typeMessage=" + typeMessage +
                ", urlFichier='" + urlFichier + '\'' +
                ", reaction='" + reaction + '\'' +
                ", isDeleted=" + isDeleted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Messages message)) return false;
        return idMessage == message.idMessage
                && lu == message.lu
                && Objects.equals(contenu, message.contenu)
                && Objects.equals(dateEnvoi, message.dateEnvoi)
                && Objects.equals(conversation, message.conversation)
                && Objects.equals(expediteur, message.expediteur)
                && typeMessage == message.typeMessage
                && Objects.equals(urlFichier, message.urlFichier)
                && Objects.equals(reaction, message.reaction)
                && isDeleted == message.isDeleted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMessage, contenu, dateEnvoi, lu, conversation, expediteur,  typeMessage, urlFichier,  reaction, isDeleted);
    }
}
