package com.task.taskreminder.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String priority;
    private String status;
    private LocalDate date;
    private LocalDateTime completedAt;
    private LocalTime reminderTime;

    
    

    @Column(nullable = false)
private boolean reminderSent = false;
private Integer reminderInterval;
private LocalDateTime lastReminderTime;



    @ManyToOne
@JoinColumn(name = "user_id")
private User user;
@Transient
private int serialNo;

public int getSerialNo() {
    return serialNo;
}

public void setSerialNo(int serialNo) {
    this.serialNo = serialNo;
}



    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isReminderSent() {
    return reminderSent;
}

public void setReminderSent(boolean reminderSent) {
    this.reminderSent = reminderSent;
}

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public User getUser() {
    return user;
}

public void setUser(User user) {
    this.user = user;
}

public Integer getReminderInterval() {
    return reminderInterval;
}

public void setReminderInterval(Integer reminderInterval) {
    this.reminderInterval = reminderInterval;
}
public LocalDateTime getLastReminderTime() {
    return lastReminderTime;
}
public void setLastReminderTime(LocalDateTime lastReminderTime) {
    this.lastReminderTime = lastReminderTime;
}

public LocalDateTime getCompletedAt() {
    return completedAt;
}

public void setCompletedAt(LocalDateTime completedAt) {
    this.completedAt = completedAt;
}
public LocalDateTime getLastReminderSentAt() {
    return lastReminderTime;
}
public void setLastReminderSentAt(LocalDateTime lastReminderSentAt) {
    this.lastReminderTime = lastReminderSentAt;
}
public LocalTime getReminderTime() {
    return reminderTime;        
}
public void setReminderTime(LocalTime reminderTime) {
    this.reminderTime = reminderTime;
}

}
