//package app;
//
//public final class Session {
//    private Session() {}
//
//    private static Integer userId;
//    private static String role;
//
//    public static void loginAs(int id, String r) {
//        userId = id;
//        role = r;
//    }
//
//    public static int getUserId() {
//        if (userId == null) throw new IllegalStateException("No user logged in");
//        return userId;
//    }
//
//    public static String getRole() { return role; }
//
//    public static boolean isAgency() {
//        return role != null && role.equalsIgnoreCase("AGENCE");
//    }
//    public static boolean isAdmin() {
//        return "ADMIN".equalsIgnoreCase(role);
//    }
//    public static boolean isClient() {
//        return "CLIENT".equalsIgnoreCase(role);
//    }
//
//
//}
package app;

public final class Session {
    private Session() {}

    private static Integer userId;
    private static String role;

    private static String fullName;
    private static String email;
    private static String username;

    public static void loginAs(int id, String r) {
        userId = id;
        role = normalizeRole(r);
    }

    // ✅ integration-friendly: apply from Person object (other module calls this)
    public static void loginPerson(int id, String firstName, String lastName, String emailAddr, String user, String r) {
        userId = id;
        role = normalizeRole(r);
        fullName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        email = emailAddr;
        username = user;
    }

    private static String normalizeRole(String r) {
        if (r == null) return "CLIENT";
        String x = r.trim().toUpperCase();

        if (x.contains("ADMIN")) return "ADMIN";
        if (x.contains("AGENCE") || x.contains("AGENCY")) return "AGENCE";
        if (x.contains("CLIENT") || x.contains("VOYAGEUR") || x.contains("TRAVEL")) return "CLIENT";

        return "CLIENT";
    }

    public static int getUserId() {
        if (userId == null) throw new IllegalStateException("No user logged in");
        return userId;
    }

    public static String getRole() { return role; }

    public static String getFullName() { return fullName; }
    public static String getEmail() { return email; }
    public static String getUsername() { return username; }

    public static boolean isAgency() {
        return "AGENCE".equalsIgnoreCase(role);
    }
    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
    public static boolean isClient() {
        return "CLIENT".equalsIgnoreCase(role);
    }
}
