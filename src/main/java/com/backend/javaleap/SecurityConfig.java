package com.backend.javaleap;

import com.backend.javaleap.exception.RestAccessDeniedHandler;
import com.backend.javaleap.exception.RestAuthenticationEntryPoint;
import com.backend.javaleap.service.ApiKeyAuthenticationFilter;
import com.backend.javaleap.service.ApiKeyService;
import com.backend.javaleap.service.JwtAuthenticationFilter;
import com.backend.javaleap.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security: JWT for people, X-API-KEY for machines (System actor).
 *
 * <pre>
 * PUBLIC       POST /api/auth/login, /api/health, /actuator/health, Swagger, /ws/**
 * ANY USER     /api/auth/**  (me, change password)
 * SYSTEM       POST /api/kinetic/telemetry           (ADMIN inherits)
 * ADMIN        /api/users/**, /api/admin/**, POST/PUT/PATCH /api/kinetic/**, POST /api/simulation/**
 * ACCOUNTANT   read access to /api/** (accounting endpoints added in Phase C/D)
 * </pre>
 * Role hierarchy: ADMIN &gt; ACCOUNTANT and ADMIN &gt; SYSTEM.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/login",
            "/api/health",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/ws/**",
            "/error"
    };

    private final JwtService jwtService;
    private final ApiKeyService apiKeyService;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/kinetic/telemetry").hasAnyRole("SYSTEM", "ADMIN")
                        .requestMatchers("/api/users/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/kinetic/**", "/api/simulation/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/kinetic/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/kinetic/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/accounting/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/accounting/accounts/**", "/api/accounting/journals/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/accounting/accounts/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/budgets/**").hasRole("ADMIN")
                        .requestMatchers("/api/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                        .anyRequest().denyAll())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyService), JwtAuthenticationFilter.class);
        return http.build();
    }

    /** ADMIN can do everything an ACCOUNTANT or the SYSTEM can do. */
    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_ACCOUNTANT
                ROLE_ADMIN > ROLE_SYSTEM
                """);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
