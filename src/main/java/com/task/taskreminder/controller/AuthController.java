package com.task.taskreminder.controller;

import com.task.taskreminder.model.User;
import com.task.taskreminder.repository.UserRepository;
import com.task.taskreminder.service.UserService;
import jakarta.servlet.http.HttpSession;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(UserService userService,
                      UserRepository userRepository) {
    this.userService = userService;
    this.userRepository = userRepository;
}
@GetMapping("/users")
public String usersPage(Model model) {
    

    List<User> users = userRepository.findAll();

    System.out.println("USERS FOUND = " + users.size());

    model.addAttribute("users", users);

    return "users";
}

  // ROOT → LOGIN
    


    // REGISTER PAGE
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("showOtp", false);
        return "register";
    }
    
  // LOGIN PAGE
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("loggedUser") != null) {
            return "redirect:/home";
        }
        return "login";
    }
    @GetMapping("/logout")
public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/login";
}
    

@PostMapping("/login")
public String login(@RequestParam String email,
                    @RequestParam String password,
                    HttpSession session,
                    Model model) {

    User user = userService.login(email, password);

    if (user == null) {
        model.addAttribute("error", "Invalid credentials or not verified");
        return "login";
    }
    if (user!=null) 
    session.setAttribute("loggedUser", user);
    return "redirect:/home";

}



    // SEND OTP
    @PostMapping("/send-otp")
    public String sendOtp(@ModelAttribute User user, Model model) {

        String result = userService.register(user);

        if (result.equals("EMAIL_EXISTS")) {
            model.addAttribute("error", "Email already exists");
            model.addAttribute("showOtp", false);
            return "register";
        }

        model.addAttribute("email", user.getEmail());
        model.addAttribute("showOtp", true);
        model.addAttribute("message", "OTP sent to your email");
        return "register";
    }
    @GetMapping("/verify")
public String showVerifyPage() {
    return "verify";
}

    // VERIFY OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String email,
                            @RequestParam String otp,
                            Model model) {

        boolean verified = userService.verifyOtp(email, otp);

        if (verified) {
            return "redirect:/login";
        }

        model.addAttribute("email", email);
        model.addAttribute("showOtp", true);
        model.addAttribute("error", "Invalid OTP");
        return "register";
    }
    @GetMapping("/delete-user/{id}")
public String deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return "redirect:/users";
}

@GetMapping("/")
public String landingPage() {
    return "landing";
}




}