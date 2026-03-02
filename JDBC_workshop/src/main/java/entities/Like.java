package entities;

import java.util.Objects;

public class Like {
    private int likeID;
    private int publicationID;
    private int commentID;
    private Client client;
    private Publication publication;

    // Default constructor
    public Like() {}

    // Full constructor
    public Like(Client client, Publication publication, int likeID, int publicationID, int commentID) {
        this.likeID = likeID;
        this.publicationID = publicationID;
        this.commentID = commentID;
        this.client = client;
        this.publication = publication;
    }

    // Constructor for new like (without ID)
    public Like(Client client, Publication publication) {
        this.client = client;
        this.publication = publication;
        this.publicationID = publication != null ? publication.getPublicationID() : 0;
        this.commentID = 0;
    }

    // Getters
    public int getLikeID() {
        return likeID;
    }

    public int getPublicationID() {
        return publicationID;
    }

    public int getCommentID() {
        return commentID;
    }

    public Client getClient() {
        return client;
    }

    public Publication getPublication() {
        return publication;
    }

    // Setters
    public void setLikeID(int likeID) {
        this.likeID = likeID;
    }

    public void setPublicationID(int publicationID) {
        this.publicationID = publicationID;
    }

    public void setCommentID(int commentID) {
        this.commentID = commentID;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
        if (publication != null) {
            this.publicationID = publication.getPublicationID();
        }
    }

    // Utility methods
    public boolean isOwnedBy(Client client) {
        return this.client != null && client != null &&
                this.client.getClientID() == client.getClientID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Like)) return false;
        Like like = (Like) o;
        return likeID == like.likeID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(likeID);
    }

    @Override
    public String toString() {
        return "Like{" +
                "likeID=" + likeID +
                ", publicationID=" + publicationID +
                ", client=" + (client != null ? client.getClientID() : "null") +
                '}';
    }
}