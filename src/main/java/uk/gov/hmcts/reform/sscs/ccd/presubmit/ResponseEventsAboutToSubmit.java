package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsType.SSCS5;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.util.DocumentUtil.isFileAPdf;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
                dwpUcbEvidenceDocument, null, null, null, null, null, null, null, null, null, null);
        DwpDocument dwpDocument = new DwpDocument(dwpDocumentDetails);
        if (isNull(sscsCaseData.getDwpDocuments())) {
            sscsCaseData.setDwpDocuments(new ArrayList<>());
        }
        sscsCaseData.getDwpDocuments().removeIf(d -> DwpDocumentType.UCB.getValue().equals(d.getValue().getDocumentType()));
        sscsCaseData.getDwpDocuments().add(dwpDocument);
    }


    public void setCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback, boolean hasSuperUserRole) {
        SscsCaseData sscsCaseData = response.getData();
        if (!callback.getEvent().getCcdType().equals(EventType.CASE_UPDATED.getCcdType())
                && sscsCaseData.getAppeal().getBenefitType() != null
                && "uc".equalsIgnoreCase(sscsCaseData.getAppeal().getBenefitType().getCode())) {
            setUcCaseCode(sscsCaseData);
        } else if (sscsCaseData.getBenefitCode() != null && sscsCaseData.getIssueCode() != null) {
            sscsCaseData.setCaseCode(buildCaseCode(sscsCaseData));
        }
        validateChangedCaseCode(response, callback, hasSuperUserRole);
    }


    public void setCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback) {
        setCaseCode(response, callback, false);
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
        sscsCaseData.setDwpState(DwpState.RESPONSE_SUBMITTED_DWP);
    }

    private void validateChangedCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback) {
        validateChangedCaseCode(response, callback, false);
    }

    private void validateChangedCaseCode(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback, boolean hasSuperUserRole) {
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();

        if (response.getData().getAppeal().getBenefitType() != null
            && caseDetailsBefore.isPresent()
            && caseDetailsBefore.get().getCaseData().getBenefitCode() != null) {

            boolean hasErrors = false;
            boolean isSscs5CaseData = isSscs5Case(response, callback);
            if (wasSscs5NowGettingChangedToNonSscs5(isSscs5CaseData, response.getData().getBenefitCode())
                || nonSscs5NowGettingchangedToSscs5(isSscs5CaseData, response.getData().getBenefitCode())) {
                if (hasSuperUserRole) {
                    response.addWarning("Benefit code cannot be changed to the selected code");
                } else {
                    response.addError("Benefit code cannot be changed to the selected code");
                    hasErrors = true;
                }
            }

            if (!caseDetailsBefore.get().getCaseData().getBenefitCode()
                .equals(callback.getCaseDetails().getCaseData().getBenefitCode())) {
                if (Benefit.CHILD_SUPPORT.getShortName()
                    .equalsIgnoreCase(response.getData().getAppeal().getBenefitType().getCode())
                    && !Benefit.CHILD_SUPPORT.getCaseLoaderKeyId().contains(response.getData().getBenefitCode())) {
                    if (response.getData().getOtherParties() != null
                        && !response.getData().getOtherParties().isEmpty()) {
                        response.addError("Benefit code cannot be changed on cases with registered 'Other Party'");
                    } else if (!hasErrors) {
                        response.addWarning("The benefit code will be changed to a non-child support benefit code");
                    }
                }
            }
        }
    }

    private boolean nonSscs5NowGettingchangedToSscs5(boolean isSscs5CaseData, String newBenefitCode) {
        return !isSscs5CaseData
                && buildSscs5BenefitCaseLoaderKeyId().contains(newBenefitCode);
    }

    private boolean wasSscs5NowGettingChangedToNonSscs5(boolean isSscs5CaseData, String newBenefitCode) {
        return isSscs5CaseData
                && !buildSscs5BenefitCaseLoaderKeyId().contains(newBenefitCode);
    }

    private boolean isSscs5Case(PreSubmitCallbackResponse<SscsCaseData> response, Callback<SscsCaseData> callback) {
        //Consider SSCS5 case if caseDetailsBefore is SSCS5 or else consider present benefit type code
        String benefitTypeCode = response.getData().getAppeal().getBenefitType().getCode();
        final Optional<CaseDetails<SscsCaseData>> caseDetailsBefore = callback.getCaseDetailsBefore();
        if (caseDetailsBefore.isPresent()
            && caseDetailsBefore.get().getCaseData().getAppeal() != null
            && caseDetailsBefore.get().getCaseData().getAppeal().getBenefitType() != null) {
            benefitTypeCode = caseDetailsBefore.get().getCaseData().getAppeal().getBenefitType().getCode();
        }
        return isSscs5Case(benefitTypeCode);
    }

    private boolean isSscs5Case(String benefitTypeCode) {
        return Optional.ofNullable(benefitTypeCode)
            .filter(b -> Benefit.findBenefitByShortName(b)
            .filter(benefit -> benefit.getSscsType().equals(SSCS5)).isPresent())
            .isPresent();
    }

    public void validateBenefitForCase(PreSubmitCallbackResponse<SscsCaseData> response,
                                       Callback<SscsCaseData> callback, boolean hasSuperUserRole) {
        if (callback.getCaseDetails().getCaseData().getAppeal().getBenefitType() != null) {
            String benefitCode = callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getCode();
            String benefitDescription =
                    callback.getCaseDetails().getCaseData().getAppeal().getBenefitType().getDescription();

            boolean isSscs5CaseData = isSscs5Case(response, callback);
            if (wasSscs5NowgettingChangedToNonSscs5WithDescription(isSscs5CaseData, benefitCode,benefitDescription)
                    || notSscs5NowGettingChangedToSscs5WithDescription(isSscs5CaseData, benefitCode, benefitDescription)) {
                if (hasSuperUserRole) {
                    response.addWarning("Benefit type cannot be changed to the selected type");
                } else {
                    response.addError("Benefit type cannot be changed to the selected type");
                }
            }
        }
    }

    private boolean notSscs5NowGettingChangedToSscs5WithDescription(boolean isSscs5CaseData, String benefitCode, String benefitDescription) {
        return !isSscs5CaseData && (buildSscs5BenefitCode().contains(benefitCode)
                || buildSscs5BenefitDescription().contains(benefitDescription));
    }

    private boolean wasSscs5NowgettingChangedToNonSscs5WithDescription(boolean isSscs5CaseData, String benefitCode, String benefitDescription) {
        return isSscs5CaseData && (!buildSscs5BenefitCode().contains(benefitCode)
                || !buildSscs5BenefitDescription().contains(benefitDescription));
    }


    private List<String> buildSscs5BenefitCaseLoaderKeyId() {
        return Arrays.stream(Benefit.values())
            .filter(benefit -> SSCS5.equals(benefit.getSscsType()))
            .map(Benefit::getCaseLoaderKeyId)
            .flatMap(Collection::stream)
            .toList();
    }

    private List<String> buildSscs5BenefitCode() {
        return Arrays.stream(Benefit.values())
            .filter(benefit -> SSCS5.equals(benefit.getSscsType()))
            .map(Benefit::getShortName)
            .toList();
    }

    private List<String> buildSscs5BenefitDescription() {
        return Arrays.stream(Benefit.values())
            .filter(benefit -> SSCS5.equals(benefit.getSscsType()))
            .map(Benefit::getDescription)
            .toList();
    }
}
