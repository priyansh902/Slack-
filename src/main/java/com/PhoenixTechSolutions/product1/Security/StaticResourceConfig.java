package com.PhoenixTechSolutions.product1.Security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;





@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
   
    @Value("${file.upload-dir:/uploads/resumes}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("api/resumes/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

}
