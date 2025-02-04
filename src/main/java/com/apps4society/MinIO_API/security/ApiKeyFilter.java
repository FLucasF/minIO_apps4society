package com.apps4society.MinIO_API.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import java.io.IOException;
import java.util.Collections;

@Component
public class ApiKeyFilter extends GenericFilterBean {

    @Value("${app.api.key}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestUri = httpRequest.getRequestURI();

        // 🔹 Permitir acesso irrestrito ao Swagger e outras rotas públicas
        if (requestUri.startsWith("/swagger-ui") ||
                requestUri.startsWith("/v3/api-docs") ||
                requestUri.startsWith("/webjars") ||
                requestUri.startsWith("/error")) {
            chain.doFilter(request, response);
            return;
        }

        // 🔐 Validação da API Key para todas as outras requisições
        String requestApiKey = httpRequest.getHeader("api-key");

        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "API Key inválida ou ausente.");
            return;
        }

        User authenticatedUser = new User("api-key-user", "", Collections.emptyList());
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        chain.doFilter(request, response);
    }
}
