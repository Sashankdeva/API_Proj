package com.squadup.config;

import com.squadup.security.JsonSecurityErrorHandler;
import com.squadup.security.TokenAuthenticationFilter;
import com.squadup.service.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Securing the Spring REST endpoints.
 *
 * <ul>
 *   <li>Stateless: no HTTP session; every API call carries {@code Authorization: Bearer <token>}.</li>
 *   <li>Public: the static web UI, register/login, the SOAP service, the WebSocket handshake
 *       (STOMP frames are authenticated separately) and the H2 console.</li>
 *   <li>Everything else under {@code /api/**} requires a logged-in player.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenService tokenService,
                                                   JsonSecurityErrorHandler errorHandler) throws Exception {
        http
                // Tokens travel in a header, not a cookie, so CSRF protection is not needed.
                .csrf(csrf -> csrf.disable())
                // Uses the CORS rules from WebConfig#addCorsMappings.
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Lets the H2 console render its frames.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/*.html", "/*.js", "/*.css", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/soap/**", "/ws/**", "/h2-console/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errorHandler)
                        .accessDeniedHandler(errorHandler))
                .addFilterBefore(new TokenAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Spring Security rejects ';' in URLs by default. Matrix variables
     * ({@code /api/v1/squads/browse/filter;game=Valorant}) need it.
     */
    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        return firewall;
    }
}
