package com.GASB.google_drive_func.annotation.JWT;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Aspect
@Component
@Slf4j
public class JWTValidationAspect {

    private final HttpServletRequest servletRequest;
    private final RestTemplate restTemplate;

    @Autowired
    public JWTValidationAspect(HttpServletRequest servletRequest, RestTemplate restTemplate) {
        this.servletRequest = servletRequest;
        this.restTemplate = restTemplate;
    }

    @Value("${auth.server.url}")
    private String authServerUrl;

    @Before("@annotation(com.GASB.slack_func.annotation.JWT.ValidateJWT)")
    public void validateJWT() {
        Cookie[] cookies = servletRequest.getCookies();
        String jwtToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    jwtToken = cookie.getValue();
                    break;
                }
            }
        }

        if (jwtToken == null) {
            servletRequest.setAttribute("error", "JWT token not found in cookies");
            return;
        }

        String email = validateRequest(jwtToken);

        if (email != null) {
            servletRequest.setAttribute("email", email);
        }
    }

    private String validateRequest(String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + jwtToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<JwtValidationResponse> response = restTemplate.postForEntity(authServerUrl, entity, JwtValidationResponse.class);
            log.info("Received response from auth server: {}", response);

            JwtValidationResponse jwtValidationResponse = response.getBody();
            log.info("JWT validation response: {}", jwtValidationResponse);
            if (jwtValidationResponse == null || !"OK".equals(jwtValidationResponse.getStatus())) {
                servletRequest.setAttribute("error", "JWT token is invalid or the validation response is null");
                return null;
            }
            return jwtValidationResponse.getEmail();
        } catch (HttpClientErrorException e) {
            log.error("JWT token validation failed: {} - Response body: {}", e.getMessage(), e.getResponseBodyAsString());
            servletRequest.setAttribute("error", "JWT token validation failed: " + e.getMessage());
            return null;
        }
    }
}
