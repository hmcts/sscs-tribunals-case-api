package uk.gov.hmcts.reform.sscs.functional.utilities.idam;

import static java.util.Objects.nonNull;

import feign.FeignException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.OAuth2Configuration;
import uk.gov.hmcts.reform.idam.client.models.TokenRequest;
import uk.gov.hmcts.reform.idam.client.models.TokenResponse;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.CreateUserResponse;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.User;
import uk.gov.hmcts.reform.sscs.functional.utilities.idam.model.UserRecord;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class LocalIdamService {

    private static final String OPENID_GRANT_TYPE = "password";
    private static final String DEFAULT_PASSWORD = "Pa$$word";
    private static final Map<String, User> USERS = new ConcurrentHashMap<>();
    private final IdamClient idamClient;
    private final IdamApi idamApi;
    private final OAuth2Configuration oauth2Configuration;
    private final AuthTokenGenerator authTokenGenerator;

    public LocalIdamService(IdamClient idamClient,
                            IdamApi idamApi,
                            OAuth2Configuration oauth2Configuration,
                            AuthTokenGenerator authTokenGenerator) {
        this.idamClient = idamClient;
        this.idamApi = idamApi;
        this.oauth2Configuration = oauth2Configuration;
        this.authTokenGenerator = authTokenGenerator;
    }

    public User withUser(User requested) {
        try {
            User existing = getUser(requested.email());

            if (nonNull(existing)) {
                if (existing.isSame(requested)) {
                    log.info("User {} already exists and matches requested configuration.", requested.email());
                    return existing;
                }
                throw new IllegalStateException(
                    "User already exists but differs. Requested=%s, Existing=%s"
                        .formatted(requested, existing)
                );
            }
        } catch (FeignException.FeignClientException.NotFound notFound) {
            log.info("User {} does not currently exist in IDAM - creating.", requested.email());
        }

        CreateUserResponse created = idamClient.createTestUser(mapUser(requested));

        TokenResponse accessToken = getAccessTokenResponse(requested.email(), DEFAULT_PASSWORD);

        User createdUser = requested.withId(created.uuid());
        IdamTokens tokens = buildIdamTokens(createdUser, accessToken);

        User cached = createdUser.withTokens(tokens);
        USERS.put(requested.email(), cached);

        return cached;
    }

    @Retryable
    public String generateServiceAuthorization() {
        return authTokenGenerator.generate();
    }

    private static @NonNull User mapUserRecord(UserRecord details) {
        return User.builder()
            .id(UUID.fromString(details.getId()))
            .email(details.getEmail())
            .forename(details.getForename())
            .surname(details.getSurname())
            .displayName(details.getDisplayName())
            .roles(details.getRoles().stream().map(String::valueOf).toList())
            .build();
    }

    private static @NonNull UserRecord mapUser(User user) {
        return UserRecord.builder()
            .email(user.email())
            .forename(user.forename())
            .surname(user.surname())
            .password(DEFAULT_PASSWORD)
            .roles(user.roles())
            .build();
    }

    private User getUser(String email) {
        return USERS.computeIfAbsent(email, this::fetchAndCacheUser);
    }

    private User fetchAndCacheUser(String email) {
        log.info("Cache miss for user {} - fetching from IDAM.", email);

        User user = mapUserRecord(idamClient.getUserDetails(email));
        TokenResponse accessToken = getAccessTokenResponse(user.email(), DEFAULT_PASSWORD);

        return user.withTokens(buildIdamTokens(user, accessToken));
    }

    private IdamTokens buildIdamTokens(User user, TokenResponse accessToken) {
        return IdamTokens.builder()
            .userId(user.id().toString())
            .email(user.email())
            .roles(user.roles())
            .idamOauth2Token("Bearer " + accessToken.accessToken)
            .serviceAuthorization(generateServiceAuthorization())
            .build();
    }

    private TokenResponse getAccessTokenResponse(String username, String password) {
        return idamApi.generateOpenIdToken(
            new TokenRequest(
                oauth2Configuration.getClientId(),
                oauth2Configuration.getClientSecret(),
                OPENID_GRANT_TYPE,
                oauth2Configuration.getRedirectUri(),
                username,
                password,
                oauth2Configuration.getClientScope(),
                null,
                null
            )
        );
    }
}
