package uk.gov.hmcts.reform.sscs.ccd.presubmit.confidentiality;

import static java.time.LocalDateTime.now;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
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
        return cmOtherPartyConfidentialityEnabled && (callback.getEvent() == DWP_UPLOAD_RESPONSE
            || callback.getEvent() == UPDATE_OTHER_PARTY_DATA
            || callback.getEvent() == INCOMPLETE_APPLICATION_RECEIVED
            || callback.getEvent() == CASE_UPDATED
            || callback.getEvent() == ACTION_HEARING_RECORDING_REQUEST) && callbackType == CallbackType.ABOUT_TO_SUBMIT && (
            callback.getCaseDetails().getCaseData().isBenefitType(Benefit.CHILD_SUPPORT)
                || callback.getCaseDetails().getCaseData().isBenefitType(Benefit.UC));
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

        if (sscsCaseData.isBenefitType(Benefit.CHILD_SUPPORT) || isNotEmpty(
            sscsCaseData.getOtherParties())) {
            sscsCaseData.getExtendedSscsCaseData().setShowConfidentialityTab(YES);
        } else {
            sscsCaseData.getExtendedSscsCaseData().setShowConfidentialityTab(NO);
        }

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
        if (nonNull(confidentialityRequired) && (confidentialityRequiredBefore == null || !Objects.equals(
            confidentialityRequiredBefore, confidentialityRequired))) {
            currentCaseData.getAppellant().ifPresent(appellant -> appellant.setConfidentialityRequiredChangedDate(now()));
        }
    }
}
