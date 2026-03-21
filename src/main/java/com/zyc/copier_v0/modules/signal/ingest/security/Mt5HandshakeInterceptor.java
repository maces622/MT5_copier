package com.zyc.copier_v0.modules.signal.ingest.security;

import com.zyc.copier_v0.modules.signal.ingest.config.Mt5SignalIngestProperties;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class Mt5HandshakeInterceptor implements HandshakeInterceptor {

    public static final String TRACE_ID_ATTR = "traceId";

    private static final Logger log = LoggerFactory.getLogger(Mt5HandshakeInterceptor.class);

    private final Mt5SignalIngestProperties properties;

    public Mt5HandshakeInterceptor(Mt5SignalIngestProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String traceId = UUID.randomUUID().toString();
        attributes.put(TRACE_ID_ATTR, traceId);

        String token = resolveToken(request);
        if (token == null || !secureEquals(token, properties.getBearerToken())) {
            log.warn(
                    "Reject MT5 websocket handshake, traceId={}, remote={}, uri={}, authHeaderPresent={}, queryTokenPresent={}, resolvedTokenFingerprint={}, expectedTokenFingerprint={}",
                    traceId,
                    request.getRemoteAddress(),
                    request.getURI(),
                    hasAuthorizationHeader(request),
                    hasQueryToken(request),
                    fingerprint(token),
                    fingerprint(properties.getBearerToken())
            );
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        List<String> authorizationHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authorizationHeaders != null) {
            for (String header : authorizationHeaders) {
                if (header != null && header.startsWith("Bearer ")) {
                    return header.substring("Bearer ".length()).trim();
                }
            }
        }

        if (!properties.isAllowQueryToken()) {
            return null;
        }

        URI uri = request.getURI();
        String query = resolveQuery(uri);
        if (query == null || query.isBlank()) {
            return null;
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && ("access_token".equals(parts[0]) || "token".equals(parts[0])) && !parts[1].isBlank()) {
                return urlDecode(parts[1]);
            }
        }

        return null;
    }

    private boolean hasAuthorizationHeader(ServerHttpRequest request) {
        List<String> authorizationHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        return authorizationHeaders != null && !authorizationHeaders.isEmpty();
    }

    private boolean hasQueryToken(ServerHttpRequest request) {
        String query = resolveQuery(request.getURI());
        return query != null && (query.contains("access_token=") || query.contains("token="));
    }

    private String resolveQuery(URI uri) {
        String query = uri.getQuery();
        if (query != null) {
            return query;
        }

        String raw = uri.toString();
        int index = raw.indexOf('?');
        if (index < 0 || index == raw.length() - 1) {
            return null;
        }
        return raw.substring(index + 1);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String fingerprint(String token) {
        if (token == null || token.isBlank()) {
            return "null";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                builder.append(String.format("%02x", hash[i]));
            }
            return builder.toString();
        } catch (Exception ex) {
            return "hash-error";
        }
    }

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
