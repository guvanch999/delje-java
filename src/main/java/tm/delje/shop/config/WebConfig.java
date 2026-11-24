package tm.delje.shop.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tm.delje.shop.service.ImageService;
import tm.delje.shop.web.LangLocaleResolver;
import tm.delje.shop.web.PublicPageInterceptor;
import tm.delje.shop.web.PublicPageModel;

@Configuration
@ConditionalOnWebApplication
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties properties;
    private final PublicPageModel publicPageModel;
    private final ImageService imageService;

    public WebConfig(AppProperties properties, PublicPageModel publicPageModel, ImageService imageService) {
        this.properties = properties;
        this.publicPageModel = publicPageModel;
        this.imageService = imageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());

        // uploaded file names are random uuids and never change, so they can be cached for long
        String uploadsLocation = imageService.getUploadsRoot().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadsLocation)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PublicPageInterceptor(publicPageModel, properties.isCookieSecure()))
                .addPathPatterns("/tk", "/tk/**", "/ru", "/ru/**");
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new LangLocaleResolver();
    }

    /**
     * "/tk/" and "/tk/products/" are handled the same way as the paths without the slash.
     */
    @Bean
    public FilterRegistrationBean<UrlHandlerFilter> trailingSlashFilter() {
        UrlHandlerFilter filter = UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();
        FilterRegistrationBean<UrlHandlerFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
