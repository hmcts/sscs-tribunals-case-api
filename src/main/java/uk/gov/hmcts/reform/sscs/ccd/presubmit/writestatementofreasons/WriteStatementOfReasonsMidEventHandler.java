package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.model.docassembly.GenerateFileParams;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteStatementOfReasonsTemplateBody;

import java.time.LocalDate;

@Component
@Slf4j
public class WriteStatementOfReasonsMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_DOCUMENT = "generateDocument";
    public static final String SSCS_URL = "www.gov.uk/appeal-benefit-decision";
    public static final String HMCTS_PNG = "[userImage:hmcts.png]";

    private final boolean isPostHearingsEnabled;
    private final GenerateFile generateFile;
    private final String templateIdEnglish;
    private final String templateIdWelsh;

    WriteStatementOfReasonsMidEventHandler(
        @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled,
        GenerateFile generateFile,
        @Value("${documents.english.SOR_WRITE}") String templateIdEnglish,
        @Value("${documents.welsh.SOR_WRITE}") String templateIdWelsh
    ) {
        this.isPostHearingsEnabled = isPostHearingsEnabled;
        this.generateFile = generateFile;
        this.templateIdEnglish = templateIdEnglish;
        this.templateIdWelsh = templateIdWelsh;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.SOR_WRITE
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        log.info("Write Statement of Reasons: handling callback with pageId {} for caseId {}", pageId, caseData.getCcdCaseId());

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && isYes(caseData.getDocumentGeneration().getWriteStatementOfReasonsGenerateNotice())) {
            processSorPdfAndSetPreviewDocument(userAuthorisation, caseData);
        }

        return response;
    }

    void processSorPdfAndSetPreviewDocument(
        String userAuthorisation,
        SscsCaseData caseData
    ) {
        String templateId;
        String language;
        if (caseData.isLanguagePreferenceWelsh()) {
            templateId = templateIdWelsh;
            language = "Welsh";
        } else {
            templateId = templateIdEnglish;
            language = "English";
        }

        log.info("Write Statement of Reasons: Generating {} notice for caseId {}", language, caseData.getCcdCaseId());
        DocumentLink previewDocument = getPreviewDocument(userAuthorisation, generateFile, templateId, caseData);
        caseData.getDocumentStaging().setPreviewDocument(previewDocument);
    }

    protected static DocumentLink getPreviewDocument(
        String userAuthorisation,
        GenerateFile generateFile,
        String templateId,
        SscsCaseData caseData
    ) {
        String requestDetails = caseData.getDocumentGeneration().getWriteStatementOfReasonsBodyContent(); // TODO where does this go?
        Appellant appellant = caseData.getAppeal().getAppellant();
        GenerateFileParams params = GenerateFileParams.builder()
            .renditionOutputLocation(null)
            .templateId(templateId)
            .formPayload(WriteStatementOfReasonsTemplateBody.builder()
                // Todo fill in
                .name(appellant.getName().getFullNameNoTitle())
                .sscsUrl(SSCS_URL)
                .hmcts2(HMCTS_PNG)
                .benefitNameAcronym(caseData.getBenefitCode()) // TODO what should this be
                .benefitNameAcronymWelsh(caseData.getBenefitCode()) // TODO what should this be
                .appealRef(caseData.getCcdCaseId()) //TODO what should this be
                .phoneNumber(appellant.getContact().getPhone())
                .hearingDate(LocalDate.parse(caseData.getLatestHearing().getValue().getHearingDate()))
                .entityType("Appellant") // TODO determine this
                .addressName("BLAh")//TODO what should this be
                .addressLine1(appellant.getAddress().getLine1())
                .addressLine2(appellant.getAddress().getLine2())
                .town(appellant.getAddress().getTown())
                .county(appellant.getAddress().getCounty())
                .postcode(appellant.getAddress().getPostcode())
                .generatedDate(LocalDate.now())
                .build())
            .userAuthentication(userAuthorisation)
            .build();
        final String generatedFileUrl = generateFile.assemble(params);

        return DocumentLink.builder()
            .documentFilename("Statement of Reasons.pdf")
            .documentBinaryUrl(generatedFileUrl + "/binary")
            .documentUrl(generatedFileUrl)
            .build();
    }

}
