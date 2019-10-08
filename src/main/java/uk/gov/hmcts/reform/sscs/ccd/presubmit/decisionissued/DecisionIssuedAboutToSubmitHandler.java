package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;

import java.time.LocalDate;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDecisionDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDecisionDocuments;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class DecisionIssuedAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.ABOUT_TO_SUBMIT
                && callback.getEvent() == EventType.DECISION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isGenerateNotice()
                && (Objects.nonNull(callback.getCaseDetails().getCaseData().getPreviewDocument())
                    || Objects.nonNull(callback.getCaseDetails().getCaseData().getSscsInterlocDecisionDocument()));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (Objects.nonNull(caseData.getPreviewDocument())) {
            SscsInterlocDecisionDocument document = SscsInterlocDecisionDocument.builder()
                    .documentFileName(caseData.getPreviewDocument().getDocumentFilename())
                    .documentLink(caseData.getPreviewDocument())
                    .documentDateAdded(Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now()))
                    .documentType(DocumentType.DECISION_NOTICE.getValue())
                    .build();

            caseData.setSscsInterlocDecisionDocument(document);
        }
        saveToHistory(caseData);
        clearTransientFields(caseData);

        if (caseData.getDecisionType() != null && caseData.getDecisionType().equals(STRIKE_OUT.getValue())) {
            caseData.setOutcome("nonCompliantAppealStruckout");
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Saved the new interloc decision document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void saveToHistory(SscsCaseData caseData) {
        List<SscsInterlocDecisionDocuments> historicDocs = new ArrayList<>(Optional.ofNullable(caseData.getHistoricSscsInterlocDecisionDocs()).orElse(Collections.emptyList()));
        historicDocs.add(SscsInterlocDecisionDocuments.builder().value(caseData.getSscsInterlocDecisionDocument()).build());
        caseData.setHistoricSscsInterlocDecisionDocs(historicDocs);
    }

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setBodyContent(null);
        caseData.setPreviewDocument(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setGenerateNotice(null);
        caseData.setDateAdded(null);
    }
}
