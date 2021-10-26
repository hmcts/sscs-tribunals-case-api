package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
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
        } else if (isYes(sscsCaseData.getDwpUcb()) && !isFileAPdf(dwpUcbEvidenceDocument)) {
            preSubmitCallbackResponse.addError("UCB document must be a PDF.");
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

    public void setCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback) {
        SscsCaseData sscsCaseData = response.getData();
        if (!callback.getEvent().getCcdType().equals(EventType.CASE_UPDATED.getCcdType())
                && sscsCaseData.getAppeal().getBenefitType() != null
                && "uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            setUcCaseCode(sscsCaseData);
        } else if (sscsCaseData.getBenefitCode() != null && sscsCaseData.getIssueCode() != null) {
            sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));
        }
        validateChangedCaseCode(response, callback);
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

    private void validateChangedCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback) {
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();

        if (response.getData().getAppeal().getBenefitType() != null
            && Benefit.CHILD_SUPPORT.getShortName().equalsIgnoreCase(response.getData().getAppeal().getBenefitType().getCode())
            && !Benefit.CHILD_SUPPORT.getCaseLoaderKeyId().contains(response.getData().getBenefitCode())
            && caseDetailsBefore.isPresent()
            && caseDetailsBefore.get().getCaseData().getBenefitCode() != null
            && !caseDetailsBefore.get().getCaseData().getBenefitCode().equals(callback.getCaseDetails().getCaseData().getBenefitCode())) {

            if (response.getData().getOtherParties() != null && response.getData().getOtherParties().size() > 0) {
                response.addError("Benefit code cannot be changed on cases with registered 'Other Party'");
            } else {
                response.addWarning("The benefit code will be changed to a non-child support benefit code");
            }
        }
    }
}
