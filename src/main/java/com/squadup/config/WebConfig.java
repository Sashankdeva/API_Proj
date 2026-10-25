package com.squadup.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC customisation: CORS and the request-logging interceptor. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RequestTraceInterceptor requestTraceInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(RequestTraceInterceptor requestTraceInterceptor,
                     @Value("${squadup.cors.allowed-origins:}") String[] allowedOrigins) {
        this.requestTraceInterceptor = requestTraceInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Enabling CORS in Spring REST: lets the UI call the API when it is opened from another
     * origin, e.g. VS Code Live Server. Credentials are not needed because the token travels
     * in the Authorization header, so any origin can be allowed.
     *
     * <p>The origins come from {@code squadup.cors.allowed-origins}. The page is served by this
     * application, so the default (empty) allows same-origin calls only; the {@code dev} profile
     * allows every origin.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins()   // clears the registry's allow-all default
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Authorization", "X-API-Version")
                .exposedHeaders("X-Request-Id", "Location")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor).addPathPatterns("/api/**");
    }
}
