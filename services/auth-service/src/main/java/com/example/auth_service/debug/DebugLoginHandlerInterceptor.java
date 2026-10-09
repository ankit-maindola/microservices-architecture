package com.example.auth_service.debug;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DebugLoginHandlerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        if (("POST".equalsIgnoreCase(request.getMethod()))
                && (request.getRequestURI().contains("/api/auth/public/login")
                || request.getRequestURI().contains("/api/auth/session/login"))) {
            // #region agent log
            DebugLog.write(
                    "H1-verify",
                    "DebugLoginHandlerInterceptor:preHandle",
                    "MVC routed POST to controller handler",
                    "{\"uri\":\"" + request.getRequestURI() + "\",\"handler\":\""
                            + handler.getClass().getSimpleName()
                            + "\",\"contentType\":\"" + request.getContentType() + "\"}"
            );
            // #endregion
        }
        return true;
    }
}
