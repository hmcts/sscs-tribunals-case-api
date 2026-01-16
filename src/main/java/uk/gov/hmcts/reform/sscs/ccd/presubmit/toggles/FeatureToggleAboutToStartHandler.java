package uk.gov.hmcts.reform.sscs.ccd.presubmit.toggles;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;
import uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;

@Slf4j
@Component
public class FeatureToggleAboutToStartHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private FeatureToggleService featureToggleService;
    private IdamService idamService;

    public FeatureToggleAboutToStartHandler(FeatureToggleService featureToggleService, IdamService idamService) {
        this.featureToggleService = featureToggleService;
        this.idamService = idamService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType == CallbackType.ABOUT_TO_START && isChildSupport(callback);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final UserDetails userDetails = idamService.getUserDetails(userAuthorisation);
        updateWithFeatureFlag(callback, userDetails, FeatureFlag.SSCS_CHILD_MAINTENANCE_FT);
        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    private void updateWithFeatureFlag(Callback<SscsCaseData> callback, UserDetails userDetails, FeatureFlag featureFlag) {
        boolean enabled = featureToggleService.isEnabled(featureFlag, userDetails.getId(), userDetails.getEmail());

        if ("system.update@hmcts.net".equals(userDetails.getEmail())) {
            return;
        }

        log.info("Feature flag {} is {} for user {} with email {}", featureFlag.getKey(), enabled, userDetails.getId(),
            userDetails.getEmail());
        if (enabled) {
            callback.getCaseDetails().getCaseData().getExtendedSscsCaseData().enableFeature(featureFlag.getKey());
        } else {
            callback.getCaseDetails().getCaseData().getExtendedSscsCaseData().disableFeature(featureFlag.getKey());
        }
    }

    private boolean isChildSupport(Callback<SscsCaseData> callback) {
        return Optional.ofNullable(callback).map(Callback::getCaseDetails).map(CaseDetails::getCaseData)
            .flatMap(SscsCaseData::getBenefitType).filter(Benefit.CHILD_SUPPORT::equals).isPresent();
    }

}
