package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuehearingenquiryform;

import static java.time.LocalDate.now;
import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class IssueHearingEnquiryFormAboutToSubmit implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final int HEF_RESPONSE_EXPECTED_BY_DAYS = 21;
    private final boolean cmOtherPartyConfidentialityEnabled;

    public IssueHearingEnquiryFormAboutToSubmit(
        @Value("${feature.cm-other-party-confidentiality.enabled}") boolean cmOtherPartyConfidentialityEnabled) {
        this.cmOtherPartyConfidentialityEnabled = cmOtherPartyConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return cmOtherPartyConfidentialityEnabled
            && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.ISSUE_HEARING_ENQUIRY_FORM;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback.");
        }

        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setDirectionDueDate(now().plusDays(HEF_RESPONSE_EXPECTED_BY_DAYS).toString());
        sscsCaseData.setInterlocReviewState(InterlocReviewState.HEF_ISSUED);
        return new PreSubmitCallbackResponse<>(sscsCaseData);
    }

    public static int getHearingResponseExpectedByDays() {
        return HEF_RESPONSE_EXPECTED_BY_DAYS;
    }

}
