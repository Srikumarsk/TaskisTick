package com.task.taskreminder.controller;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.servlet.http.HttpServletResponse;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;





import com.task.taskreminder.model.Task;
import com.task.taskreminder.repository.TaskRepository;
import com.task.taskreminder.service.EmailService;
import com.task.taskreminder.model.User;


import jakarta.servlet.http.HttpSession;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
public class TaskController {

    private final TaskRepository repository;


    public TaskController(TaskRepository repository) {
        this.repository = repository;
    }
  @Autowired
private EmailService emailService;
  @GetMapping("/home")
public String home(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "5") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        Model model ,HttpSession session) {
            User loggedUser = (User) session.getAttribute("loggedUser");

    
  Sort prioritySort = Sort.by(
        Sort.Order.asc(
                "priority"
        )
);

Pageable pageable = PageRequest.of(page, size, prioritySort);

   Page<Task> taskPage = repository.findByUser(loggedUser, pageable);

    List<Task> tasks = taskPage.getContent();
    tasks = tasks.stream()
    .sorted((t1, t2) -> {

        List<String> order = List.of("HIGH", "MEDIUM", "LOW");

        String p1 = (t1.getPriority() == null) ? "LOW" : t1.getPriority();
        String p2 = (t2.getPriority() == null) ? "LOW" : t2.getPriority();

        return Integer.compare(
                order.indexOf(p1),
                order.indexOf(p2)
        );
    })
    .toList();


    // Apply filters on current page (simple approach)
    if (keyword != null && !keyword.isEmpty()) {
        tasks = tasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
    if (status != null && !status.isEmpty()) {
        tasks = tasks.stream()  
                .filter(t -> t.getStatus().equalsIgnoreCase(status))
                .toList();
    }
    if (priority != null && !priority.isEmpty()) {
        tasks = tasks.stream()
                .filter(t -> t.getPriority().equalsIgnoreCase(priority))
                .toList();
    }

    // ✅ Counts (from filtered list)
    long pendingCount = tasks.stream().filter(t -> "PENDING".equalsIgnoreCase(t.getStatus())).count();
    long inProgressCount = tasks.stream().filter(t -> "IN_PROGRESS".equalsIgnoreCase(t.getStatus())).count();
    long doneCount = tasks.stream().filter(t -> "DONE".equalsIgnoreCase(t.getStatus())).count();

    model.addAttribute("pendingCount", pendingCount);
    model.addAttribute("inProgressCount", inProgressCount);
    model.addAttribute("doneCount", doneCount);
    int serialNo = 1;
for (Task t : tasks) {
    t.setSerialNo(serialNo++);
}


    model.addAttribute("tasks", tasks);
    model.addAttribute("task", new Task());

    // pagination data
    model.addAttribute("currentPage", page);
    model.addAttribute("totalPages", taskPage.getTotalPages());
    model.addAttribute("size", size);

    return "index";
    
}
@GetMapping("/filter/search")
public String filterTasks(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        Model model,
        HttpSession session) {

    User loggedUser = (User) session.getAttribute("loggedUser");

    List<Task> tasks = repository.findByUser(loggedUser);

    if (keyword != null && !keyword.isEmpty()) {
        tasks = tasks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    if (status != null && !status.isEmpty()) {
        tasks = tasks.stream()
                .filter(t -> t.getStatus().equalsIgnoreCase(status))
                .toList();
    }

    if (priority != null && !priority.isEmpty()) {
        tasks = tasks.stream()
                .filter(t -> t.getPriority().equalsIgnoreCase(priority))
                .toList();
    }

    model.addAttribute("tasks", tasks);
    return "filter";
}




@GetMapping("/filter")
public String filterPage(HttpSession session) {

    if (session.getAttribute("loggedUser") == null) {
        return "redirect:/login";
    }

    return "filter";
}


@GetMapping("/welcome")
public String welcome() {
    return "welcome";
}
@GetMapping("/cards")
public String cardView(Model model, HttpSession session) {

    User loggedUser = (User) session.getAttribute("loggedUser");

    List<Task> tasks = repository.findByUser(loggedUser);

    //  Priority order: HIGH → MEDIUM → LOW
    tasks = tasks.stream()
    .sorted((t1, t2) -> {

        List<String> order = List.of("HIGH", "MEDIUM", "LOW");

        String p1 = (t1.getPriority() == null) ? "LOW" : t1.getPriority();
        String p2 = (t2.getPriority() == null) ? "LOW" : t2.getPriority();

        return Integer.compare(
                order.indexOf(p1),
                order.indexOf(p2)
        );
    })
    .toList();
            
    int serialNo = 1;
    for (Task t : tasks) {
        t.setSerialNo(serialNo++);
    }

    model.addAttribute("tasks", tasks);
    return "card-view";
}

    @PostMapping("/addTask")
public String addTask(@ModelAttribute Task task, HttpSession session) {

    User loggedUser = (User) session.getAttribute("loggedUser");

    task.setUser(loggedUser);   // 🔥 STEP 3 (IMPORTANT)
    // Apply Smart Reminder Logic
Integer finalInterval = calculateSmartReminder(
        task.getDate(),
        task.getPriority(),
        task.getReminderInterval()
);

task.setReminderInterval(finalInterval);
     if (task.getReminderInterval() == null) {
        task.setReminderInterval(60);
    }
 
    repository.save(task);
     emailService.sendTaskCreatedMail(loggedUser.getEmail(), task);
     emailService.sendMail(
        loggedUser.getEmail(),
        "New Task Added",
        "Task: " + task.getTitle() + " added successfully!"
    );

    return "redirect:/home";
}

// SMART AUTO REMINDER LOGIC

private Integer calculateSmartReminder(LocalDate dueDate, String priority, Integer userSelectedInterval) {

    long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

    // HIGH PRIORITY
    if (priority.equals("HIGH")) {

        if (daysLeft == 0) return 120;   // 2 hrs
        if (daysLeft == 1) return 360;   // 6 hrs
        if (daysLeft == 2) return 720;   // 12 hrs

        return userSelectedInterval;
    }

    // MEDIUM PRIORITY
    else if (priority.equals("MEDIUM")) {

        if (daysLeft == 0) return 180;   // 3 hrs
        if (daysLeft == 1) return 480;   // 8 hrs

        return userSelectedInterval;
    }

    // LOW PRIORITY
    else {

        if (daysLeft == 0) return 240;   // 4 hrs
        if (daysLeft == 1) return 600;   // 10 hrs

        return userSelectedInterval;
    }
}

@GetMapping("/edit/{id}")
public String showEditPage(@PathVariable Long id, Model model) {
    Task task = repository.findById(id).orElseThrow();
    model.addAttribute("task", task);
    return "edit";   // edit.html
}


    // Delete Task
    @GetMapping("/delete/{id}")
public String deleteTask(@PathVariable Long id) {
    System.out.println("Deleting id = " + id);
    repository.deleteById(id);
    return "redirect:/home";
}

@GetMapping("/toggle/{id}")
public String toggleStatus(@PathVariable Long id) {

    Task task = repository.findById(id).orElse(null);

    if (task != null) {

        if ("DONE".equalsIgnoreCase(task.getStatus())) {

            task.setStatus("PENDING");
            task.setCompletedAt(null);

            // ✅ Reset reminder again
            task.setReminderSent(false);

        } else {

            task.setStatus("DONE");

            task.setCompletedAt(LocalDateTime.now());
        }

        repository.save(task);
    }

    return "redirect:/home";
}





    @GetMapping("/view/{id}")
public String viewTask(@PathVariable Long id, Model model, HttpSession session) {
    User loggedUser = (User) session.getAttribute("loggedUser");

List<Task> tasks = repository.findByUser(loggedUser);

int serial = 1;
for (Task t : tasks) {
    if (t.getId().equals(id)) {
        t.setSerialNo(serial);
        model.addAttribute("task", t);
        break;
    }
    serial++;
}

    return "view-task";
}
@GetMapping("/search")
public String search(@RequestParam("q") String q, Model model) {
    model.addAttribute("task", new Task());
    model.addAttribute("tasks",
            repository.findAll().stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(q.toLowerCase()))
                    .toList()
    );
    return "index";
}
@GetMapping("/api/tasks")
@ResponseBody
public List<Task> getTasks(HttpSession session) {

    User loggedUser = (User) session.getAttribute("loggedUser");

    return repository.findByUser(loggedUser);
}


@GetMapping("/calendar")
public String calendarView() {
    return "calendar";
}
@PostMapping("/updateTask")
public String updateTask(@ModelAttribute Task task, HttpSession session) {

    User loggedUser = (User) session.getAttribute("loggedUser");

    if (loggedUser == null) {
        return "redirect:/login";
    }

    task.setUser(loggedUser);

    // ✅ Set completedAt automatically
    if ("DONE".equalsIgnoreCase(task.getStatus())) {

        // Only set if it was not already set
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }

    } else {
        // If status not DONE → remove completedAt
        task.setCompletedAt(null);
    }

    repository.save(task);

    return "redirect:/home";
}
@GetMapping("/tasks/download")
public void downloadTasksExcel(
        HttpSession session,
        HttpServletResponse response) throws IOException {

    User loggedUser = (User) session.getAttribute("loggedUser");
    if (loggedUser == null) {
        response.sendRedirect("/login");
        return;
    }

    List<Task> tasks = repository.findByUser(loggedUser);

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet("My Tasks");

    // Header row
    Row header = sheet.createRow(0);
    String[] columns = {"S.No", "Title", "Description", "Date", "Priority", "Status"};

    for (int i = 0; i < columns.length; i++) {
        Cell cell = header.createCell(i);
        cell.setCellValue(columns[i]);
    }

    // Data rows
    int rowNum = 1;
    int serial = 1;

    for (Task task : tasks) {
        Row row = sheet.createRow(rowNum++);

        row.createCell(0).setCellValue(serial++);
        row.createCell(1).setCellValue(task.getTitle());
        row.createCell(2).setCellValue(task.getDescription());
        row.createCell(3).setCellValue(task.getDate().toString());
        row.createCell(4).setCellValue(task.getPriority());
        row.createCell(5).setCellValue(task.getStatus());
    }

    // Auto-size columns
    for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
    }

    // Response settings
    response.setContentType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setHeader(
        "Content-Disposition",
        "attachment; filename=My_Tasks.xlsx");

    workbook.write(response.getOutputStream());
    workbook.close();
}


}
