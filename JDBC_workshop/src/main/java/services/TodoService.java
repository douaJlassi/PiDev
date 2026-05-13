package services;

import entities.Todo;
import utils.MyDBConnexion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TodoService {

    private Connection cnx;

    public TodoService() {
        cnx = MyDBConnexion.getInstance().getCnx();
    }

    /**
     * Create todo table if not exists
     */
    public void createTable() throws SQLException {
        String req = "CREATE TABLE IF NOT EXISTS todo (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "title VARCHAR(255) NOT NULL, " +
                "description TEXT, " +
                "status VARCHAR(50) DEFAULT 'To Do', " +
                "user_id INT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "priority INT DEFAULT 2, " +
                "category VARCHAR(100), " +
                "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ")";
        try (Statement st = cnx.createStatement()) {
            st.execute(req);
            System.out.println("✅ Todo table created/verified");
        }
    }

    /**
     * Add a new todo
     */
    public void addTodo(Todo todo) throws SQLException {
        String req = "INSERT INTO todo (title, description, status, user_id, created_at, updated_at, priority, category) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, todo.getTitle());
            ps.setString(2, todo.getDescription());
            ps.setString(3, todo.getStatus());
            ps.setInt(4, todo.getUserId());
            ps.setTimestamp(5, Timestamp.valueOf(todo.getCreatedAt() != null ? todo.getCreatedAt() : LocalDateTime.now()));
            ps.setTimestamp(6, Timestamp.valueOf(todo.getUpdatedAt() != null ? todo.getUpdatedAt() : LocalDateTime.now()));
            ps.setInt(7, todo.getPriority());
            ps.setString(8, todo.getCategory());

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    todo.setId(rs.getInt(1));
                }
            }
            System.out.println("✅ Todo added: " + todo.getTitle());
        }
    }

    /**
     * Update a todo
     */
    public void updateTodo(Todo todo) throws SQLException {
        String req = "UPDATE todo SET title = ?, description = ?, status = ?, priority = ?, category = ?, updated_at = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, todo.getTitle());
            ps.setString(2, todo.getDescription());
            ps.setString(3, todo.getStatus());
            ps.setInt(4, todo.getPriority());
            ps.setString(5, todo.getCategory());
            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(7, todo.getId());

            ps.executeUpdate();
            System.out.println("✅ Todo updated: " + todo.getTitle());
        }
    }

    /**
     * Delete a todo
     */
    public void deleteTodo(int todoId) throws SQLException {
        String req = "DELETE FROM todo WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, todoId);
            ps.executeUpdate();
            System.out.println("✅ Todo deleted: ID " + todoId);
        }
    }

    /**
     * Get todos by user ID
     */
    public List<Todo> getTodosByUserId(int userId) throws SQLException {
        List<Todo> todos = new ArrayList<>();
        String req = "SELECT * FROM todo WHERE user_id = ? ORDER BY priority, created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Todo todo = new Todo();
                todo.setId(rs.getInt("id"));
                todo.setTitle(rs.getString("title"));
                todo.setDescription(rs.getString("description"));
                todo.setStatus(rs.getString("status"));
                todo.setUserId(rs.getInt("user_id"));
                todo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                todo.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                        rs.getTimestamp("updated_at").toLocalDateTime() : null);
                todo.setPriority(rs.getInt("priority"));
                todo.setCategory(rs.getString("category"));
                todos.add(todo);
            }
        }
        return todos;
    }

    /**
     * Get todos by status
     */
    public List<Todo> getTodosByStatus(String status) throws SQLException {
        List<Todo> todos = new ArrayList<>();
        String req = "SELECT * FROM todo WHERE status = ? ORDER BY priority, created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Todo todo = new Todo();
                todo.setId(rs.getInt("id"));
                todo.setTitle(rs.getString("title"));
                todo.setDescription(rs.getString("description"));
                todo.setStatus(rs.getString("status"));
                todo.setUserId(rs.getInt("user_id"));
                todo.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                todo.setUpdatedAt(rs.getTimestamp("updated_at") != null ?
                        rs.getTimestamp("updated_at").toLocalDateTime() : null);
                todo.setPriority(rs.getInt("priority"));
                todo.setCategory(rs.getString("category"));
                todos.add(todo);
            }
        }
        return todos;
    }

    /**
     * Move todo to different status
     */
    public void moveTodo(int todoId, String newStatus) throws SQLException {
        String req = "UPDATE todo SET status = ?, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(req)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, todoId);
            ps.executeUpdate();
            System.out.println("✅ Todo moved to " + newStatus + ": ID " + todoId);
        }
    }
}