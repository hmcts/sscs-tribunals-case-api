package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
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
    protected void clearTransientFields(SscsCaseData caseData, State state) {
        caseData.setBodyContent(null);
        caseData.setPreviewDocument(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setGenerateNotice(null);
        caseData.setDateAdded(null);
        caseData.setSscsInterlocDirectionDocument(null);
        caseData.setSscsInterlocDecisionDocument(null);

        if (!DirectionType.APPEAL_TO_PROCEED.equals(caseData.getDirectionType()) || !state.equals(State.INTERLOCUTORY_REVIEW_STATE)) {
            caseData.setDirectionType(null);
        }
    }

    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback, DocumentType documentType, String templateId, GenerateFile generateFile, String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String documentUrl = Optional.ofNullable(caseData.getPreviewDocument()).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now());

        DirectionOrDecisionIssuedTemplateBody formPayload = DirectionOrDecisionIssuedTemplateBody.builder()
                .appellantFullName(buildFullName(caseData))
                .caseId(caseData.getCcdCaseId())
                .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
                .noticeBody(caseData.getBodyContent())
                .userName(caseData.getSignedBy())
                .noticeType(documentType.getValue().toUpperCase())
                .userRole(caseData.getSignedRole())
                .dateAdded(dateAdded)
                .generatedDate(LocalDate.now())
                .build();

        boolean isScottish = Optional.ofNullable(caseData.getRegionalProcessingCenter()).map(f -> equalsIgnoreCase(f.getName(), GLASGOW)).orElse(false);

        if (isScottish) {
            formPayload = formPayload.toBuilder().image(DirectionOrDecisionIssuedTemplateBody.SCOTTISH_IMAGE).build();
        }

        GenerateFileParams params = GenerateFileParams.builder()
                .renditionOutputLocation(documentUrl)
                .templateId(templateId)
                .formPayload(formPayload)
                .userAuthentication(userAuthorisation)
                .build();

        log.info(String.format("Generating %s document isScottish = %s", documentType.getValue(), isScottish));

        final String generatedFileUrl = generateFile.assemble(params);

        final String filename = String.format("%s issued on %s.pdf", documentType.getValue(), dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
                .documentFilename(filename)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();
        caseData.setPreviewDocument(previewFile);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private String buildFullName(SscsCaseData caseData) {
        StringBuilder fullNameText = new StringBuilder();
        if (caseData.getAppeal().getAppellant().getIsAppointee() != null && caseData.getAppeal().getAppellant().getIsAppointee().equalsIgnoreCase("Yes") && caseData.getAppeal().getAppellant().getAppointee().getName() != null) {
            fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(), ' ', '.'));
            fullNameText.append(", appointee for ");
        }

        fullNameText.append(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), ' ', '.'));

        return fullNameText.toString();
    }
}
