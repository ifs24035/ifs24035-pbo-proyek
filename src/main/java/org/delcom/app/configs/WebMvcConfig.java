package org.delcom.app.configs;

import org.delcom.app.interceptors.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**") // Terapkan ke semua endpoint /api
                .excludePathPatterns("/api/auth/**") // Kecuali endpoint auth
                .excludePathPatterns("/api/public/**"); // Dan endpoint public
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    // Agar file di folder uploads bisa diakses via URL /uploads/...
    registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/");
    }
}