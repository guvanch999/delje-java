package tm.delje.shop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * The admin panel has a single account configured with ADMIN_LOGIN / ADMIN_PASSWORD_HASH.
 * Everything outside /admin is public.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties) throws Exception {
        // token lives in a cookie, so visitors of public pages with forms do not get a server session
        CookieCsrfTokenRepository csrfRepository = new CookieCsrfTokenRepository();
        csrfRepository.setCookieCustomizer(cookie -> cookie.secure(properties.isCookieSecure()).sameSite("Lax"));

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("login")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/admin", true)
                        .failureHandler((request, response, exception) -> {
                            log.warn("failed admin login, login={}", request.getParameter("login"));
                            response.sendRedirect("/admin/login?error");
                        }))
                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login")
                        .deleteCookies("DELJE_SESSION"))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .requestCache(cache -> cache.disable());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(AppProperties properties) {
        String login = properties.getAdmin().getLogin();
        String hash = properties.getAdmin().getPasswordHash();
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        if (login == null || login.isBlank() || hash == null || hash.isBlank()) {
            log.warn("ADMIN_LOGIN or ADMIN_PASSWORD_HASH is not set, admin login is disabled");
            return manager;
        }
        manager.createUser(User.withUsername(login).password(hash).roles("ADMIN").build());
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
