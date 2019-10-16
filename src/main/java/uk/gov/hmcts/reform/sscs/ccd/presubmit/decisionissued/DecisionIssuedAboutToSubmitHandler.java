package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
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
            SscsDocument decisionNotice = caseData.getLatestDocumentForDocumentType(DocumentType.DECISION_NOTICE);
            if (decisionNotice != null) {
                url = decisionNotice.getValue().getDocumentLink();
            }
        }

        createFooter(url, caseData);
        clearTransientFields(caseData);

        if (caseData.getDecisionType() != null && caseData.getDecisionType().equals(STRIKE_OUT.getValue())) {
            caseData.setOutcome("nonCompliantAppealStruckout");
        }

        PreSubmitCallbackResponse<SscsCaseData> sscsCaseDataPreSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);
        log.info("Saved the new interloc decision document for case id: " + caseData.getCcdCaseId());

        return sscsCaseDataPreSubmitCallbackResponse;
    }

    private void createFooter(DocumentLink url, SscsCaseData caseData) {
        if (url != null) {
            log.info("Decision issued adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());

            SscsDocument sscsDocument = footerService.createFooterDocument(caseData, url, "Decision notice",
                    caseData.getPreviewDocument().getDocumentFilename(), caseData.getDateAdded(), DocumentType.DECISION_NOTICE);

            List<SscsDocument> documents = new ArrayList<>();
            if (caseData.getSscsDocument() != null) {
                documents.addAll(caseData.getSscsDocument());
            }
            documents.add(sscsDocument);
            caseData.setSscsDocument(documents);
        } else {
            log.info("Could not find decision issued document for caseId {} so skipping generating footer", caseData.getCcdCaseId());
        }
    }
}
