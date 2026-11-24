package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    /**
     * "/" goes to the language section: saved choice, then Accept-Language, then Turkmen.
     */
    @GetMapping("/")
    public String root(HttpServletRequest request) {
        return "redirect:/" + Lang.resolve(request);
    }
}
