package io.github.patternknife.pxbsample.domain.traditionaloauth.service;

import io.github.patternknife.pxbsample.domain.traditionaloauth.bo.BasicTokenResolver;
import io.github.patternknife.pxbsample.domain.traditionaloauth.dto.SpringSecurityTraditionalOauthDTO;
import io.github.patternknife.pxbsample.config.logger.module.NonStopErrorLogConfig;
import io.github.patternknife.pxbsample.config.response.error.exception.auth.UnauthenticatedException;
import io.github.patternknife.pxbsample.config.response.error.exception.auth.UnauthorizedException;
import io.github.patternknife.pxbsample.config.response.error.message.SecurityExceptionMessage;
import io.github.patternknife.pxbsample.config.security.OAuth2ClientCachedInfo;
import io.github.patternknife.pxbsample.config.security.serivce.CommonOAuth2AuthorizationCycle;
import io.github.patternknife.pxbsample.config.security.serivce.Oauth2AuthenticationHashCheckService;
import io.github.patternknife.pxbsample.config.security.serivce.persistence.authorization.OAuth2AuthorizationServiceImpl;
import io.github.patternknife.pxbsample.config.security.serivce.userdetail.ConditionalDetailsService;
import io.github.patternknife.pxbsample.config.security.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Service
public class TraditionalOauthService {

    private static final Logger logger = LoggerFactory.getLogger(NonStopErrorLogConfig.class);

    private final RegisteredClientRepository registeredClientRepository;

    private final OAuth2AuthorizationServiceImpl authorizationService;

    private final ConditionalDetailsService conditionalDetailsService;

    private final CommonOAuth2AuthorizationCycle commonOAuth2AuthorizationCycle;
    private final Oauth2AuthenticationHashCheckService oauth2AuthenticationHashCheckService;


    public TraditionalOauthService(RegisteredClientRepository registeredClientRepository,
                                   OAuth2AuthorizationServiceImpl authorizationService,
                                   ConditionalDetailsService conditionalDetailsService,
                                   CommonOAuth2AuthorizationCycle commonOAuth2AuthorizationCycle,
                                   Oauth2AuthenticationHashCheckService oauth2AuthenticationHashCheckService) {

        this.registeredClientRepository = registeredClientRepository;
        this.authorizationService = authorizationService;
        this.conditionalDetailsService = conditionalDetailsService;

        this.commonOAuth2AuthorizationCycle = commonOAuth2AuthorizationCycle;
        this.oauth2AuthenticationHashCheckService = oauth2AuthenticationHashCheckService;

    }


    public SpringSecurityTraditionalOauthDTO.TokenResponse createAccessToken(SpringSecurityTraditionalOauthDTO.TokenRequest accessTokenRequest,
                                                                             String authorizationHeader) {

        BasicTokenResolver.BasicCredentials basicCredentials = BasicTokenResolver.parse(authorizationHeader).orElseThrow(UnauthorizedException::new);

        UserDetails userDetails = conditionalDetailsService.loadUserByUsername(accessTokenRequest.getUsername(), basicCredentials.getClientId());

        oauth2AuthenticationHashCheckService.validateUsernamePassword(accessTokenRequest.getPassword(), userDetails);

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

        OAuth2Authorization oAuth2Authorization = commonOAuth2AuthorizationCycle.run(userDetails,
                new AuthorizationGrantType(accessTokenRequest.getGrant_type()), basicCredentials.getClientId(), SecurityUtil.getTokenUsingSecurityAdditionalParameters(request), null);

        Instant now = Instant.now();
        Instant expiresAt = oAuth2Authorization.getAccessToken().getToken().getExpiresAt();
        int accessTokenRemainingSeconds = Math.toIntExact(Duration.between(now, expiresAt).getSeconds());

        return new SpringSecurityTraditionalOauthDTO.TokenResponse(
                oAuth2Authorization.getAccessToken().getToken().getTokenValue(), OAuth2AccessToken.TokenType.BEARER.getValue(), Objects.requireNonNull(oAuth2Authorization.getRefreshToken()).getToken().getTokenValue(),
                accessTokenRemainingSeconds,
                String.join(" ", Objects.requireNonNull(OAuth2ClientCachedInfo.getScopeByValue(basicCredentials.getClientId()))));
    }

    public SpringSecurityTraditionalOauthDTO.TokenResponse refreshAccessToken(SpringSecurityTraditionalOauthDTO.TokenRequest refreshTokenRequest,
                                                                              String authorizationHeader) throws IOException {

        BasicTokenResolver.BasicCredentials basicCredentials = BasicTokenResolver.parse(authorizationHeader).orElseThrow(()-> new UnauthorizedException(SecurityExceptionMessage.AUTHORIZATION_ERROR.getMessage()));

        RegisteredClient registeredClient = registeredClientRepository.findByClientId(basicCredentials.getClientId());

        assert registeredClient != null;

        if(!(basicCredentials.getClientId().equals(registeredClient.getClientId())
                && oauth2AuthenticationHashCheckService.validateClientCredentials(basicCredentials.getClientSecret(), registeredClient))) {
            throw new UnauthorizedException(SecurityExceptionMessage.AUTHORIZATION_ERROR.getMessage());
        }

        OAuth2Authorization oAuth2Authorization = authorizationService.findByToken(refreshTokenRequest.getRefresh_token(), OAuth2TokenType.REFRESH_TOKEN);

        UserDetails userDetails;
        if (oAuth2Authorization == null || oAuth2Authorization.getRefreshToken() == null) {
            throw new UnauthenticatedException(SecurityExceptionMessage.AUTHENTICATION_ERROR.getMessage());
        }else{
            userDetails = conditionalDetailsService.loadUserByUsername(oAuth2Authorization.getPrincipalName(), registeredClient.getClientId());
        }

        Map<String, Object> modifiableAdditionalParameters = new HashMap<>();
        modifiableAdditionalParameters.put("refresh_token", refreshTokenRequest.getRefresh_token());

        oAuth2Authorization = commonOAuth2AuthorizationCycle.run(userDetails,
                new AuthorizationGrantType(refreshTokenRequest.getGrant_type()),
                basicCredentials.getClientId(), oAuth2Authorization.getAttributes(), modifiableAdditionalParameters);


        Instant now = Instant.now();
        Instant expiresAt = oAuth2Authorization.getRefreshToken().getToken().getExpiresAt();
        int refreshTokenRemainingSeconds = Math.toIntExact(Duration.between(now, expiresAt).getSeconds());

        return new SpringSecurityTraditionalOauthDTO.TokenResponse(
                oAuth2Authorization.getAccessToken().getToken().getTokenValue(), OAuth2AccessToken.TokenType.BEARER.getValue(), Objects.requireNonNull(oAuth2Authorization.getRefreshToken()).getToken().getTokenValue(),
                refreshTokenRemainingSeconds,
                String.join(" ", registeredClient.getScopes()));
    }

}
