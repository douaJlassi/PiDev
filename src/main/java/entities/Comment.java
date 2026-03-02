package entities;

import java.util.Date;
import java.util.Objects;

public class Comment {
    private int commentID;
    private int publicationID;
    private String content;
    private Date commentDate;
    private Client client;
    private Publication publication;

    // Default constructor
    public Comment() {}

    // Full constructor
    public Comment(Client client, Publication publication, int commentID, int publicationID, String content, Date commentDate) {
        this.commentID = commentID;
        this.publicationID = publicationID;
        this.content = content;
        this.commentDate = commentDate;
        this.client = client;
        this.publication = publication;
    }

    // Constructor for new comment (without ID)
    public Comment(Client client, Publication publication, String content, Date commentDate) {
        this.client = client;
        this.publication = publication;
        this.publicationID = publication != null ? publication.getPublicationID() : 0;
        this.content = content;
        this.commentDate = commentDate;
    }

    // Getters
    public int getCommentID() {
        return commentID;
    }

    public int getPublicationID() {
        return publicationID;
    }

    public String getContent() {
        return content;
    }

    public Date getCommentDate() {
        return commentDate;
    }

    public Client getClient() {
        return client;
    }

    public Publication getPublication() {
        return publication;
    }

    // Setters
    public void setCommentID(int commentID) {
        this.commentID = commentID;
    }

    public void setPublicationID(int publicationID) {
        this.publicationID = publicationID;
    }

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment content cannot be null or empty");
        }
        this.content = content;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
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

    public String getTimeAgo() {
        if (commentDate == null) return "";

        long diff = new Date().getTime() - commentDate.getTime();
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (hours > 0) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else {
            return "Just now";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Comment)) return false;
        Comment comment = (Comment) o;
        return commentID == comment.commentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commentID);
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentID=" + commentID +
                ", publicationID=" + publicationID +
                ", content='" + content + '\'' +
                ", commentDate=" + commentDate +
                ", client=" + (client != null ? client.getClientID() : "null") +
                '}';
    }
}