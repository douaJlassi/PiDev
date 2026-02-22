package tn.esprit.projet.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class UserStatusManager {

    private static final Set<Integer> onlineUsers = Collections.synchronizedSet(new HashSet<>());

    public static void userLoggedIn(int userId) {
        onlineUsers.add(userId);
    }

    public static void userLoggedOut(int userId) {
        onlineUsers.remove(userId);
    }

    public static boolean isUserOnline(int userId) {
        return onlineUsers.contains(userId);
    }

    public static Set<Integer> getOnlineUsers() {
        return new HashSet<>(onlineUsers);
    }

    public static int getOnlineCount() {
        return onlineUsers.size();
    }

    public static void clearAllUsers() {
        onlineUsers.clear();
    }
}