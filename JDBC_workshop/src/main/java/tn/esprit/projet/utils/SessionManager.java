package tn.esprit.projet.utils;

import tn.esprit.projet.entities.Person;
import java.io.*;
import java.time.LocalDateTime;
import java.util.prefs.Preferences;

public class SessionManager {

    private static Person currentUser = null;
    private static final String SESSION_FILE = "session.dat";

    // Save session to file
    public static void createSession(Person user) {
        currentUser = user;
        saveSessionToFile();
        System.out.println("Session created for user: " + user.getUsername());
    }

    // Get current session user
    public static Person getCurrentUser() {
        if (currentUser == null) {
            loadSessionFromFile();
        }
        return currentUser;
    }

    // Clear session
    public static void clearSession() {
        System.out.println("Clearing session...");
        currentUser = null;
        boolean deleted = deleteSessionFile();

        // Also clear any saved preferences if needed
        Preferences prefs = Preferences.userRoot().node(SessionManager.class.getName());
        prefs.remove("session_user_id");

        if (deleted) {
            System.out.println("Session file deleted successfully");
        } else {
            System.out.println("Session file was not found or could not be deleted");
        }
    }

    // Check if user is logged in
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    // Save session to file
    private static void saveSessionToFile() {
        if (currentUser == null) return;

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_FILE))) {
            oos.writeObject(currentUser);
            oos.writeObject(LocalDateTime.now());
            System.out.println("Session saved to file: " + SESSION_FILE);

            // Also save user ID in preferences as backup
            Preferences prefs = Preferences.userRoot().node(SessionManager.class.getName());
            prefs.putInt("session_user_id", currentUser.getId());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load session from file
    private static void loadSessionFromFile() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) {
            System.out.println("No session file found");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Person user = (Person) ois.readObject();
            LocalDateTime sessionTime = (LocalDateTime) ois.readObject();

            // Check if session is still valid (24 hours)
            LocalDateTime now = LocalDateTime.now();
            long hoursElapsed = java.time.Duration.between(sessionTime, now).toHours();

            if (hoursElapsed < 24) {
                currentUser = user;
                System.out.println("Session loaded for user: " + user.getUsername() + " (age: " + hoursElapsed + " hours)");
            } else {
                // Session expired, delete file
                System.out.println("Session expired (age: " + hoursElapsed + " hours) - deleting file");
                file.delete();
            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            // If error reading session, delete corrupted file
            System.out.println("Error reading session file - deleting corrupted file");
            file.delete();
        }
    }

    private static boolean deleteSessionFile() {
        File file = new File(SESSION_FILE);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("Session file deleted: " + SESSION_FILE);
            } else {
                System.out.println("Failed to delete session file: " + SESSION_FILE);
            }
            return deleted;
        } else {
            System.out.println("Session file does not exist: " + SESSION_FILE);
            return false;
        }
    }
}