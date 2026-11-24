package tm.delje.shop.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Runs for /tk/** and /ru/**: remembers the language in a cookie (used by the "/" redirect)
 * and adds the common public page attributes to the model.
 */
public class PublicPageInterceptor implements HandlerInterceptor {

    private static final long ONE_YEAR_SECONDS = 365L * 24 * 60 * 60;

    private final PublicPageModel pageModel;
    private final boolean secureCookie;

    public PublicPageInterceptor(PublicPageModel pageModel, boolean secureCookie) {
        this.pageModel = pageModel;
        this.secureCookie = secureCookie;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String lang = Lang.fromPath(request.getRequestURI());
        // only write the cookie when the choice changes
        if (lang != null && !lang.equals(Lang.savedLang(request))) {
            ResponseCookie cookie = ResponseCookie.from(Lang.COOKIE_NAME, lang)
                    .path("/")
                    .maxAge(ONE_YEAR_SECONDS)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null) {
            return;
        }
        String viewName = modelAndView.getViewName();
        if (viewName == null || viewName.startsWith("redirect:")) {
            return;
        }
        String lang = Lang.fromPath(request.getRequestURI());
        if (lang != null) {
            pageModel.fill(modelAndView.getModel(), request, lang);
        }
    }
}
