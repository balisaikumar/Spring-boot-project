package com.brinta.hcms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class GlobalCorsConfig {

    private static final Logger logger = LoggerFactory.getLogger(GlobalCorsConfig.class);

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        logger.info("Initializing Global CORS Configuration");

        // Define CORS configuration
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "https://devops.d1zop4g5qvr3ao.amplifyapp.com"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        logger.info("Allowed Origins: {}", config.getAllowedOrigins());
        logger.info("Allowed Methods: {}", config.getAllowedMethods());

        // Register the CORS configuration for all paths
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        // Create and return the filter bean
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(0); // Ensure it's the first filter applied

        logger.info("CORS filter registered successfully with order {}", bean.getOrder());

        return bean;
    }
}
