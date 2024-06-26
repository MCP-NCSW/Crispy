package com.mcp.crispy.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/crispy/main");
        registry.addRedirectViewController("/crispy", "/crispy/main");
        registry.addRedirectViewController("/crispy/", "/crispy/main");
        registry.addRedirectViewController("/CRISPY", "/crispy/main");
        registry.addRedirectViewController("/CRISPY/", "/crispy/main");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/profiles/**")
                .addResourceLocations("file:///Users/baeyeong-ug/Desktop/image/profiles/")
                .addResourceLocations("file:///C:/GDJ77/mcp/crispy_img/emp_profile/");
        registry.addResourceHandler("/franchise/**")
                .addResourceLocations("file:///Users/baeyeong-ug/Desktop/image/profiles/");
        registry.addResourceHandler("/crispy_img/**")
                .addResourceLocations("file:///C:/GDJ77/mcp/crispy_img/")
                .addResourceLocations("file:///Users/baeyeong-ug/Desktop/image/");
        registry.addResourceHandler("/emp_sign/**")
                .addResourceLocations("file:///C:/GDJ77/mcp/crispy_img/emp_sign/")
                .addResourceLocations("file:///Users/baeyeong-ug/Desktop/image/signatures/");
        registry.addResourceHandler("/appr_file/**")
                .addResourceLocations("file:///C:/GDJ77/mcp/crispy_img/appr_file/");
    }
}