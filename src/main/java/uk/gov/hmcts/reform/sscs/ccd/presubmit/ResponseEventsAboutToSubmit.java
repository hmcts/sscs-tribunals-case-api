package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class ResponseEventsAboutToSubmit {

    public void checkMandatoryFields(PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse, SscsCaseData sscsCaseData) {
        if (sscsCaseData.getBenefitCode() == null) {
            preSubmitCallbackResponse.addError("Benefit code cannot be empty");
        }
        if (sscsCaseData.getIssueCode() == null) {
            preSubmitCallbackResponse.addError("Issue code cannot be empty");
        } else if (sscsCaseData.getIssueCode().equals("DD")) {
            preSubmitCallbackResponse.addError("Issue code cannot be set to the default value of DD");
        }
        DocumentLink dwpUcbEvidenceDocument = sscsCaseData.getDwpUcbEvidenceDocument();
        if (isYes(sscsCaseData.getDwpUcb()) && dwpUcbEvidenceDocument == null) {
            preSubmitCallbackResponse.addError("Please upload a UCB document");
        } else if (!isYes(sscsCaseData.getDwpUcb()) && preSubmitCallbackResponse.getErrors().isEmpty()) {
            sscsCaseData.setDwpUcb(null);
            sscsCaseData.setDwpUcbEvidenceDocument(null);
        }
        if (isYes(sscsCaseData.getDwpUcb()) && dwpUcbEvidenceDocument != null && preSubmitCallbackResponse.getErrors().isEmpty()) {
            addToDwpDocuments(sscsCaseData, dwpUcbEvidenceDocument);
            sscsCaseData.setDwpUcbEvidenceDocument(null);
        }
    }

    private void addToDwpDocuments(SscsCaseData sscsCaseData, DocumentLink dwpUcbEvidenceDocument) {
        DwpDocumentDetails dwpDocumentDetails = new DwpDocumentDetails(DwpDocumentType.UCB.getValue(),
                "UCB document",
                null,
                LocalDateTime.now(),
                dwpUcbEvidenceDocument, null, null, null, null, null, null, null, null, null);
        DwpDocument dwpDocument = new DwpDocument(dwpDocumentDetails);
        if (isNull(sscsCaseData.getDwpDocuments())) {
            sscsCaseData.setDwpDocuments(new ArrayList<>());
        }
        sscsCaseData.getDwpDocuments().removeIf(d -> DwpDocumentType.UCB.getValue().equals(d.getValue().getDocumentType()));
        sscsCaseData.getDwpDocuments().add(dwpDocument);
    }

    public void setCaseCode(SscsCaseData sscsCaseData, EventType event) {
        if (!event.getCcdType().equals(EventType.CASE_UPDATED.getCcdType())
                && sscsCaseData.getAppeal().getBenefitType() != null
                && "uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            setUcCaseCode(sscsCaseData);
        } else if (sscsCaseData.getBenefitCode() != null && sscsCaseData.getIssueCode() != null) {
            sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));
        }
    }

    private String buildCaseCode(SscsCaseData sscsCaseData) {
        return sscsCaseData.getBenefitCode() + sscsCaseData.getIssueCode();
    }

    private void setUcCaseCode(SscsCaseData sscsCaseData) {
        boolean multiElementAppeal = null != sscsCaseData.getElementsDisputedList() && sscsCaseData.getElementsDisputedList().size() > 1;
        String issueCode = multiElementAppeal ? "UM" : "US";

        sscsCaseData.setIssueCode(issueCode);
        sscsCaseData.setBenefitCode("001");
        sscsCaseData.setCaseCode("001" + issueCode);
        sscsCaseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP.getId());
    }
}
