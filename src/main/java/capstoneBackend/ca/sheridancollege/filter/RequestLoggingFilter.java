package capstoneBackend.ca.sheridancollege.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        log.info(">>> INCOMING REQUEST: {} {}", req.getMethod(), req.getRequestURI());

        chain.doFilter(request, response);

        HttpServletResponse res = (HttpServletResponse) response;
        log.info("<<< RESPONSE STATUS: {} {}", res.getStatus(), req.getRequestURI());
    }

    @Override
    public void destroy() {}
}
