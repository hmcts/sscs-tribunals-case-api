package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentiality;

import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateOtherPartiesConfidentialityChangedDate;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class ConfidentialityTabAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmOtherPartyConfidentialityEnabled;

    public ConfidentialityTabAboutToSubmitHandler(
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        // Runs for every event type to ensure that any changes to data are reflected in the tab and also to future-proof against other new or modified events that may update confidentiality data
        return cmOtherPartyConfidentialityEnabled && callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getCaseDetails()
                       .getCaseData()
                       .isBenefitType(
                           Benefit.CHILD_SUPPORT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {

        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        updateAppellantConfidentialityRequiredChangedDate(callback);
        updateOtherPartiesConfidentialityRequiredChangedDate(callback);

        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    private void updateOtherPartiesConfidentialityRequiredChangedDate(Callback<SscsCaseData> callback) {
        final List<CcdValue<OtherParty>> previousOtherParties = callback.getCaseDetailsBefore()
                                                                        .map(CaseDetails::getCaseData)
                                                                        .map(SscsCaseData::getOtherParties)
                                                                        .orElse(null);
        updateOtherPartiesConfidentialityChangedDate(callback.getCaseDetails().getCaseData().getOtherParties(),
            previousOtherParties);
    }

    private static void updateAppellantConfidentialityRequiredChangedDate(final Callback<SscsCaseData> callback) {
        final YesNo confidentialityRequiredBefore = callback.getCaseDetailsBefore()
                                                            .map(CaseDetails::getCaseData)
                                                            .flatMap(SscsCaseData::getAppellantConfidentialityRequired)
                                                            .orElse(null);
        final SscsCaseData currentCaseData = callback.getCaseDetails().getCaseData();
        final YesNo confidentialityRequired = currentCaseData.getAppellantConfidentialityRequired().orElse(null);
        if (nonNull(confidentialityRequired) && (confidentialityRequiredBefore == null || !Objects.equals(confidentialityRequiredBefore, confidentialityRequired))) {
            currentCaseData.getAppellant()
                           .ifPresent(appellant -> appellant.setConfidentialityRequiredChangedDate(now()));
        }
    }
}
