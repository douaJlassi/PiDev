package entities;

public class Like {
    private int likeID;
    private int publicationID;
    private int commentID;
    private Client client;
    private Publication publication;

    public Like(Client client, Publication publication, int likeID, int publicationID, int commentID) {
        this.likeID = likeID;
        this.publicationID = publicationID;
        this.commentID = commentID;
        this.client = client;
        this.publication = publication;
    }
    public int getLikeID() {
        return likeID;
    }
    public void setLikeID(int likeID) {
        this.likeID = likeID;
    }
    public int getPublicationID() {
        return publicationID;
    }
    public void setPublicationID(int publicationID) {
        this.publicationID = publicationID;
    }
    public int getCommentID() {
        return commentID;
    }
    public void setCommentID(int commentID) {
        this.commentID = commentID;
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
}
