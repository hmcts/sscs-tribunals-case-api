package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
@Slf4j
public class DecisionIssuedAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final FooterService footerService;

    @Autowired
    public DecisionIssuedAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DECISION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        DocumentLink url = null;
        if (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())) {
            url = caseData.getPreviewDocument();
        } else {
            if (caseData.getSscsInterlocDecisionDocument() != null) {
                url = caseData.getSscsInterlocDecisionDocument().getDocumentLink();
                caseData.setDateAdded(caseData.getSscsInterlocDecisionDocument().getDocumentDateAdded());
            }
        }

        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DECISION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()).format(DateTimeFormatter.ofPattern("dd-MM-YYYY")));

        State beforeState = callback.getCaseDetailsBefore().map(CaseDetails::getState).orElse(null);

        clearTransientFields(caseData, beforeState);

        caseData.setDwpState(DwpState.STRUCK_OUT.getId());

        caseData.setDirectionDueDate(null);

        if (STRIKE_OUT.getValue().equals(caseData.getDecisionType())) {
            if (State.INTERLOCUTORY_REVIEW_STATE.equals(beforeState)) {
                caseData.setOutcome("nonCompliantAppealStruckout");
            } else {
                caseData.setOutcome("struckOut");
            }
            caseData.setInterlocReviewState(null);
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Saved the new interloc decision document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }
}
