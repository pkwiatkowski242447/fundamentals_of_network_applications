package pl.pas.gr3.cinema.security.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pl.pas.gr3.cinema.security.services.JWSService;

import java.io.IOException;

@RequiredArgsConstructor
public class UserUpdateFilter extends OncePerRequestFilter {

    private final JWSService jwsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String ifMatchHeader = request.getHeader(HttpHeaders.IF_MATCH);
        if (ifMatchHeader == null || ifMatchHeader.isEmpty()) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(HttpStatus.PRECONDITION_FAILED.value());
            httpServletResponse.getWriter().write("If-Match header content is missing.");
            return;
        }
        String currentUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!jwsService.extractUsernameFromSignature(ifMatchHeader.replace("\"", "")).equals(currentUserName)) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(HttpStatus.FORBIDDEN.value());
            httpServletResponse.getWriter().write("Modifing other user data is forbidden.");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
