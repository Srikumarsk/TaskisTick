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
public class DashboardController {

    private final TaskRepository taskRepository;

    public DashboardController(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {

        User loggedUser = (User) session.getAttribute("loggedUser");
        if (loggedUser == null) {
            return "redirect:/login";
        }

        List<Task> tasks = taskRepository.findByUser(loggedUser);

        long total = tasks.size();
        long completed = tasks.stream()
                .filter(t -> "DONE".equalsIgnoreCase(t.getStatus()))
                .count();
        long pending = tasks.stream()
                .filter(t -> "PENDING".equalsIgnoreCase(t.getStatus()))
                .count();
        long InProgress = tasks.stream()
                .filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus()))
                .count();        
        long overdue = tasks.stream()
                .filter(t -> t.getDate() != null &&
                        t.getDate().isBefore(LocalDate.now()) &&
                        !"DONE".equalsIgnoreCase(t.getStatus()))
                .count();
        long highPriority = tasks.stream()
                .filter(t -> t.getPriority() != null &&
         "High".equalsIgnoreCase(t.getPriority().trim()))
                .count();

long dueToday = tasks.stream()
        .filter(t -> t.getDate() != null &&
                     t.getDate().equals(LocalDate.now()))
        .count();              

        List<Task> recentTasks = tasks.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(5)
                .toList();
                
       double productivityScore = 0;

if(total > 0){
    productivityScore = (completed * 100.0) / total;
}

model.addAttribute("productivityScore", Math.round(productivityScore));

        model.addAttribute("totalTasks", total);
        model.addAttribute("completedTasks", completed);
        model.addAttribute("pendingTasks", pending);
        model.addAttribute("inProgressTasks", InProgress);
        model.addAttribute("overdueTasks", overdue);
        model.addAttribute("highPriority", highPriority);
        model.addAttribute("dueTodayTasks", dueToday);
        model.addAttribute("recentTasks", recentTasks);
        model.addAttribute("user", loggedUser);

        return "dashboard";
    }
}
