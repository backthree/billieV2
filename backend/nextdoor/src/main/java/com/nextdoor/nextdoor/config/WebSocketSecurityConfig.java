package com.nextdoor.nextdoor.config;

import com.nextdoor.nextdoor.domain.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.messaging.access.intercept.MessageAuthorizationContext;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.function.Supplier;

@RequiredArgsConstructor
@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    private final PathPattern pattern = new PathPatternParser().parse(
            "/topic/rental-reservation/{uuid}/**"
    );
    private final JwtProvider jwtProvider;

    @Bean
    public AuthorizationManager<Message<?>> messageAuthorizationManager(MessageMatcherDelegatingAuthorizationManager.Builder builder) {
        return builder
                .simpTypeMatchers(SimpMessageType.SUBSCRIBE).access(this::checkDestination)
                .build();
    }

    private AuthorizationDecision checkDestination(Supplier<Authentication> authentication, MessageAuthorizationContext<?> object) {
        String destination = object.getMessage().getHeaders().get("destination", String.class);
        if (destination == null) {
            return new AuthorizationDecision(false);
        }
        PathContainer pathContainer = PathContainer.parsePath(destination);
        if (!pattern.matches(pathContainer)) {
            return new AuthorizationDecision(true);
        }
        String destinationUuid = pattern.matchAndExtract(pathContainer).getUriVariables().get("uuid");
        String token = object.getMessage().getHeaders().get("authorization", String.class);
        if (!destinationUuid.equals(jwtProvider.validateAndGetUuid(token))) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(true);
    }
}
