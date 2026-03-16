package com.USWCicrcleLink.server.global.security.config;

import com.USWCicrcleLink.server.global.security.exception.CustomAuthenticationEntryPoint;
import com.USWCicrcleLink.server.global.security.jwt.filter.LoggingFilter;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.SecurityProperties;
import com.USWCicrcleLink.server.global.security.jwt.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_LEADER = "LEADER";
    private static final String ROLE_USER = "USER";
    private static final String[] PERMITTED_HEADERS = {
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "emailToken_uuid",
            "uuid"
    };
    private static final HttpMethod[] ALLOWED_METHODS = {
            HttpMethod.GET,
            HttpMethod.POST,
            HttpMethod.PUT,
            HttpMethod.PATCH,
            HttpMethod.DELETE,
            HttpMethod.OPTIONS
    };

    private static final String[] ADMIN_SHARED_READ_PATHS = {"/admin/clubs", "/admin/clubs/{clubUUID}"};
    private static final String[] NOTICE_SHARED_READ_PATHS = {"/notices/{noticeUUID}", "/notices"};
    private static final String[] ADMIN_ALL_PATHS = {"/admin/**"};
    private static final String[] NOTICE_ADMIN_PATHS = {"/notices/**"};
    private static final String[] USER_PATCH_PATHS = {"/profiles/change", "/users/userpw", "/club-leader/fcmtoken"};
    private static final String[] USER_GET_PATHS = {"/my-notices", "/mypages/my-clubs", "/mypages/club-applications", "/profiles/me", "/my-notices/{noticeUUID}/details"};
    private static final String[] USER_EXIT_POST_PATHS = {"/users/exit/send-code"};
    private static final String[] USER_EXIT_DELETE_PATHS = {"/users/exit"};
    private static final String[] USER_APPLY_PATHS = {"/apply/**"};
    private static final String[] LEADER_ALL_PATHS = {"/club-leader/**"};
    private static final String[] ADMIN_LEADER_SHARED_GET_PATHS = mergePaths(ADMIN_SHARED_READ_PATHS, NOTICE_SHARED_READ_PATHS);

    private final JwtProvider jwtProvider;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final SecurityProperties securityProperties;

    @Value("${cors.allowed-origins}")
    private String allowedOrigin;

    @Bean
    public JwtFilter jwtAuthFilter() {
        return new JwtFilter(jwtProvider, securityProperties.getPermitAllPaths(), customAuthenticationEntryPoint);
    }

    @Bean
    public LoggingFilter loggingFilter() {
        return new LoggingFilter(securityProperties.getLoggingPaths(), securityProperties.getMethods());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(customAuthenticationEntryPoint))
                .authorizeHttpRequests(this::configureAuthorization)
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(loggingFilter(), JwtFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        configuration.addAllowedOriginPattern(allowedOrigin);
        addAllowedMethods(configuration);
        addAllowedHeaders(configuration);
        configuration.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private void configureAuthorization(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(securityProperties.getPermitAllPaths().toArray(new String[0])).permitAll();
        configureSharedReadRules(auth);
        configureAdminRules(auth);
        configureUserRules(auth);
        configureLeaderRules(auth);
        auth.anyRequest().authenticated();
    }

    private void configureSharedReadRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.GET, ADMIN_LEADER_SHARED_GET_PATHS).hasAnyRole(ROLE_ADMIN, ROLE_LEADER);
    }

    private void configureAdminRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.GET, ADMIN_ALL_PATHS).hasRole(ROLE_ADMIN);
        auth.requestMatchers(HttpMethod.POST, ADMIN_ALL_PATHS).hasRole(ROLE_ADMIN);
        auth.requestMatchers(HttpMethod.PATCH, ADMIN_ALL_PATHS).hasRole(ROLE_LEADER);
        auth.requestMatchers(HttpMethod.DELETE, ADMIN_ALL_PATHS).hasRole(ROLE_ADMIN);
        auth.requestMatchers(HttpMethod.PUT, ADMIN_ALL_PATHS).hasRole(ROLE_ADMIN);

        auth.requestMatchers(HttpMethod.POST, NOTICE_ADMIN_PATHS).hasRole(ROLE_ADMIN);
        auth.requestMatchers(HttpMethod.DELETE, NOTICE_ADMIN_PATHS).hasRole(ROLE_ADMIN);
    }

    private void configureUserRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.PATCH, USER_PATCH_PATHS).hasRole(ROLE_USER);
        auth.requestMatchers(HttpMethod.GET, USER_GET_PATHS).hasRole(ROLE_USER);
        auth.requestMatchers(HttpMethod.DELETE, USER_EXIT_DELETE_PATHS).hasRole(ROLE_USER);
        auth.requestMatchers(HttpMethod.POST, USER_EXIT_POST_PATHS).hasRole(ROLE_USER);
        auth.requestMatchers(HttpMethod.POST, USER_APPLY_PATHS).hasRole(ROLE_USER);
        auth.requestMatchers(HttpMethod.GET, USER_APPLY_PATHS).hasRole(ROLE_USER);
    }

    private void configureLeaderRules(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth.requestMatchers(HttpMethod.POST, LEADER_ALL_PATHS).hasRole(ROLE_LEADER);
        auth.requestMatchers(HttpMethod.GET, LEADER_ALL_PATHS).hasRole(ROLE_LEADER);
        auth.requestMatchers(HttpMethod.PATCH, LEADER_ALL_PATHS).hasRole(ROLE_LEADER);
        auth.requestMatchers(HttpMethod.DELETE, LEADER_ALL_PATHS).hasRole(ROLE_LEADER);
        auth.requestMatchers(HttpMethod.PUT, LEADER_ALL_PATHS).hasRole(ROLE_LEADER);
    }

    private void addAllowedMethods(CorsConfiguration configuration) {
        for (HttpMethod method : ALLOWED_METHODS) {
            configuration.addAllowedMethod(method);
        }
    }

    private void addAllowedHeaders(CorsConfiguration configuration) {
        for (String header : PERMITTED_HEADERS) {
            configuration.addAllowedHeader(header);
        }
    }

    private static String[] mergePaths(String[] first, String[] second) {
        String[] merged = new String[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

}
