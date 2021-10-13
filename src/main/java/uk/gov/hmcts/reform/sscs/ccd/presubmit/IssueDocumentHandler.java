package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.text.WordUtils.capitalizeFully;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Slf4j
public class IssueDocumentHandler {

    private static final String GLASGOW = "GLASGOW";
    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    protected void clearTransientFields(SscsCaseData caseData, State beforeState) {
        clearBasicTransientFields(caseData);
        caseData.setExtensionNextEventDl(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);

    }

    protected void clearBasicTransientFields(SscsCaseData caseData) {
        caseData.setBodyContent(null);
        caseData.setDirectionNoticeContent(null);
        caseData.setPreviewDocument(null);
        caseData.setGenerateNotice(null);
        caseData.setWriteFinalDecisionGenerateNotice(null);
        caseData.setDateAdded(null);
        caseData.setSscsInterlocDirectionDocument(null);
        caseData.setSscsInterlocDecisionDocument(null);
        caseData.setAdjournCasePreviewDocument(null);
    }

    protected NoticeIssuedTemplateBody createPayload(PreSubmitCallbackResponse<SscsCaseData> response, SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, LocalDate generatedDate, boolean isScottish, String userAuthorisation) {
        NoticeIssuedTemplateBody formPayload = NoticeIssuedTemplateBody.builder()
                .appellantFullName(buildFullName(caseData))
                .appointeeFullName(buildAppointeeName(caseData).orElse(null))
                .caseId(caseData.getCcdCaseId())
                .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
                .noticeBody(ofNullable(caseData.getBodyContent())
                        .orElse(caseData.getDirectionNoticeContent()))
                .userName(caseData.getSignedBy())
                .noticeType(documentTypeLabel.toUpperCase())
                .userRole(caseData.getSignedRole())
                .dateAdded(dateAdded)
                .generatedDate(generatedDate)
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

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback, DocumentType documentType, String templateId, GenerateFile generateFile, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if ((ADJOURNMENT_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType)) && caseData.getAdjournCaseGenerateNotice() == null) {
            throw new IllegalStateException("Generate notice has not been set");
        }

        String documentUrl = ofNullable(getDocumentFromCaseData(caseData)).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = ofNullable(caseData.getDateAdded()).orElse(LocalDate.now());

        String documentTypeLabel = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        String embeddedDocumentTypeLabel = (FINAL_DECISION_NOTICE.equals(documentType) ? "Decision Notice" : documentTypeLabel);

        boolean isScottish = ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);

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

        log.info(format("Generating %s document isScottish = %s", documentTypeLabel, isScottish));

        final String generatedFileUrl = generateFile.assemble(params);

        documentTypeLabel = documentTypeLabel + ((DRAFT_DECISION_NOTICE.equals(documentType) || DRAFT_ADJOURNMENT_NOTICE.equals(documentType)) ? " generated" : " issued");

        final String filename = format("%s on %s.pdf", documentTypeLabel, dateAdded.format(DATE_PATTERN));

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
        caseData.setPreviewDocument(file);
    }

    /**
     * Override this method if previewDocument is not the correct field to use.
     *
     * @return DocumentLink
     */
    protected DocumentLink getDocumentFromCaseData(SscsCaseData caseData) {
        return caseData.getPreviewDocument();
    }

    protected String buildFullName(SscsCaseData caseData) {
        StringBuilder fullNameText = new StringBuilder();
        if (isYes(caseData.getAppeal().getAppellant().getIsAppointee()) && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            fullNameText.append(capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(capitalizeFully(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), ' ', '.'));

        return fullNameText.toString();
    }

    protected Optional<String> buildAppointeeName(SscsCaseData caseData) {
        if (isYes(caseData.getAppeal().getAppellant().getIsAppointee()) && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            return Optional.of(capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
        }
        return empty();
    }
}
