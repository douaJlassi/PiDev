package entities;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Publication {
    private int publicationID;
    private String content;
    private Date datePublication;
    private Client client;
    private List<Comment> comments = new ArrayList<>();
    private List<Like> likes = new ArrayList<>();

    public Publication(Client client,int publicationID, String content, Date datePublication) {
        this.publicationID = publicationID;
        this.content = content;
        this.datePublication = datePublication;
        this.client = client;
    }

    public int getPublicationID() {
        return publicationID;
    }

    public String getContent() {
        return content;
    }

    public Date getDatePublication() {
        return datePublication;
    }

    public void setPublicationID(int publicationID) {
        this.publicationID = publicationID;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setDatePublication(Date datePublication) {
        this.datePublication = datePublication;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Like> getLikes() {
        return likes;
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes;
    }

    @Override
    public String toString() {
        return "Publication{" +
                "publicationID=" + publicationID +
                ", content='" + content + '\'' +
                ", datePublication=" + datePublication +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Publication that)) return false;
        return publicationID == that.publicationID && Objects.equals(content, that.content) && Objects.equals(datePublication, that.datePublication);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationID, content, datePublication);
    }
}
