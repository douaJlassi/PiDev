package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Conversation {

    private int idConversation;
    private TypeConversation typeConversation;
    private LocalDateTime dateCreation;

    public Conversation() {}

    public Conversation(int idConversation, TypeConversation typeConversation, LocalDateTime dateCreation) {
        this.idConversation = idConversation;
        this.typeConversation = typeConversation;
        this.dateCreation = dateCreation;
    }

    public int getIdConversation() {
        return idConversation;
    }

    public void setIdConversation(int idConversation) {
        this.idConversation = idConversation;
    }

    public TypeConversation getTypeConversation() {
        return typeConversation;
    }

    public void setTypeConversation(TypeConversation typeConversation) {
        this.typeConversation = typeConversation;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    @Override
    public String toString() {
        return "Conversation{" +
                "idConversation='" + idConversation + '\'' +
                ", typeConversation=" + typeConversation +
                ", dateCreation=" + dateCreation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Conversation that)) return false;
        return Objects.equals(idConversation, that.idConversation) && typeConversation == that.typeConversation && Objects.equals(dateCreation, that.dateCreation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idConversation, typeConversation, dateCreation);
    }
}
