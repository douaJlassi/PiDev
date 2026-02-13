package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class ParticipantConversation {

    private int idParticipant;
    private Utilisateur participant;
    private Conversation conversation;
    private LocalDateTime dateAjout;

    public ParticipantConversation() {}

    public ParticipantConversation(int idParticipant, Utilisateur participant, Conversation conversation, LocalDateTime dateAjout) {
        this.idParticipant = idParticipant;
        this.participant = participant;
        this.conversation = conversation;
        this.dateAjout = dateAjout;
    }
    public ParticipantConversation(Utilisateur participant, Conversation conversation, LocalDateTime dateAjout) {
        this.idParticipant = idParticipant;
        this.participant = participant;
        this.conversation = conversation;
        this.dateAjout = dateAjout;
    }

    public int getIdParticipant() {
        return idParticipant;
    }

    public void setIdParticipant(int idParticipant) {
        this.idParticipant = idParticipant;
    }

    public Utilisateur getParticipant() {
        return participant;
    }

    public void setParticipant(Utilisateur participant) {
        this.participant = participant;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public LocalDateTime getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(LocalDateTime dateAjout) {
        this.dateAjout = dateAjout;
    }

    @Override
    public String toString() {
        return "ParticipantConversation{" +
                "participant=" + participant +
                ", conversation=" + conversation +
                ", dateAjout=" + dateAjout +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParticipantConversation that)) return false;
        return Objects.equals(participant, that.participant) && Objects.equals(conversation, that.conversation) && Objects.equals(dateAjout, that.dateAjout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participant, conversation, dateAjout);
    }
}
