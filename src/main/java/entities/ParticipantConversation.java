package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class ParticipantConversation {

    private Utilisateur participant;
    private Message message;
    private LocalDateTime dateAjout;

    public ParticipantConversation() {}

    public ParticipantConversation(Utilisateur participant, Message message, LocalDateTime dateAjout) {
        this.participant = participant;
        this.message = message;
        this.dateAjout = dateAjout;
    }

    public Utilisateur getParticipant() {
        return participant;
    }

    public void setParticipant(Utilisateur participant) {
        this.participant = participant;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
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
                ", message=" + message +
                ", dateAjout=" + dateAjout +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParticipantConversation that)) return false;
        return Objects.equals(participant, that.participant) && Objects.equals(message, that.message) && Objects.equals(dateAjout, that.dateAjout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(participant, message, dateAjout);
    }
}
