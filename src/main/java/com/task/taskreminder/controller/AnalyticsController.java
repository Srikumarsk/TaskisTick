package com.task.taskreminder.controller;

import com.task.taskreminder.model.Task;
import com.task.taskreminder.model.User;
import com.task.taskreminder.repository.TaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AnalyticsController {

    private final TaskRepository taskRepository;

    public AnalyticsController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/analytics")
    public String analytics(Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        List<Task> tasks = taskRepository.findByUser(user);

        long total = tasks.size();

        long completed = tasks.stream()
                .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                .count();

        long pending = tasks.stream()
                .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                .count();

        long inProgress = tasks.stream()
                .filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                .count();

        long overdue = tasks.stream()
                .filter(t -> t.getDate() != null &&
                        t.getDate().isBefore(LocalDate.now()) &&
                        !"DONE".equalsIgnoreCase(t.getStatus()))
                .count();

        double completionRate = total == 0 ? 0 :
                ((double) completed / total) * 100;

        model.addAttribute("totalTasks", total);
        model.addAttribute("completedTasks", completed);
        model.addAttribute("pendingTasks", pending);
        model.addAttribute("inProgressTasks", inProgress);
        model.addAttribute("overdueTasks", overdue);
        model.addAttribute("completionRate", Math.round(completionRate));

        return "analytics";
    }
}