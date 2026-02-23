package app;

public final class Session {
    private Session() {}

    private static Integer userId;
    private static String role;

    public static void loginAs(int id, String r) {
        userId = id;
        role = r;
    }

    public static int getUserId() {
        if (userId == null) throw new IllegalStateException("No user logged in");
        return userId;
    }

    public static String getRole() { return role; }

    public static boolean isAgency() {
        return role != null && role.equalsIgnoreCase("AGENCE");
    }
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
    public static boolean isClient() {
        return "CLIENT".equalsIgnoreCase(role);
    }


}
