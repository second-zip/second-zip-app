package com.secondzip.backend.report.service.external.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Base64;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodefTokenProviderTest {

    private static final String TOKEN_URL = "https://oauth.codef.io/oauth/token";

    private MockRestServiceServer server;
    private CodefTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        provider = new CodefTokenProvider(restTemplate);
        ReflectionTestUtils.setField(provider, "clientId", "client-id");
        ReflectionTestUtils.setField(provider, "clientSecret", "client-secret");
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void requestsTokenWithBasicAuthAndCachesIt() {
        String basicAuth = Base64.getEncoder().encodeToString(
                "client-id:client-secret".getBytes(StandardCharsets.UTF_8)
        );
        server.expect(tokenRequest())
                .andExpect(request -> {
                    assertThat(request.getHeaders().getFirst("Authorization"))
                            .isEqualTo("Basic " + basicAuth);
                    assertThat(((MockClientHttpRequest) request).getBodyAsString())
                            .isEqualTo("grant_type=client_credentials&scope=read");
                })
                .andRespond(withSuccess(
                        "{\"access_token\":\"token-1\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON
                ));

        assertThat(provider.getToken()).isEqualTo("token-1");
        assertThat(provider.getToken()).isEqualTo("token-1");
    }

    @Test
    void invalidateForcesAFreshTokenRequest() {
        expectToken("token-1", "120");
        expectToken("token-2", "120");

        assertThat(provider.getToken()).isEqualTo("token-1");
        provider.invalidate();
        assertThat(provider.getToken()).isEqualTo("token-2");
    }

    @Test
    void expiredCachedTokenIsReplaced() {
        ReflectionTestUtils.setField(provider, "cachedToken", "expired");
        ReflectionTestUtils.setField(provider, "expiresAtEpochMillis", 0L);
        expectToken("fresh", "120");

        assertThat(provider.getToken()).isEqualTo("fresh");
    }

    @ParameterizedTest
    @MethodSource("missingCredentials")
    void missingCredentialsSkipHttpCall(String clientId, String clientSecret) {
        ReflectionTestUtils.setField(provider, "clientId", clientId);
        ReflectionTestUtils.setField(provider, "clientSecret", clientSecret);

        assertThat(provider.getToken()).isNull();
    }

    @Test
    void missingAccessTokenReturnsNull() {
        server.expect(tokenRequest())
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(provider.getToken()).isNull();
    }

    @Test
    void httpFailureReturnsNullWithoutLeakingException() {
        server.expect(tokenRequest()).andRespond(withServerError());

        assertThat(provider.getToken()).isNull();
    }

    @Test
    void invalidExpiresInFallsBackToDefaultLifetime() {
        long before = System.currentTimeMillis();
        expectToken("token", "not-a-number");

        assertThat(provider.getToken()).isEqualTo("token");
        long expiresAt = (long) ReflectionTestUtils.getField(provider, "expiresAtEpochMillis");
        assertThat(expiresAt).isGreaterThan(before + 6L * 24L * 60L * 60L * 1000L);
    }

    private void expectToken(String token, String expiresIn) {
        server.expect(tokenRequest())
                .andRespond(withSuccess(
                        "{\"access_token\":\"" + token + "\",\"expires_in\":\"" + expiresIn + "\"}",
                        MediaType.APPLICATION_JSON
                ));
    }

    private RequestMatcher tokenRequest() {
        return request -> {
            assertThat(request.getURI()).isEqualTo(URI.create(TOKEN_URL));
            assertThat(request.getMethod()).isEqualTo(org.springframework.http.HttpMethod.POST);
        };
    }

    private static Stream<Arguments> missingCredentials() {
        return Stream.of(
                Arguments.of(null, "secret"),
                Arguments.of("   ", "secret"),
                Arguments.of("id", null),
                Arguments.of("id", "  ")
        );
    }
}
