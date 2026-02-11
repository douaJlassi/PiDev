package entities;

import java.util.Date;

public class Comment {
    private int commentID;
    private int publicationID;
    private String content;
    private Date commentDate;
    private Client client;
    private Publication publication;


    public Comment(Client client, Publication publication, int commentID, int publicationID, String content, Date commentDate) {
        this.commentID = commentID;
        this.publicationID = publicationID;
        this.content = content;
        this.commentDate = commentDate;
        this.client = client;
        this.publication = publication;

    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public int getCommentID() {
        return commentID;
    }

    public void setCommentID(int commentID) {
        this.commentID = commentID;
    }

    public int getPublicationID() {
        return publicationID;
    }

    public void setPublicationID(int publicationID) {
        this.publicationID = publicationID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }
}
