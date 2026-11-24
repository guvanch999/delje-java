package tm.delje.shop.web.admin;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
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
import tm.delje.shop.dto.ProductForm;
import tm.delje.shop.entity.Product;
import tm.delje.shop.entity.ProductImage;
import tm.delje.shop.service.CategoryService;
import tm.delje.shop.service.ImageService;
import tm.delje.shop.service.NotFoundException;
import tm.delje.shop.service.ProductService;
import tm.delje.shop.web.FlashMessage;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductController.class);

    private static final int MAX_FILES = 12;

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(required = false) String page,
                       Model model) {
        int pageNumber = parsePage(page);
        Page<Product> result = productService.list(q, pageNumber);

        model.addAttribute("title", "Товары");
        model.addAttribute("products", result.getContent());
        model.addAttribute("count", result.getTotalElements());
        model.addAttribute("page", pageNumber);
        model.addAttribute("pages", Math.max(1, result.getTotalPages()));
        model.addAttribute("q", q);
        String pageUrl = "/admin/products?" + (q.isEmpty() ? "" : "q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&") + "page=";
        model.addAttribute("pageUrl", pageUrl);
        return "admin/products/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, null, new ProductForm(), null);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        Product product = find(id);
        return form(model, product, ProductForm.of(product), null);
    }

    @PostMapping
    public String create(@ModelAttribute("values") ProductForm values, BindingResult bindingResult,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images,
                         Model model, RedirectAttributes redirectAttributes) {
        String error = validate(values, images);
        if (error != null) {
            return form(model, null, values, error);
        }

        try {
            productService.create(values, images);
        } catch (ImageService.InvalidImageException e) {
            return form(model, null, values, "Файл должен быть изображением");
        } catch (IOException e) {
            log.error("product image processing failed", e);
            return form(model, null, values, "Не удалось обработать картинку");
        }
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Товар создан"));
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Integer id, @ModelAttribute("values") ProductForm values,
                         BindingResult bindingResult,
                         @RequestParam(value = "images", required = false) List<MultipartFile> images,
                         Model model, RedirectAttributes redirectAttributes) {
        Product product = find(id);
        String error = validate(values, images);
        if (error != null) {
            return form(model, product, values, error);
        }

        try {
            productService.update(id, values, images);
        } catch (ImageService.InvalidImageException e) {
            return form(model, product, values, "Файл должен быть изображением");
        } catch (IOException e) {
            log.error("product image processing failed", e);
            return form(model, product, values, "Не удалось обработать картинку");
        }
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Товар сохранён"));
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        productService.delete(id);
        redirectAttributes.addFlashAttribute("flash", FlashMessage.success("Товар удалён"));
        return "redirect:/admin/products";
    }

    private String validate(ProductForm values, List<MultipartFile> images) {
        String error = values.validate();
        if (error != null) {
            return error;
        }
        if (images != null && images.stream().filter(f -> !f.isEmpty()).count() > MAX_FILES) {
            return "Можно загрузить не больше " + MAX_FILES + " картинок за раз";
        }
        return null;
    }

    private Product find(Integer id) {
        return productService.findWithImages(id).orElseThrow(() -> new NotFoundException("product " + id));
    }

    private String form(Model model, Product product, ProductForm values, String error) {
        List<ProductImage> images = product != null ? product.getImages() : List.of();
        model.addAttribute("title", product != null ? "Редактирование товара" : "Новый товар");
        model.addAttribute("product", product);
        model.addAttribute("images", images);
        model.addAttribute("values", values);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("error", error);
        return "admin/products/form";
    }

    private static int parsePage(String page) {
        if (page == null) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(page.trim()));
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
