package entities;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Todo implements Serializable {
    private int id;
    private String title;
    private String description;
    private String status; // "To Do", "In Progress", "Done"
    private int userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int priority; // 1 (High), 2 (Medium), 3 (Low)
    private String category;

    public Todo() {}

    public Todo(int id, String title, String description, String status, int userId,
                LocalDateTime createdAt, LocalDateTime updatedAt, int priority, String category) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.priority = priority;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriorityText() {
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Medium";
        }
    }

    public String getPriorityColor() {
        switch (priority) {
            case 1: return "#ff5e62";
            case 2: return "#FEC74C";
            case 3: return "#0FA5A2";
            default: return "#FEC74C";
        }
    }
}