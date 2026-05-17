package com.github.dimitryivaniuta.experimentation.security;

import com.github.dimitryivaniuta.experimentation.config.ExperimentationProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Lightweight admin protection for local/simple deployments.
 *
 * <p>Real production deployments should normally replace this filter with OAuth2/OIDC, mTLS, or an API gateway
 * policy. The filter still avoids accidental unauthenticated writes in this standalone project.</p>
 */
@Component
@RequiredArgsConstructor
public class AdminTokenWebFilter implements WebFilter {

    private static final String ADMIN_TOKEN_HEADER = "X-Admin-Token";
    private final ExperimentationProperties properties;

    /**
     * Rejects unsafe admin requests when the expected static admin token is missing.
     *
     * @param exchange current HTTP exchange
     * @param chain next filter chain
     * @return completion signal
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!requiresAdminToken(request)) {
            return chain.filter(exchange);
        }
        String suppliedToken = request.getHeaders().getFirst(ADMIN_TOKEN_HEADER);
        if (isSameToken(suppliedToken, properties.adminToken())) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean requiresAdminToken(final ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        return path.startsWith("/api/v1/experiments")
                && !"GET".equalsIgnoreCase(request.getMethod().name());
    }

    private boolean isSameToken(final String suppliedToken, final String expectedToken) {
        if (suppliedToken == null || expectedToken == null) {
            return false;
        }
        return MessageDigest.isEqual(
                suppliedToken.getBytes(StandardCharsets.UTF_8),
                expectedToken.getBytes(StandardCharsets.UTF_8)
        );
    }
}
