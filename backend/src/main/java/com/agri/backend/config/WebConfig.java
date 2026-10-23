package com.agri.backend.config;

import com.agri.backend.security.AuthInterceptor;
import com.agri.backend.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors-origins:*}")
    private String corsOrigins;

    private final AuthInterceptor authInterceptor;
    private final ImageStorageService imageStorage;

    public WebConfig(AuthInterceptor authInterceptor, ImageStorageService imageStorage) {
        this.authInterceptor = authInterceptor;
        this.imageStorage = imageStorage;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
    }

    /** Serve seller product photos and crop-dataset photos publicly. Soil reports / land photos are NOT served. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String base = imageStorage.getRoot().toUri().toString(); // ends with "/" because the folder exists
        registry.addResourceHandler("/uploads/listings/**").addResourceLocations(base + "listings/");
        registry.addResourceHandler("/uploads/crops/**").addResourceLocations(base + "crops/");
    }

    /** CORS_ORIGINS = comma separated frontend URLs (for example https://agrisetu.azurestaticapps.net). "*" allows any site. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsOrigins.split("\\s*,\\s*"))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
