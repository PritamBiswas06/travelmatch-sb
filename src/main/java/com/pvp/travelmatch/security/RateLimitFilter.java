package com.pvp.travelmatch.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight per-process abuse throttling for small deployments. Use gateway/WAF limits too when horizontally scaled. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Rule(int maxRequests, Duration window) {}
    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry("POST:/api/auth/login", new Rule(10, Duration.ofMinutes(15))),
            Map.entry("POST:/api/auth/register", new Rule(5, Duration.ofHours(1))),
            Map.entry("POST:/api/auth/verify", new Rule(10, Duration.ofMinutes(15))),
            Map.entry("POST:/api/auth/resend-otp", new Rule(3, Duration.ofMinutes(15))),
            Map.entry("POST:/api/auth/forgot-password", new Rule(5, Duration.ofHours(1))),
            Map.entry("POST:/api/auth/reset-password", new Rule(10, Duration.ofMinutes(15))),
            Map.entry("POST:/api/reports/", new Rule(5, Duration.ofHours(1))),
            Map.entry("POST:/api/chat/send/", new Rule(30, Duration.ofMinutes(1))),
            Map.entry("POST:/api/match/send/", new Rule(20, Duration.ofHours(1))),
            Map.entry("POST:/api/travel", new Rule(10, Duration.ofHours(1))),
            Map.entry("POST:/api/trust/phone/", new Rule(3, Duration.ofMinutes(15))),
            Map.entry("POST:/api/trust/selfie-review", new Rule(3, Duration.ofHours(1))),
            Map.entry("POST:/api/trust/photo-review/start", new Rule(3, Duration.ofHours(1)))
    );
    private final ConcurrentHashMap<String, Deque<Long>> requests = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        String keyPath = request.getMethod() + ":" + request.getRequestURI();
        Rule rule = ruleFor(keyPath);
        if (rule == null) {
            chain.doFilter(request, response);
            return;
        }
        String key = request.getRemoteAddr() + ":" + ruleKey(keyPath);
        long now = System.currentTimeMillis();
        Deque<Long> times = requests.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (times) {
            long cutoff = now - rule.window().toMillis();
            while (!times.isEmpty() && times.peekFirst() < cutoff) times.removeFirst();
            if (times.size() >= rule.maxRequests()) {
                long retrySeconds = Math.max(1, (times.peekFirst() + rule.window().toMillis() - now + 999) / 1000);
                response.setStatus(429);
                response.setHeader("Retry-After", String.valueOf(retrySeconds));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Too many requests. Please wait before trying again.\"}");
                return;
            }
            times.addLast(now);
        }
        // Prune stale buckets if many distinct client addresses have appeared.
        if (requests.size() > 10000) {
            long staleBefore = now - Duration.ofHours(2).toMillis();
            requests.entrySet().removeIf(entry -> {
                Deque<Long> bucket = entry.getValue();
                synchronized (bucket) {
                    return bucket.isEmpty() || bucket.peekLast() < staleBefore;
                }
            });
        }
        chain.doFilter(request, response);
    }

    private Rule ruleFor(String key) {
        Rule exact = RULES.get(key);
        if (exact != null) return exact;
        for (var entry : RULES.entrySet()) {
            if (entry.getKey().endsWith("/") && key.startsWith(entry.getKey())) return entry.getValue();
        }
        return null;
    }

    private String ruleKey(String key) {
        for (String prefix : RULES.keySet()) {
            if (prefix.endsWith("/") && key.startsWith(prefix)) return prefix;
        }
        return key;
    }
}
