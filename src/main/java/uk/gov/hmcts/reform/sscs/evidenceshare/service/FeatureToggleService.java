package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Optional.ofNullable;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Slf4j
@Service
public class FeatureToggleService {

    private final LDClient ldClient;
    private final String ldUserKey;
    private final IdamService idamService;

    @Autowired
    public FeatureToggleService(LDClient ldClient, IdamService idamService, @Value("${ld.user-key}") String ldUserKey) {
        this.ldClient = ldClient;
        this.ldUserKey = ldUserKey;
        this.idamService = idamService;
    }

    public boolean isEnabled(final FeatureFlag featureFlag, final String userId, final String email) {
        ofNullable(featureFlag)
            .orElseThrow(() -> new IllegalArgumentException("featureFlag must not be null"));
        ofNullable(userId)
            .orElseThrow(() -> new IllegalArgumentException("userId must not be null"));

        log.info("Retrieve boolean value for featureFlag: {} for userId: {}", featureFlag, userId);
        return ldClient.boolVariation(featureFlag.getKey(), createLaunchDarklyContext(userId, email), false);

    }

    public boolean isEnabled(final FeatureFlag featureFlag) {
        final IdamTokens idamTokens = idamService.getIdamTokens();
        return isEnabled(featureFlag, idamTokens.getUserId(), idamTokens.getEmail());
    }

    public boolean isSendGridEnabled() {
        return ldClient.boolVariation("send-grid", createLdContext(), false);
    }

    private LDContext createLaunchDarklyContext(final String userId, final String email) {
        return LDContext.builder("sscs-tribunals-case-api")
            .set("name", userId)
            .set("email", email)
            .set("firstName", "SSCS")
            .set("lastName", "Tribunals")
            .build();
    }

    private LDContext createLdContext() {
        var contextBuilder = LDContext.builder(ldUserKey)
            .set("timestamp", String.valueOf(System.currentTimeMillis()));

        return contextBuilder.build();
    }
}
