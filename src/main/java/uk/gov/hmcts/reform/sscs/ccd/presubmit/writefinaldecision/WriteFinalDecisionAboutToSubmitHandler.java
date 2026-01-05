package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static java.util.Objects.isNull;

import java.time.LocalDate;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
@AllArgsConstructor
public class WriteFinalDecisionAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final DecisionNoticeService decisionNoticeService;
    private final PreviewDocumentService previewDocumentService;
    private final UserDetailsService userDetailsService;
    @Value("${feature.postHearings.enabled}")
    private boolean isPostHearingsEnabled;

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

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();
        State state = sscsCaseData.getState();

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

            if (!(State.READY_TO_LIST.equals(state) || State.WITH_DWP.equals(sscsCaseData.getState()))) {
                sscsCaseData.setPreviousState(state);
            }
            
            if (isPostHearingsEnabled) {
                SscsFinalDecisionCaseData finalDecisionCaseData = sscsCaseData.getSscsFinalDecisionCaseData();

                if (isNull(finalDecisionCaseData.getFinalDecisionIssuedDate())) {
                    finalDecisionCaseData.setFinalDecisionIdamSurname(userDetailsService.buildLoggedInUserName(userAuthorisation));
                    finalDecisionCaseData.setFinalDecisionGeneratedDate(LocalDate.now());
                }   
            }

            SscsUtil.setCorrectionInProgress(caseDetails, isPostHearingsEnabled);

            DocumentType docType = SscsUtil.getWriteFinalDecisionDocumentType(sscsCaseData, isPostHearingsEnabled);
            DocumentLink docLink = sscsCaseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionPreviewDocument();

            previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, docType, docLink);
        }

        return preSubmitCallbackResponse;
    }
}
