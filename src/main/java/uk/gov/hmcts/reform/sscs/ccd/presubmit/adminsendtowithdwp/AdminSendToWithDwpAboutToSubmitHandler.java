package uk.gov.hmcts.reform.sscs.ccd.presubmit.adminsendtowithdwp;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DateTimeUtils;

@Service
@Slf4j
public class AdminSendToWithDwpAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final int dwpResponseDueDays;
    private final int dwpResponseDueDaysChildSupport;


    public AdminSendToWithDwpAboutToSubmitHandler(@Value("${dwp.response.due.days}") int dwpResponseDueDays,
                                                  @Value("${dwp.response.due.days-child-support}") int dwpResponseDueDaysChildSupport) {
        this.dwpResponseDueDays = dwpResponseDueDays;
        this.dwpResponseDueDaysChildSupport = dwpResponseDueDaysChildSupport;
    }

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
        caseData.setDwpDueDate(DateTimeUtils.generateDwpResponseDueDate(getResponseDueDays(caseData)));

        if (InterlocReferralReason.PHE_REQUEST.equals(caseData.getInterlocReferralReason())) {
            caseData.setInterlocReviewState(InterlocReviewState.NONE);
            caseData.setInterlocReferralReason(InterlocReferralReason.NONE);
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private int getResponseDueDays(SscsCaseData caseData) {
        return caseData.getAppeal().getBenefitType() != null
                && Benefit.CHILD_SUPPORT.getShortName().equalsIgnoreCase(caseData.getAppeal().getBenefitType().getCode())
                ? dwpResponseDueDaysChildSupport : dwpResponseDueDays;
    }
}
