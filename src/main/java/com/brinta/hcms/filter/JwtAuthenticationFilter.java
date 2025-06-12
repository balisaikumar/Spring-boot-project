package com.brinta.hcms.filter;

import com.brinta.hcms.exception.JwtExpiredException;
import com.brinta.hcms.exception.JwtInvalidException;
import com.brinta.hcms.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal
            (HttpServletRequest request,
             HttpServletResponse response,
             FilterChain filterChain) throws ServletException, IOException {
        // Intercept the request
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();

        //Skip Authentication for Public endpoints
        if(shouldSkipAuthentication(path,contextPath)){
            logger.info(path);
            filterChain.doFilter(request,response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = getJwtFromRequest(request);
        try{
            // check if the token is valid
            if(!jwtService.isValidToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            username = jwtService.extractUsernameFromToken(jwt);

            if(username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if(jwtService.validateTokenForUser(jwt, userDetails)) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                filterChain.doFilter(request, response);
            }
        }catch (JwtExpiredException | JwtInvalidException e) {
            // ⚠️ Attach the cause to the request
            request.setAttribute("authException", e);
            // 🔁 Forward to the entry point (authentication failure handler)
            SecurityContextHolder.clearContext();
            // Let Spring Security handle it
            request.getRequestDispatcher("/error").forward(request, response);
        }

    }

    private String getJwtFromRequest(HttpServletRequest request) {
        final String authHeader = request.getHeader("Authorization");
        // Bearer <token>
        return authHeader.substring(7);
    }
    private  Boolean shouldSkipAuthentication(String path,String contextPath){
        return path.equals(contextPath +"user/userAuthenticate")
                || path.equalsIgnoreCase(contextPath + "/user/logoutUserFromConcurrentSession")
                || path.startsWith(contextPath + "/swagger-ui")
                || path.startsWith(contextPath + "/api/auth/register")
                || path.startsWith(contextPath + "/api/auth/login");
    }
}
