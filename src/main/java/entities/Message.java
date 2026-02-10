package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {

    private int idMessage;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private boolean lu;
    private Conversation conversation;


    public Message() {
    }

    public Message(int idMessage, String contenu, LocalDateTime dateEnvoi, boolean lu, int idConversation) {
        this.idMessage = idMessage;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.lu = lu;
        this.conversation = conversation;
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

    public Conversation getIdConversation() {
        return conversation;
    }

    public void setIdConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    @Override
    public String toString() {
        return "Message{" +
                "idMessage=" + idMessage +
                ", contenu='" + contenu + '\'' +
                ", dateEnvoi=" + dateEnvoi +
                ", lu=" + lu +
                ", conversation=" + conversation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return idMessage == message.idMessage && lu == message.lu && conversation == message.conversation && Objects.equals(contenu, message.contenu) && Objects.equals(dateEnvoi, message.dateEnvoi);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMessage, contenu, dateEnvoi, lu, conversation);
    }
}
