package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.IssueFinalDecisionTemplateBody;

@Component
@Slf4j
public class WriteFinalDecisionMidEventHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final GenerateFile generateFile;
    private final String templateId;

    @Autowired
    public WriteFinalDecisionMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.issue_final_decision}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
            && callback.getEvent() == EventType.WRITE_FINAL_DECISION
            && Objects.nonNull(callback.getCaseDetails())
            && Objects.nonNull(callback.getCaseDetails().getCaseData());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);

        if (isDecisionNoticeDatesInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice end date must be after decision notice start date");
        }
        if (isDecisionNoticeDateOfDecisionInvalid(sscsCaseData)) {
            preSubmitCallbackResponse.addError("Decision notice date of decision must not be in the future");
        }

        if (sscsCaseData.isPipWriteFinalDecisionGenerateNotice()) {
            return issueDocument(callback, DocumentType.DECISION_NOTICE, templateId, generateFile, userAuthorisation);
        }

        return preSubmitCallbackResponse;
    }

    private boolean isDecisionNoticeDatesInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getPipWriteFinalDecisionStartDate() != null && sscsCaseData.getPipWriteFinalDecisionEndDate() != null) {
            LocalDate decisionNoticeStartDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionStartDate());
            LocalDate decisionNoticeEndDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionEndDate());
            return !decisionNoticeStartDate.isBefore(decisionNoticeEndDate);
        }
        return false;
    }

    private boolean isDecisionNoticeDateOfDecisionInvalid(SscsCaseData sscsCaseData) {
        if (sscsCaseData.getPipWriteFinalDecisionDateOfDecision() != null) {
            LocalDate decisionNoticeDecisionDate = LocalDate.parse(sscsCaseData.getPipWriteFinalDecisionDateOfDecision());
            LocalDate today = LocalDate.now();
            return decisionNoticeDecisionDate.isAfter(today);
        }
        return false;
    }

    @Override
    protected PreSubmitCallbackResponse<SscsCaseData> issueDocument(Callback<SscsCaseData> callback,
        DocumentType documentType, String templateId, GenerateFile generateFile, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String documentUrl = Optional.ofNullable(caseData.getPreviewDocument()).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now());

        String documentTypeLabel = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();

        IssueFinalDecisionTemplateBody formPayload = IssueFinalDecisionTemplateBody.builder()
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

        log.info(String.format("Generating %s document isScottish = %s", documentTypeLabel, isScottish));

        final String generatedFileUrl = generateFile.assemble(params);

        final String filename = String.format("%s issued on %s.pdf", documentTypeLabel, dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
            .documentFilename(filename)
            .documentBinaryUrl(generatedFileUrl + "/binary")
            .documentUrl(generatedFileUrl)
            .build();
        caseData.setPreviewDocument(previewFile);

        return new PreSubmitCallbackResponse<>(caseData);
    }


}
