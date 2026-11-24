package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tm.delje.shop.dto.ContactForm;
import tm.delje.shop.service.ContactService;

@Controller
public class PageController {

    // the form shows one error at a time, checked in the order of the fields
    private static final List<String> CONTACT_FIELDS = List.of("name", "email", "message");

    private final ContactService contactService;
    private final Messages messages;

    public PageController(ContactService contactService, Messages messages) {
        this.contactService = contactService;
        this.messages = messages;
    }

    @GetMapping("/{lang:tk|ru}/about")
    public String about(@PathVariable String lang, Model model) {
        model.addAttribute("title", messages.get("meta.aboutTitle", lang));
        model.addAttribute("metaDescription", messages.get("meta.aboutDesc", lang));
        return "public/about";
    }

    @GetMapping("/{lang:tk|ru}/contact")
    public String contact(@PathVariable String lang, Model model) {
        if (!model.containsAttribute("values")) {
            model.addAttribute("values", new ContactForm());
        }
        return contactPage(lang, model, null);
    }

    @PostMapping("/{lang:tk|ru}/contact")
    public String sendContact(@PathVariable String lang,
                              @Valid @ModelAttribute("values") ContactForm form,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return contactPage(lang, model, messages.get(firstErrorKey(bindingResult), lang));
        }
        if (contactService.isRateLimited(request.getRemoteAddr())) {
            return contactPage(lang, model, messages.get("contact.error.tooMany", lang));
        }
        if (!contactService.send(form, lang)) {
            return contactPage(lang, model, messages.get("contact.error.sendFailed", lang));
        }

        redirectAttributes.addFlashAttribute("sent", true);
        return "redirect:/" + lang + "/contact";
    }

    private String contactPage(String lang, Model model, String error) {
        model.addAttribute("title", messages.get("meta.contactTitle", lang));
        model.addAttribute("metaDescription", messages.get("meta.contactDesc", lang));
        model.addAttribute("error", error);
        if (!model.containsAttribute("sent")) {
            model.addAttribute("sent", false);
        }
        return "public/contact";
    }

    private static String firstErrorKey(BindingResult bindingResult) {
        for (String field : CONTACT_FIELDS) {
            FieldError error = bindingResult.getFieldError(field);
            if (error != null) {
                return error.getDefaultMessage();
            }
        }
        return "contact.error.message";
    }
}
