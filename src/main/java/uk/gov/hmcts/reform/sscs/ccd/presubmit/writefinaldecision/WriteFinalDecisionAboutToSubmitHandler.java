package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@Component
@Slf4j
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeService decisionNoticeService;
    private final PreviewDocumentService previewDocumentService;

    @Autowired
    public WriteFinalDecisionAboutToSubmitHandler(DecisionNoticeService decisionNoticeService,
                                                  PreviewDocumentService previewDocumentService) {
        this.decisionNoticeService = decisionNoticeService;
        this.previewDocumentService = previewDocumentService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
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

        // Due to a bug with CCD related to hidden fields, this field is not being set
        // on the final submission from CCD, so we need to reset it here
        // See https://tools.hmcts.net/jira/browse/RDM-8200
        // This is a temporary workaround for this issue.
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGeneratedDate(LocalDate.now().toString());

        if ("na".equals(sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionEndDateType())) {
            sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionEndDateType(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        String benefitType = WriteFinalDecisionBenefitTypeHelper.getBenefitType(sscsCaseData);

        if (benefitType == null) {
            preSubmitCallbackResponse.addError("Unexpected error - benefit type is null");
        } else {

            DecisionNoticeOutcomeService outcomeService = decisionNoticeService.getOutcomeService(benefitType);

            outcomeService.validate(preSubmitCallbackResponse, sscsCaseData);

            previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_DECISION_NOTICE, sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument());
        }
        return preSubmitCallbackResponse;
    }
}
