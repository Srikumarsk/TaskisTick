package com.task.taskreminder.scheduler;

import com.task.taskreminder.model.Task;
import com.task.taskreminder.repository.TaskRepository;
import com.task.taskreminder.service.EmailService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public ReminderScheduler(TaskRepository taskRepository,
                             EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;

        System.out.println("✅ ReminderScheduler Started");
    }

    // =====================================================
    // 🌅 DAILY MORNING REMINDER (8 AM)
    // =====================================================
    @Scheduled(cron = "0 15 10 * * ?", zone = "Asia/Kolkata")
    public void sendMorningReminders() {

        System.out.println("🌅 Sending Daily Morning Reminders");

        List<Task> tasks = taskRepository.findAll();

        for (Task task : tasks) {

            if (!isValidTask(task)) continue;

            sendReminder(task);
        }
    }

    // =====================================================
    // ⏰ USER CUSTOM REMINDER TIME
    // =====================================================
    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Kolkata")
    public void sendUserSelectedReminders() {

        System.out.println("⏰ Checking Custom Reminder Times");

        List<Task> tasks = taskRepository.findAll();

        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        for (Task task : tasks) {

            if (!isValidTask(task)) continue;

            LocalTime reminderTime = task.getReminderTime();

            if (reminderTime == null) continue;

            LocalTime taskTime = reminderTime.withSecond(0).withNano(0);

            if (taskTime.equals(now)) {
                sendReminder(task);
            }
        }
    }

    // =====================================================
    // SEND EMAIL
    // =====================================================
    private void sendReminder(Task task) {

        try {

            if (task.getUser() == null) return;

            String email = task.getUser().getEmail();

            if (email == null || email.isBlank()) return;

            emailService.sendTaskReminder(email, task);

            task.setLastReminderSentAt(LocalDateTime.now());
            taskRepository.save(task);

            System.out.println("📩 Reminder sent for: " + task.getTitle());

        } catch (Exception e) {

            System.out.println("❌ Error sending reminder for task: " + task.getId());
            e.printStackTrace();
        }
    }

    // =====================================================
    // TASK VALIDATION
    // =====================================================
    private boolean isValidTask(Task task) {

        if (task == null) return false;

        if (task.getStatus() == null) return false;

        String status = task.getStatus().toUpperCase();

        if (!(status.equals("PENDING") || status.equals("IN_PROGRESS"))) {
            return false;
        }

        if (task.getUser() == null) return false;

        return true;
    }
}