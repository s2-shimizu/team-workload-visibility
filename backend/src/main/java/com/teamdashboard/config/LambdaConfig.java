package com.teamdashboard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.FormContentFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@Configuration
@Profile("lambda")
public class LambdaConfig {
    
    /**
     * Lambda環境でのFormContentFilterの重複登録を防ぐ
     */
    @Bean
    @ConditionalOnProperty(name = "spring.servlet.multipart.enabled", havingValue = "false", matchIfMissing = true)
    public FilterRegistrationBean<FormContentFilter> disableFormContentFilter() {
        FilterRegistrationBean<FormContentFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FormContentFilter());
        registration.setEnabled(false);
        registration.setName("formContentFilter");
        return registration;
    }
    
    /**
     * Lambda環境でのHiddenHttpMethodFilterの重複登録を防ぐ
     */
    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> disableHiddenHttpMethodFilter() {
        FilterRegistrationBean<HiddenHttpMethodFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HiddenHttpMethodFilter());
        registration.setEnabled(false);
        registration.setName("hiddenHttpMethodFilter");
        return registration;
    }
}