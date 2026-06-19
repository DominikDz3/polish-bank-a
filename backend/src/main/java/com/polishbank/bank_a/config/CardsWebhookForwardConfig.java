package com.polishbank.bank_a.config;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.util.Set;

@Configuration
public class CardsWebhookForwardConfig {

    private static final Set<String> FORWARDED_PATHS = Set.of("/capture", "/authorize", "/refund");

    @Bean
    public FilterRegistrationBean<Filter> cardsWebhookForwardFilter() {
        FilterRegistrationBean<Filter> reg = new FilterRegistrationBean<>();
        reg.setFilter((request, response, chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            String path = req.getRequestURI();
            if ("POST".equalsIgnoreCase(req.getMethod()) && FORWARDED_PATHS.contains(path)) {
                req.getRequestDispatcher("/api/webhooks/cards" + path).forward(request, response);
                return;
            }
            chain.doFilter(request, response);
        });
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        reg.addUrlPatterns("/capture", "/authorize", "/refund");
        return reg;
    }
}