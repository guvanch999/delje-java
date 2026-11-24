package tm.delje.shop.web.admin;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tm.delje.shop.dto.BannerForm;
import tm.delje.shop.service.BannerService;
import tm.delje.shop.service.ImageService;
import tm.delje.shop.web.FlashMessage;

@Controller
@RequestMapping("/admin/banners")
public class AdminBannerController {

    private static final Logger log = LoggerFactory.getLogger(AdminBannerController.class);

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("title", "Баннеры");
        model.addAttribute("banners", bannerService.findAll());
        return "admin/banners/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, new BannerForm(), null);
    }

    @PostMapping
    public String create(@ModelAttribute("values") BannerForm values, BindingResult bindingResult,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         Model model, RedirectAttributes redirectAttributes) {
        if (image == null || image.isEmpty()) {
            return form(model, values, "Нужно загрузить картинку баннера");
        }
        if (image.getContentType() == null || !image.getContentType().startsWith("image/")) {
            return form(model, values, "Файл должен быть изображением");
        }
        String error = values.validate();
        if (error != null) {
            return form(model, values, error);
        }

        try {
            bannerService.create(values, image);
        } catch (ImageService.InvalidImageException e) {
            return form(model, values, "Файл должен быть изображением");
        } catch (IOException e) {
            log.error("banner image processing failed", e);
            return form(model, values, "Не удалось обработать картинку");
        }
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Баннер создан"));
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        bannerService.toggle(id);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Статус баннера обновлён"));
        return "redirect:/admin/banners";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        bannerService.delete(id);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Баннер удалён"));
        return "redirect:/admin/banners";
    }

    private String form(Model model, BannerForm values, String error) {
        model.addAttribute("title", "Новый баннер");
        model.addAttribute("values", values);
        model.addAttribute("error", error);
        return "admin/banners/form";
    }
}
