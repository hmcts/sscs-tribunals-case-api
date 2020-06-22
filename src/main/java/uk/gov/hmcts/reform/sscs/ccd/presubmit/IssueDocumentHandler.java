package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;

@Slf4j
public class IssueDocumentHandler {

    private static final String GLASGOW = "GLASGOW";

    // Fields used for a short period in case progression are transient,
    // relevant for a short period of the case lifecycle.
    protected void clearTransientFields(SscsCaseData caseData, State beforeState) {
        caseData.setBodyContent(null);
        caseData.setPreviewDocument(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setGenerateNotice(null);
        caseData.setDateAdded(null);
        caseData.setSscsInterlocDirectionDocument(null);
        caseData.setSscsInterlocDecisionDocument(null);
        caseData.setExtensionNextEventDl(null);

        if (caseData.getDirectionTypeDl() != null && !DirectionType.APPEAL_TO_PROCEED.toString().equals(caseData.getDirectionTypeDl().getValue().getCode())
                || !State.INTERLOCUTORY_REVIEW_STATE.equals(beforeState)) {
            caseData.setDirectionTypeDl(null);
        }
    }

    protected DirectionOrDecisionIssuedTemplateBody createPayload(SscsCaseData caseData, String documentTypeLabel, LocalDate dateAdded, boolean isScottish, String userAuthorisation) {
        DirectionOrDecisionIssuedTemplateBody formPayload = DirectionOrDecisionIssuedTemplateBody.builder()
            .appellantFullName(buildFullName(caseData))
            .caseId(caseData.getCcdCaseId())
            .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
            .noticeBody(caseData.getBodyContent())
            .userName(caseData.getSignedBy())
            .noticeType(documentTypeLabel.toUpperCase())
            .userRole(caseData.getSignedRole())
            .dateAdded(dateAdded)
            .generatedDate(LocalDate.now())
            .build();

        if (isScottish) {
            formPayload = formPayload.toBuilder().image(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE).build();
        }
        return formPayload;
    }

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback, DocumentType documentType, String templateId, GenerateFile generateFile, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String documentUrl = Optional.ofNullable(getDocumentFromCaseData(caseData)).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now());

        String documentTypeLabel = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);

        FormPayload formPayload = createPayload(caseData, documentTypeLabel, dateAdded, isScottish, userAuthorisation);

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(documentUrl)
                .templateId(templateId)
                .formPayload(formPayload)
                .userAuthentication(userAuthorisation)
                .build();

        log.info(String.format("Generating %s document isScottish = %s", documentTypeLabel, isScottish));

        final String generatedFileUrl = generateFile.assemble(params);

        documentTypeLabel = documentTypeLabel + (DRAFT_DECISION_NOTICE.equals(documentType) ? " generated" : " issued");

        final String filename = String.format("%s on %s.pdf", documentTypeLabel, dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
                .documentFilename(filename)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();

        setDocumentOnCaseData(caseData, previewFile);

        return new PreSubmitCallbackResponse<>(caseData);
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
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), ' ', '.'));

        return fullNameText.toString();
    }
}
