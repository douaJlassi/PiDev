package entities;

public class Client {
    private int clientID;
    private String username;
    private String avatarPath; // URL or local path

    public Client() {}

    public Client(int clientID, String username, String avatarPath) {
        this.clientID = clientID;
        this.username = username;
        this.avatarPath = avatarPath;
    }

    public int getClientID() { return clientID; }
    public void setClientID(int clientID) { this.clientID = clientID; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
}