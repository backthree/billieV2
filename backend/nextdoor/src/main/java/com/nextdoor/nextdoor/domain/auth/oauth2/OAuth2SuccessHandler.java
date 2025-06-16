package com.nextdoor.nextdoor.domain.auth.oauth2;

import com.nextdoor.nextdoor.domain.auth.CustomOAuth2User;
import com.nextdoor.nextdoor.domain.auth.exception.InvalidRedirectUrlException;
import com.nextdoor.nextdoor.domain.auth.port.AuthFintechQueryPort;
import com.nextdoor.nextdoor.domain.auth.port.AuthMemberQueryPort;
import com.nextdoor.nextdoor.domain.auth.service.JwtProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.nextdoor.nextdoor.domain.auth.filter.RedirectUrlCookieFilter.REDIRECT_URI_PARAM;

@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final List<String> allowedRedirectUrls = List.of(
            "http://localhost:3000",
            "http://localhost:8081",
            "http://localhost:8082",
            "http://k12e205.p.ssafy.io",
            "http://k12e205.p.ssafy.io:3000",
            "http://k12e205.p.ssafy.io:8081",
            "http://k12e205.p.ssafy.io:8082"
    );

    private final OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository;

    private final JwtProvider jwtProvider;

    private final AuthMemberQueryPort authMemberQueryPort;
    private final AuthFintechQueryPort authFintechQueryPort;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String accessToken = jwtProvider.createAccessToken(authentication);
        Optional<Cookie> oCookie = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(REDIRECT_URI_PARAM)).findFirst();
        Optional<String> redirectUrl = oCookie.map(Cookie::getValue);
        redirectUrl.ifPresent(this::validateRedirectUrl);
        Long userId = Long.parseLong(((CustomOAuth2User) authentication.getPrincipal()).getName());
        response.sendRedirect(
                redirectUrl.orElseThrow(() -> new InvalidRedirectUrlException("Redirect URL이 유효하지 않습니다."))
                + "/social-login?accessToken=" + accessToken +
                "&userKey=" + authFintechQueryPort.findByUserId(userId).getUserKey() +
                "&uuid=" + authMemberQueryPort.findById(userId).orElseThrow().getUuid());
        clearAuthenticationAttributes(request, response);
    }

    private void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        oAuth2AuthorizationRequestBasedOnCookieRepository.removeAuthorizationRequestCookies(request, response);
    }

    private void validateRedirectUrl(String url) {
        for (String allowedRedirectUrl : allowedRedirectUrls) {
            if (url.startsWith(allowedRedirectUrl)) {
                return;
            }
        }
        throw new InvalidRedirectUrlException("Redirect URL이 유효하지 않습니다.");
    }
}
