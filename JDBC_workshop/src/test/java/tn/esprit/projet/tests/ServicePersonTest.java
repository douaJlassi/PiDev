package tests;

import org.junit.jupiter.api.*;
import entities.Person;
import services.PersonService;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServicePersonTest {

    static PersonService sp;
    static Person testUser;
    static int testUserId;

    @BeforeAll
    static void setUp() {
        sp = new PersonService();
        System.out.println("=== Starting Person Service Tests ===");
    }

    @BeforeEach
    void init() {
        System.out.println("Running test...");
    }

    @AfterAll
    static void tearDown() {
        System.out.println("=== All tests completed ===");
    }

    @Test
    @Order(1)
    void testAjout() {
        System.out.println("Test 1: Adding new user");

        // Create a unique test user
        String uniqueId = String.valueOf(System.currentTimeMillis()).substring(7);
        testUser = new Person(
                0,
                "TestName" + uniqueId,
                "TestLastName" + uniqueId,
                "test" + uniqueId + "@test.com",
                "password123",
                Date.valueOf("2000-01-01"),
                "User",
                "testuser" + uniqueId
        );

        try {
            sp.insertOneUpdated(testUser);

            // Get the user back to verify
            Person createdUser = sp.login(testUser.getEmail(), testUser.getPassword());
            assertNotNull(createdUser, "User should be created successfully");

            if (createdUser != null) {
                testUserId = createdUser.getId();
                testUser.setId(testUserId);
                System.out.println("✓ User added successfully with ID: " + testUserId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while adding user: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    void testSelectAll() {
        System.out.println("Test 2: Selecting all users");

        try {
            List<Person> userList = sp.selectALL();
            assertNotNull(userList, "User list should not be null");
            assertTrue(userList.size() > 0, "User list should contain at least one user");

            System.out.println("✓ Retrieved " + userList.size() + " users from database");

            // Verify our test user is in the list
            if (testUserId > 0) {
                boolean found = userList.stream().anyMatch(u -> u.getId() == testUserId);
                assertTrue(found, "Test user should be in the list");
                System.out.println("✓ Test user found in list");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while selecting users: " + e.getMessage());
        }
    }

    @Test
    @Order(3)
    void testGetUserById() {
        System.out.println("Test 3: Getting user by ID");

        try {
            Person user = sp.getUserById(testUserId);
            assertNotNull(user, "User should be found by ID");
            assertEquals(testUser.getUsername(), user.getUsername(), "Username should match");
            assertEquals(testUser.getEmail(), user.getEmail(), "Email should match");

            System.out.println("✓ Retrieved user by ID: " + user.getUsername());
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while getting user by ID: " + e.getMessage());
        }
    }

    @Test
    @Order(4)
    void testLogin() {
        System.out.println("Test 4: Testing login functionality");

        try {
            // Test with correct credentials
            Person loggedIn = sp.login(testUser.getEmail(), testUser.getPassword());
            assertNotNull(loggedIn, "Login should succeed with correct credentials");
            assertEquals(testUser.getUsername(), loggedIn.getUsername(), "Username should match");

            // Test with wrong password
            Person wrongPassword = sp.login(testUser.getEmail(), "wrongpassword");
            assertNull(wrongPassword, "Login should fail with wrong password");

            // Test with wrong email
            Person wrongEmail = sp.login("nonexistent@test.com", testUser.getPassword());
            assertNull(wrongEmail, "Login should fail with wrong email");

            System.out.println("✓ Login tests passed");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while testing login: " + e.getMessage());
        }
    }

    @Test
    @Order(5)
    void testUpdateUser() {
        System.out.println("Test 5: Updating user");

        try {
            // Update user information
            testUser.setName("UpdatedName");
            testUser.setLastName("UpdatedLastName");
            testUser.setEmail("updated" + testUser.getEmail());

            sp.updateOne(testUser);

            // Verify update
            Person updatedUser = sp.getUserById(testUserId);
            assertNotNull(updatedUser, "Updated user should exist");
            assertEquals("UpdatedName", updatedUser.getName(), "Name should be updated");
            assertEquals("UpdatedLastName", updatedUser.getLastName(), "Last name should be updated");

            System.out.println("✓ User updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while updating user: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    void testUpdateUserStatus() {
        System.out.println("Test 6: Updating user status");

        try {
            // Update status to online
            sp.updateUserStatus(testUserId, "online");
            Person user = sp.getUserById(testUserId);
            assertEquals("online", user.getStatus(), "Status should be online");

            // Update status to offline
            sp.updateUserStatus(testUserId, "offline");
            user = sp.getUserById(testUserId);
            assertEquals("offline", user.getStatus(), "Status should be offline");

            System.out.println("✓ User status updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while updating user status: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    void testGetOnlineUsers() {
        System.out.println("Test 7: Getting online users");

        try {
            // Set test user to online
            sp.updateUserStatus(testUserId, "online");

            List<Person> onlineUsers = sp.getOnlineUsers();
            assertNotNull(onlineUsers, "Online users list should not be null");

            boolean found = onlineUsers.stream().anyMatch(u -> u.getId() == testUserId);
            assertTrue(found, "Test user should be in online list");

            System.out.println("✓ Found " + onlineUsers.size() + " online users");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while getting online users: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    void testGetOfflineUsers() {
        System.out.println("Test 8: Getting offline users");

        try {
            // Set test user to offline
            sp.updateUserStatus(testUserId, "offline");

            List<Person> offlineUsers = sp.getOfflineUsers();
            assertNotNull(offlineUsers, "Offline users list should not be null");

            boolean found = offlineUsers.stream().anyMatch(u -> u.getId() == testUserId);
            assertTrue(found, "Test user should be in offline list");

            System.out.println("✓ Found " + offlineUsers.size() + " offline users");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while getting offline users: " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    void testGetAdminUsers() {
        System.out.println("Test 9: Getting admin users");

        try {
            // Update test user to admin for testing
            testUser.setRole("Admin");
            sp.updateOne(testUser);

            List<Person> adminUsers = sp.getAdminUsers();
            assertNotNull(adminUsers, "Admin users list should not be null");

            boolean found = adminUsers.stream().anyMatch(u -> u.getId() == testUserId);
            assertTrue(found, "Test user should be in admin list");

            System.out.println("✓ Found " + adminUsers.size() + " admin users");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while getting admin users: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    void testDeleteUser() {
        System.out.println("Test 10: Deleting user");

        try {
            sp.deleteOne(testUser);

            // Verify deletion
            Person deletedUser = sp.getUserById(testUserId);
            assertNull(deletedUser, "User should be deleted");

            System.out.println("✓ User deleted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            fail("Exception occurred while deleting user: " + e.getMessage());
        }
    }
}