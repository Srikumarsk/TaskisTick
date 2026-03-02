package com.task.taskreminder.scheduler;

import com.task.taskreminder.model.Task;
import com.task.taskreminder.repository.TaskRepository;
import com.task.taskreminder.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
    // 🌅 DAILY MORNING REMINDER – 8 AM IST
    // =====================================================
    @Scheduled(cron = "0 16 11 * * ?", zone = "Asia/Kolkata")
    public void sendMorningReminders() {

        System.out.println("🌅 Running Morning Reminder");

        List<Task> tasks = taskRepository.findAll();

        for (Task task : tasks) {

            if (!isValidTask(task)) continue;

            sendReminder(task);
        }
    }

    // =====================================================
    // ⏰ CUSTOM INTERVAL REMINDER (Runs Every 1 Minute)
    // =====================================================
    @Scheduled(fixedRate = 60000)
    public void sendIntervalReminders() {

        System.out.println("⏰ Checking Interval Reminders...");

        List<Task> tasks = taskRepository.findAll();

        for (Task task : tasks) {

            if (!isValidTask(task)) continue;

            Integer interval = task.getReminderInterval();

            if (interval == null || interval <= 0) {
                continue;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lastSent = task.getLastReminderSentAt();

            boolean shouldSend = false;

            if (lastSent == null) {
                shouldSend = true;
            } else if (lastSent.plusMinutes(interval).isBefore(now)) {
                shouldSend = true;
            }

            if (shouldSend) {
                sendReminder(task);
            }
        }
    }

    // =====================================================
    // COMMON REMINDER METHOD
    // =====================================================
    private void sendReminder(Task task) {

        try {

            String email = task.getUser().getEmail();

            if (email == null || email.isBlank()) {
                System.out.println("⚠ No email found for task: " + task.getId());
                return;
            }

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
    // VALIDATION METHOD
    // =====================================================
    private boolean isValidTask(Task task) {

        if (task == null) return false;

        if (task.getStatus() == null) return false;

        if (!task.getStatus().equalsIgnoreCase("PENDING")) return false;

        if (task.getUser() == null) {
            System.out.println("⚠ Skipping task with null user: " + task.getId());
            return false;
        }

        return true;
    }
}