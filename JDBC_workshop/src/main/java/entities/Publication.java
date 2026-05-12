package entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class Publication implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Moderation status — set by the tagged agency in the back office. */
    public enum Status { PENDING, APPROVED, REJECTED }

    private int publicationID;
    private String content;
    private Date datePublication;
    private Client client;
    private List<Comment> comments;
    private List<Like> likes;
    private String imagePath;
    private String place;
    private String placeId;  // Nominatim place_id for future API calls (weather, maps, etc.)

    // ── Agency moderation fields ──────────────────────────────────────────────
    /** ID of the agency this post was submitted to (0 = no agency tagged). */
    private int agencyId;
    /** Current moderation status. Defaults to PENDING when agencyId is set. */
    private Status status = Status.APPROVED; // standalone posts auto-approved

    public Publication() {
        this.comments = new ArrayList<>();
        this.likes    = new ArrayList<>();
    }

    public Publication(Client client, int publicationID, String content,
                       Date datePublication, String imagePath, String place) {
        this.client          = client;
        this.publicationID   = publicationID;
        this.content         = content;
        this.datePublication = datePublication;
        this.imagePath       = imagePath;
        this.place           = place;
        this.comments        = new ArrayList<>();
        this.likes           = new ArrayList<>();
        this.status          = Status.APPROVED;
        this.agencyId        = 0;
    }

    /** Constructor used when user tags an agency — starts PENDING. */
    public Publication(Client client, int publicationID, String content,
                       Date datePublication, String imagePath, String place, int agencyId) {
        this(client, publicationID, content, datePublication, imagePath, place);
        this.agencyId = agencyId;
        this.status   = agencyId > 0 ? Status.PENDING : Status.APPROVED;
    }

    // Getters
    public int    getPublicationID()   { return publicationID; }
    public String getContent()         { return content; }
    public Date   getDatePublication() { return datePublication; }
    public Client getClient()          { return client; }
    public String getImagePath()       { return imagePath; }
    public String getPlace()           { return place; }
    public String getPlaceId()         { return placeId; }
    public int    getAgencyId()        { return agencyId; }
    public Status getStatus()          { return status; }

    /** Returns a defensive copy — mutate via addComment/removeComment. */
    public List<Comment> getComments() { return new ArrayList<>(comments); }

    /** Returns a defensive copy — mutate via addLike/removeLike. */
    public List<Like> getLikes()       { return new ArrayList<>(likes); }

    // Setters
    public void setPublicationID(int publicationID) { this.publicationID = publicationID; }

    public void setContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        this.content = content;
    }

    public void setDatePublication(Date datePublication) { this.datePublication = datePublication; }
    public void setClient(Client client)                 { this.client = client; }
    public void setImagePath(String imagePath)           { this.imagePath = imagePath; }
    public void setPlace(String place)                   { this.place = place; }
    public void setPlaceId(String placeId)               { this.placeId = placeId; }
    public void setStatus(Status status)                 { this.status = status; }

    public void setComments(List<Comment> comments) {
        this.comments = comments != null ? new ArrayList<>(comments) : new ArrayList<>();
    }

    public void setLikes(List<Like> likes) {
        this.likes = likes != null ? new ArrayList<>(likes) : new ArrayList<>();
    }

    public void setAgencyId(int agencyId) {
        this.agencyId = agencyId;
        if (agencyId > 0 && this.status == Status.APPROVED) this.status = Status.PENDING;
    }

    // Moderation helpers
    public boolean isPending()  { return status == Status.PENDING; }
    public boolean isApproved() { return status == Status.APPROVED; }
    public boolean isRejected() { return status == Status.REJECTED; }
    public boolean hasAgency()  { return agencyId > 0; }

    // Comment / Like mutation
    public void addComment(Comment comment)    { if (comment != null) comments.add(comment); }
    public void removeComment(Comment comment) { comments.remove(comment); }
    public void addLike(Like like)             { if (like != null) likes.add(like); }
    public void removeLike(Like like)          { likes.remove(like); }

    // Counts
    public int getCommentsCount() { return comments.size(); }
    public int getLikesCount()    { return likes.size(); }

    // Predicates
    public boolean hasImage() { return imagePath != null && !imagePath.trim().isEmpty(); }
    public boolean hasPlace() { return place     != null && !place.trim().isEmpty(); }

    public boolean isOwnedBy(Client client) {
        return this.client != null && client != null &&
                this.client.getClientID() == client.getClientID();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Publication)) return false;
        Publication that = (Publication) o;
        return publicationID == that.publicationID;
    }

    @Override
    public int hashCode() { return Objects.hash(publicationID); }

    @Override
    public String toString() {
        return "Publication{" +
                "publicationID=" + publicationID +
                ", content='" + content + '\'' +
                ", datePublication=" + datePublication +
                ", client=" + (client != null ? client.getClientID() : "null") +
                ", commentsCount=" + getCommentsCount() +
                ", likesCount=" + getLikesCount() +
                ", hasImage=" + hasImage() +
                ", place='" + place + '\'' +
                '}';
    }
}