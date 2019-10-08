package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.WordUtils;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.DirectionOrDecisionIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;

@Component
@Slf4j
public class DecisionIssuedMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private static final String GLASGOW = "GLASGOW";
    private static final String NOTICE_TYPE = "DECISION NOTICE";

    private final GenerateFile generateFile;
    private final String templateId;

    @Autowired
    public DecisionIssuedMidEventHandler(GenerateFile generateFile, @Value("${doc_assembly.decision_issued}") String templateId) {
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType == CallbackType.MID_EVENT
                && callback.getEvent() == EventType.DECISION_ISSUED
                && Objects.nonNull(callback.getCaseDetails())
                && Objects.nonNull(callback.getCaseDetails().getCaseData())
                && callback.getCaseDetails().getCaseData().isGenerateNotice();
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String documentUrl = Optional.ofNullable(caseData.getPreviewDocument()).map(DocumentLink::getDocumentUrl).orElse(null);

        LocalDate dateAdded = Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now());

        DirectionOrDecisionIssuedTemplateBody formPayload = DirectionOrDecisionIssuedTemplateBody.builder()
                .appellantFullName(WordUtils.capitalizeFully(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(), ' ', '.'))
                .caseId(caseData.getCcdCaseId())
                .nino(caseData.getAppeal().getAppellant().getIdentity().getNino())
                .noticeBody(caseData.getBodyContent())
                .userName(caseData.getSignedBy())
                .noticeType(NOTICE_TYPE)
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

        log.info(String.format("Generating Interloc decision issued document isScottish = %s", isScottish));

        final String generatedFileUrl = generateFile.assemble(params);

        final String filename = String.format("%s issued on %s.pdf", DocumentType.DECISION_NOTICE.getValue(), dateAdded.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

        DocumentLink previewFile = DocumentLink.builder()
                .documentFilename(filename)
                .documentBinaryUrl(generatedFileUrl + "/binary")
                .documentUrl(generatedFileUrl)
                .build();
        caseData.setPreviewDocument(previewFile);

        return new PreSubmitCallbackResponse<>(caseData);
    }

}
