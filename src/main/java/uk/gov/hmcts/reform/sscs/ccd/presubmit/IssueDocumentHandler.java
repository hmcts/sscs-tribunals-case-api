package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.Respondent;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Slf4j
public class IssueDocumentHandler {

    private static final String GLASGOW = "GLASGOW";

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    protected void clearTransientFields(SscsCaseData caseData, State beforeState) {
        clearBasicTransientFields(caseData);
        caseData.setExtensionNextEventDl(null);
    }

    protected void clearBasicTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
        caseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(null);
        caseData.setSscsInterlocDirectionDocument(null);
        caseData.setSscsInterlocDecisionDocument(null);
        caseData.getAdjournment().setPreviewDocument(null);
    }

    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response, SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish, String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = NoticeIssuedTemplateBody.builder()
                .appellantFullName(buildFullName(caseData))
                .appointeeFullName(buildAppointeeName(caseData).orElse(null))
                .caseId(caseData.getCcdCaseId())
                .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
                .shouldHideNino(isBenefitTypeValidToHideNino(caseData.getBenefitType()))
                .respondents(getRespondents(caseData))
                .noticeBody(Optional.ofNullable(caseData.getDocumentGeneration().getBodyContent())
                        .orElse(caseData.getDocumentGeneration().getDirectionNoticeContent()))
                .userName(caseData.getDocumentGeneration().getSignedBy())
                .noticeType(documentTypeLabel.toUpperCase())
                .userRole(caseData.getDocumentGeneration().getSignedRole())
                .dateAdded(dateAdded)
                .generatedDate(generatedDate)
                .idamSurname(caseData.getDocumentGeneration().getSignedBy())
                .build();

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

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(SscsCaseData caseData,
                                                                    DocumentType documentType,
                                                                    String templateId,
                                                                    GenerateFile generateFile,
                                                                    String userAuthorisation) {
        if ((ADJOURNMENT_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType))
            && caseData.getAdjournment().getGenerateNotice() == null) {
            throw new IllegalStateException("Generate notice has not been set");
        }

        String documentUrl = Optional.ofNullable(getDocumentFromCaseData(caseData)).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now());

        String documentTypeLabel = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        String embeddedDocumentTypeLabel = (FINAL_DECISION_NOTICE.equals(documentType) ? "Decision Notice" : documentTypeLabel);

        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        FormPayload formPayload = createPayload(response, caseData, embeddedDocumentTypeLabel, dateAdded, LocalDate.now(), isScottish, userAuthorisation);

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

        documentTypeLabel = documentTypeLabel + ((DRAFT_DECISION_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType)) ? " generated" : " issued");

        final String filename = String.format("%s on %s.pdf", documentTypeLabel, dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
                .documentFilename(filename)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();

        setDocumentOnCaseData(caseData, previewFile);

        return response;
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
            fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), ' ', '.'));

        return fullNameText.toString();
    }

    protected Optional<String> buildAppointeeName(SscsCaseData caseData) {
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            return Optional.of(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
        } else {
            return Optional.empty();
        }
    }
}
