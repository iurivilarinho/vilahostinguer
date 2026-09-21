package com.bancada.config;

import java.io.IOException;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestLoggingInterceptor requestLoggingInterceptor;

    public WebMvcConfig(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestLoggingInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns("/api/events", "/api/devices/*/metrics");
    }

    /**
     * Bundles under /assets carry a hash and are cached forever. Everything else is the React app:
     * unknown paths fall back to index.html so the router can handle them, and index.html is never
     * cached, since it points to the current bundle and a stale copy keeps loading the old one.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());

        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.noStore())
            .resourceChain(false)
            .addResolver(new PathResourceResolver() {
                @Override
                protected Resource getResource(String resourcePath, Resource location) throws IOException {
                    if (resourcePath.startsWith("api/") || resourcePath.startsWith("ws/") || resourcePath.startsWith("swagger")
                        || resourcePath.startsWith("v3/")) {
                        return null;
                    }
                    Resource requested = location.createRelative(resourcePath);
                    if (requested.exists() && requested.isReadable()) {
                        return requested;
                    }
                    if (resourcePath.startsWith("portal")) {
                        // the customer panel never falls back to the administrator page
                        return null;
                    }
                    Resource index = new ClassPathResource("/static/index.html");
                    return index.exists() ? index : null;
                }
            });
    }
}
