package com.brinta.hcms.filter;

import com.brinta.hcms.exception.exceptionHandler.JwtExpiredException;
import com.brinta.hcms.exception.exceptionHandler.JwtInvalidException;
import com.brinta.hcms.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (shouldSkipAuthentication(path, contextPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = getJwtFromRequest(request);

        try {
            // Validate the token (throws JwtExpiredException or JwtInvalidException if invalid)
            jwtService.isValidToken(jwt);

            username = jwtService.extractUsernameFromToken(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.validateTokenForUser(jwt, userDetails)) {
                    Claims claims = jwtService.extractAllClaims(jwt);
                    List<String> roles = claims.get("roles", List.class);

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    // Token doesn't match user
                    sendErrorResponse(response, "Invalid token", HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            }

            filterChain.doFilter(request, response);

        } catch (JwtExpiredException ex) {
            sendErrorResponse(response, "Token expired", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (JwtInvalidException ex) {
            sendErrorResponse(response, "Invalid token", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception ex) {
            sendErrorResponse(response, "Something went wrong",
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendErrorResponse(HttpServletResponse response,
                                   String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        return authHeader.substring(7);
    }

    private Boolean shouldSkipAuthentication(String path, String contextPath) {
        return path.equals(contextPath + "user/userAuthenticate")
                || path.equalsIgnoreCase(contextPath + "/user/logoutUserFromConcurrentSession")
                || path.startsWith(contextPath + "/swagger-ui")
                || path.startsWith(contextPath + "/api/auth/register")
                || path.startsWith(contextPath + "/api/auth/login")
                || path.startsWith(contextPath + "/health/check");
    }

}
