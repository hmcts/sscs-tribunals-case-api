package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureToggleService {

    private final LDClient ldClient;
    private final String ldUserKey;

    @Autowired
    public FeatureToggleService(LDClient ldClient, @Value("${ld.user-key}") String ldUserKey) {
        this.ldClient = ldClient;
        this.ldUserKey = ldUserKey;
    }

    public boolean isSendGridEnabled() {
        return ldClient.boolVariation("send-grid", createLdContext(), false);
    }

    private LDContext createLdContext() {
        var contextBuilder = LDContext.builder(ldUserKey)
            .set("timestamp", String.valueOf(System.currentTimeMillis()));

        return contextBuilder.build();
    }
}
