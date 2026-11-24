package tm.delje.shop.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tm.delje.shop.dto.CategoryForm;
import tm.delje.shop.entity.Category;
import tm.delje.shop.service.CategoryService;
import tm.delje.shop.service.NotFoundException;
import tm.delje.shop.web.FlashMessage;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("title", "Категории");
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, null, new CategoryForm(), null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Category category = find(id);
        return form(model, category, CategoryForm.of(category), null);
    }

    @PostMapping
    public String create(@ModelAttribute("values") CategoryForm values, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        String error = values.validate();
        if (error != null) {
            return form(model, null, values, error);
        }
        categoryService.create(values);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Категория создана"));
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute("values") CategoryForm values,
                         BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        Category category = find(id);
        String error = values.validate();
        if (error != null) {
            return form(model, category, values, error);
        }
        categoryService.update(id, values);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Категория сохранена"));
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        categoryService.delete(id);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Категория удалена"));
        return "redirect:/admin/categories";
    }

    private Category find(Integer id) {
        return categoryService.findById(id).orElseThrow(() -> new NotFoundException("category " + id));
    }

    private String form(Model model, Category category, CategoryForm values, String error) {
        model.addAttribute("title", category != null ? "Редактирование категории" : "Новая категория");
        model.addAttribute("category", category);
        model.addAttribute("values", values);
        model.addAttribute("error", error);
        return "admin/categories/form";
    }
}
