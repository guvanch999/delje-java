package tm.delje.shop.web.admin;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {

    @GetMapping("/admin/login")
    public String login(@RequestParam(required = false) String error, Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/admin";
        }
        model.addAttribute("title", "Вход");
        model.addAttribute("error", error != null ? "Неверный логин или пароль" : null);
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("title", "Панель управления");
        return "admin/dashboard";
    }
}
