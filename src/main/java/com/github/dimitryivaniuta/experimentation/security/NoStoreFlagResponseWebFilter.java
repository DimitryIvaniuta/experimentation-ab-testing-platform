package com.github.dimitryivaniuta.experimentation.security;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Adds no-store cache headers to personalized flag-assignment responses.
 *
 * <p>Assignments are user-specific and can carry variant payloads. Shared proxies and browsers should not cache them
 * across users, so this filter applies conservative cache headers only to the public flag API path.</p>
 */
@Component
public class NoStoreFlagResponseWebFilter implements WebFilter {

    private static final String FLAG_API_PREFIX = "/api/v1/flags/";

    /**
     * Applies privacy-safe cache headers for flag-assignment responses.
     *
     * @param exchange current HTTP exchange
     * @param chain next web filter chain
     * @return completion signal
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange, final WebFilterChain chain) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        if (path.startsWith(FLAG_API_PREFIX)) {
            HttpHeaders headers = exchange.getResponse().getHeaders();
            headers.setCacheControl(CacheControl.noStore().cachePrivate());
            headers.setPragma("no-cache");
        }
        return chain.filter(exchange);
    }
}
