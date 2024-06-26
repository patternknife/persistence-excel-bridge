package io.github.patternknife.pxbsample.config.security.provider.resource.introspector;


import io.github.patternknife.pxbsample.config.response.error.exception.auth.UnauthenticatedException;
import io.github.patternknife.pxbsample.config.response.error.message.SecurityExceptionMessage;
import io.github.patternknife.pxbsample.config.security.serivce.persistence.authorization.OAuth2AuthorizationServiceImpl;
import io.github.patternknife.pxbsample.config.security.serivce.userdetail.ConditionalDetailsService;
import io.github.patternknife.pxbsample.config.security.principal.AccessTokenUserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.introspection.NimbusOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;


public class JpaTokenStoringOauth2TokenIntrospector implements OpaqueTokenIntrospector {


    private OpaqueTokenIntrospector delegate =
            new NimbusOpaqueTokenIntrospector(
                    "http://localhost:8300/oauth2/introspect",
                    "barClient",
                    "barClientSecret"
            );

    private final OAuth2AuthorizationServiceImpl authorizationService;
    private final ConditionalDetailsService conditionalDetailsService;


    public JpaTokenStoringOauth2TokenIntrospector(OAuth2AuthorizationServiceImpl authorizationService,
                                                  ConditionalDetailsService conditionalDetailsService) {
        this.authorizationService = authorizationService;
        this.conditionalDetailsService = conditionalDetailsService;
    }

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {

/*        try {
            OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
            return principal;
        }catch (Exception e){
            //throw e;
            throw new UnauthenticatedException(e.getMessage());
        }*/

        OAuth2Authorization oAuth2Authorization = authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);

        if(oAuth2Authorization == null || oAuth2Authorization.getAccessToken() == null || oAuth2Authorization.getAccessToken().isExpired()
                || oAuth2Authorization.getRefreshToken() == null || oAuth2Authorization.getRefreshToken().isExpired()){
            throw new UnauthenticatedException(SecurityExceptionMessage.AUTHENTICATION_FAILURE.getMessage());
            //return null;
        }

        return (AccessTokenUserInfo) conditionalDetailsService.loadUserByUsername(oAuth2Authorization.getPrincipalName(), (String) oAuth2Authorization.getAttributes().get("client_id"));
    }
}