package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;
import tm.delje.shop.service.NotFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final Pattern PRODUCT_UPDATE = Pattern.compile("^/admin/products/(\\d+)$");

    private final PublicPageModel publicPageModel;
    private final Messages messages;

    public GlobalExceptionHandler(PublicPageModel publicPageModel, Messages messages) {
        this.publicPageModel = publicPageModel;
        this.messages = messages;
    }

    /**
     * Form values are trimmed everywhere, empty strings stay empty strings.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(false));
    }

    /**
     * Localized 404 page for unknown URLs, missing products and missing admin records.
     */
    @ExceptionHandler({NotFoundException.class, NoResourceFoundException.class, NoHandlerFoundException.class,
            MethodArgumentTypeMismatchException.class})
    public ModelAndView notFound(HttpServletRequest request) {
        String lang = Lang.resolve(request);
        ModelAndView mav = new ModelAndView("public/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        mav.addObject("title", messages.get("meta.notFoundTitle", lang));
        mav.addObject("metaDescription", null);
        publicPageModel.fill(mav.getModel(), request, lang);
        return mav;
    }

    /**
     * Upload over the size limit: back to the form with an error message.
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public String uploadFailed(MultipartException e, HttpServletRequest request) {
        log.warn("upload rejected for {}: {}", request.getRequestURI(), e.getMessage());

        String uri = request.getRequestURI();
        String target;
        Matcher matcher = PRODUCT_UPDATE.matcher(uri);
        if (matcher.matches()) {
            target = "/admin/products/" + matcher.group(1) + "/edit";
        } else if (uri.startsWith("/admin/products")) {
            target = "/admin/products/new";
        } else if (uri.startsWith("/admin/banners")) {
            target = "/admin/banners/new";
        } else {
            target = "/";
        }

        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        flashMap.put("flash", FlashMessage.error("Файл слишком большой или форма повреждена"));
        return "redirect:" + target;
    }
}
