package com.brinta.hcms.exception;
import jakarta.servlet.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        Throwable cause = (Throwable) request.getAttribute("authException");

        if (cause instanceof JwtExpiredException) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expired"); // 401
        } else if (cause instanceof JwtInvalidException) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token invalid"); // 403
        } else {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"); // 401
        }
    }

}
