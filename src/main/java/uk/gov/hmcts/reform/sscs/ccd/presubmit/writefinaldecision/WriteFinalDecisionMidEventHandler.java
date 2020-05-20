package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.time.LocalDate;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Component
@Slf4j
public class WriteFinalDecisionMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.WRITE_FINAL_DECISION
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isDecisionNoticeDatesInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice end date must be after decision notice start date");
        }
        if (isDecisionNoticeDecisionDateInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice decision date must not be in the future");
        }
        return preSubmitCallbackResponse;
    }

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getPipWriteFinalDecisionStartDate() != null && sscsCaseData.getPipWriteFinalDecisionEndDate() != null) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

    private boolean isDecisionNoticeDecisionDateInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getPipWriteFinalDecisionDecisionDate()!= null) {
            LocalDate decisionNoticeDecisionDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionDecisionDate());
            LocalDate today = LocalDate.now();
            return decisionNoticeDecisionDate.isAfter(today);
        }
        return false;
    }

}
