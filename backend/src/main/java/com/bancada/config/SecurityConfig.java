package com.bancada.config;

import com.bancada.repository.CustomerRepository;
import com.bancada.service.PortalTokenService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security of the customer panel (stateless JWT). The administrator API keeps its own guard, the
 * session token of the Tauri shell ({@link AppTokenFilter}); here every non-portal path is left to it.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PortalTokenService portalTokenService,
                                                   CustomerRepository customerRepository) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/api/portal/auth/**", "/api/portal/public/**").permitAll()
                .requestMatchers("/api/portal/**", "/ws/portal/**").hasRole(PortalJwtFilter.CUSTOMER_ROLE)
                .anyRequest().permitAll())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Sua sessão expirou. Entre de novo."))
                .accessDeniedHandler((request, response, exception) ->
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Sem permissão para esta ação.")))
            .addFilterBefore(new PortalJwtFilter(portalTokenService, customerRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now() + "\",\"message\":[\"" + message + "\"]}");
    }
}
