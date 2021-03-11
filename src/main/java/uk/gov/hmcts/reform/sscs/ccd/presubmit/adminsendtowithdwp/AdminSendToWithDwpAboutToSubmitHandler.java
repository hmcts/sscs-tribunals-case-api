package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminsendtowithdwp;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class AdminSendToWithDwpAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ADMIN_SEND_TO_WITH_DWP;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        log.info("Setting date sent to dwp for case id {} for AdminSendToWithDwpHandler" + callback.getCaseDetails().getId());
        caseData.setDateSentToDwp(LocalDate.now().toString());

        if (InterlocReferralReason.PHME_REQUEST.getId().equals(caseData.getInterlocReferralReason())) {
            caseData.setInterlocReviewState(InterlocReviewState.NONE.getId());
            caseData.setInterlocReferralReason(InterlocReferralReason.NONE.getId());
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }
}
