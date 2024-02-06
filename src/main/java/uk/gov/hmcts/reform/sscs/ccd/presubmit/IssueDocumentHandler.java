package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.Respondent;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;


@Slf4j
public class IssueDocumentHandler {
    private static final String GLASGOW = "GLASGOW";

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    protected void clearTransientFields(SscsCaseData caseData) {
        clearBasicTransientFields(caseData);
        caseData.setExtensionNextEventDl(null);
    }

    protected void clearBasicTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(null);
        caseData.setSscsInterlocDirectionDocument(null);
        caseData.setSscsInterlocDecisionDocument(null);
    }

    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response,
                                                     SscsCaseData caseData, String documentTypeLabel,
                                                     LocalDate dateAdded, LocalDate generatedDate,
                                                     boolean isScottish, boolean isPostHearingsEnabled,
                                                     boolean isPostHearingsBEnabled,
                                                     String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = NoticeIssuedTemplateBody.builder()
                .appellantFullName(buildFullName(caseData))
                .appointeeFullName(buildAppointeeName(caseData).orElse(null))
                .caseId(caseData.getCcdCaseId())
                .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
                .shouldHideNino(isBenefitTypeValidToHideNino(caseData.getBenefitType()))
                .respondents(getRespondents(caseData))
                .noticeType(documentTypeLabel.toUpperCase())
                .dateAdded(dateAdded)
                .generatedDate(generatedDate)
                .idamSurname(caseData.getDocumentGeneration().getSignedBy())
                .build();

        if (isPostHearingsEnabled) {
            SscsFinalDecisionCaseData finalDecisionCaseData = caseData.getSscsFinalDecisionCaseData();

            if (isYes(caseData.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress())) {
                formPayload = formPayload.toBuilder()
                        .generatedDate(finalDecisionCaseData.getFinalDecisionGeneratedDate())
                        .idamSurname(finalDecisionCaseData.getFinalDecisionIdamSurname())
                        .dateIssued(finalDecisionCaseData.getFinalDecisionIssuedDate())
                        .correctedJudgeName(caseData.getDocumentGeneration().getSignedBy())
                        .correctedGeneratedDate(generatedDate)
                        .correctedDateIssued(LocalDate.now()).build();
            }
        }
      
        formPayload = PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(caseData, formPayload, isPostHearingsEnabled, isPostHearingsBEnabled);

        if (isScottish) {
            formPayload = formPayload.toBuilder().image(NoticeIssuedTemplateBody.SCOTTISH_IMAGE).build();
        }

        if (caseData.isLanguagePreferenceWelsh()) {
            formPayload = formPayload.toBuilder()
                    .welshDateAdded(LocalDateToWelshStringConverter.convert(dateAdded))
                    .welshGeneratedDate(LocalDateToWelshStringConverter.convert(generatedDate))
                    .build();
        }
        return formPayload;
    }

    protected List<Respondent> getRespondents(SscsCaseData caseData) {
        List<Respondent> respondents = new ArrayList<>();
        Optional<Benefit> benefitType = caseData.getBenefitType();
        if (benefitType.isPresent()) {
            if (benefitType.get().getSscsType().equals(SscsType.SSCS5)) {
                respondents.add(Respondent.builder().name(Respondent.HMRC).build());
            } else if (benefitType.get().getSscsType().equals(SscsType.SSCS1) || benefitType.get().getSscsType().equals(SscsType.SSCS2)) {
                respondents.add(Respondent.builder().name(Respondent.DWP).build());
            }
        }
        List<CcdValue<OtherParty>> otherParties = caseData.getOtherParties();
        if (otherParties != null) {
            String name;
            for (int i = 0; i < otherParties.size(); i++) {
                if (i < 10) {
                    name = Respondent.labelPrefixes[i] + " Respondent: ";
                } else {
                    name = "Respondent: ";
                }
                name = name + otherParties.get(i).getValue().getName().getFullName();
                respondents.add(Respondent.builder().name(name).build());
            }
        }
        return respondents;
    }

    protected boolean isBenefitTypeValidToHideNino(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS2.equals(benefit.getSscsType())
                || SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback, DocumentType documentType,
                                                                    String templateId, GenerateFile generateFile, String userAuthorisation) {
        return issueDocument(callback, documentType, templateId, generateFile, userAuthorisation, false, false);
    }

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback, DocumentType documentType,
                                                                    String templateId, GenerateFile generateFile, String userAuthorisation,
                                                                    boolean isPostHearingsEnabled, boolean isPostHearingsBEnabled) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if ((ADJOURNMENT_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType))
            && caseData.getAdjournment().getGenerateNotice() == null) {
            throw new IllegalStateException("Generate notice has not been set");
        }

        String documentUrl = Optional.ofNullable(getDocumentFromCaseData(caseData)).map(DocumentLink::getDocumentUrl).orElse(null);
        LocalDate dateAdded = Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now());
        String documentTypeLabel = getDocumentTypeLabel(caseData, documentType, isPostHearingsEnabled);
        String embeddedDocumentTypeLabel = getEmbeddedDocumentTypeLabel(caseData, documentType, documentTypeLabel, isPostHearingsEnabled);
        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        FormPayload formPayload = createPayload(response, caseData, embeddedDocumentTypeLabel, dateAdded, LocalDate.now(), isScottish, isPostHearingsEnabled, isPostHearingsBEnabled, userAuthorisation);

        if (!response.getErrors().isEmpty()) {
            return response;
        }

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(documentUrl)
                .templateId(templateId)
                .formPayload(formPayload)
                .userAuthentication(userAuthorisation)
                .build();

        log.info(String.format("Generating %s document isScottish = %s", documentTypeLabel, isScottish));
        final String generatedFileUrl = generateFile.assemble(params);
        documentTypeLabel = documentTypeLabel + ((DRAFT_CORRECTED_NOTICE.equals(documentType) || DRAFT_DECISION_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType)) ? " generated" : " issued");
        final String filename = String.format("%s on %s.pdf", documentTypeLabel, dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
                .documentFilename(filename)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();

        setDocumentOnCaseData(caseData, previewFile);

        return response;
    }

    private String getDocumentTypeLabel(SscsCaseData caseData, DocumentType documentType, boolean isPostHearingsEnabled) {
        String documentTypeLabel = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        if (isPostHearingsEnabled) {
            PostHearingReviewType reviewType = caseData.getPostHearing().getReviewType();

            if (nonNull(reviewType)) {
                documentTypeLabel = documentTypeLabel.replace(reviewType.getDescriptionEn(), reviewType.getShortDescriptionEn());
            }
        }

        return documentTypeLabel;
    }

    protected String getEmbeddedDocumentTypeLabel(SscsCaseData caseData, DocumentType documentType, String documentTypeLabel, boolean isPostHearingsEnabled) {
        String embeddedDocumentTypeLabel = (FINAL_DECISION_NOTICE.equals(documentType) || CORRECTION_GRANTED.equals(documentType) ? "Decision Notice" : documentTypeLabel);

        if (isPostHearingsEnabled) {
            PostHearing postHearing = caseData.getPostHearing();
            PostHearingReviewType postHearingReviewType = postHearing.getReviewType();

            if (nonNull(postHearingReviewType)) {
                if (PostHearingReviewType.PERMISSION_TO_APPEAL.equals(postHearingReviewType)
                        && PermissionToAppealActions.REVIEW.equals(postHearing.getPermissionToAppeal().getAction())) {
                    return "Review Decision Notice";
                }

                return postHearingReviewType.getDescriptionEn() + " Decision Notice";
            }

            if (isYes(postHearing.getCorrection().getIsCorrectionFinalDecisionInProgress())) {
                return documentTypeLabel;
            }
        }

        return embeddedDocumentTypeLabel;
    }

    /**
     * Override this method if previewDocument is not the correct field to set.
     */
    protected void setDocumentOnCaseData(SscsCaseData caseData, DocumentLink file) {
        caseData.getDocumentStaging().setPreviewDocument(file);
    }

    /**
     * Override this method if previewDocument is not the correct field to use.
     *
     * @return DocumentLink
     */
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getDocumentStaging().getPreviewDocument();
    }

    protected String buildFullName(SscsCaseData caseData) {
        StringBuilder fullNameText = new StringBuilder();
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            fullNameText.append(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle());
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());

        return fullNameText.toString();
    }

    protected Optional<String> buildAppointeeName(SscsCaseData caseData) {
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            return Optional.of(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle());
        } else {
            return Optional.empty();
        }
    }
}
