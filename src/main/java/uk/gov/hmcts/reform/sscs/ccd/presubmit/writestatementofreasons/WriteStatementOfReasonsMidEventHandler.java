package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;

@Component
@Slf4j
public class WriteStatementOfReasonsMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_DOCUMENT = "generateDocument";

    private final boolean isPostHearingsEnabled;
    private final GenerateFile generateFile;
    private final String templateIdEnglish;
    private final String templateIdWelsh;

    WriteStatementOfReasonsMidEventHandler(
        @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled,
        GenerateFile generateFile,
        @Value("${documents.english.WRITE_SOR}") String templateIdEnglish,
        @Value("${documents.welsh.WRITE_SOR}") String templateIdWelsh
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
        String caseId = caseData.getCcdCaseId();
        log.info("Write Statement of Reasons: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        String templateId;
        String language;
        if (caseData.isLanguagePreferenceWelsh()) {
            templateId = templateIdWelsh;
            language = "Welsh";
        } else {
            templateId = templateIdEnglish;
            language = "English";
        }

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && isYes(caseData.getDocumentGeneration().getWriteStatementOfReasonsGenerateNotice())) {
            log.info("Write Statement of Reasons: Generating {} notice for caseId {}", language, caseId);
            PdfRequestUtil.processRequestPdfAndSetPreviewDocument(PdfRequestUtil.PdfType.STATEMENT_OF_REASONS,
                userAuthorisation, caseData, response, generateFile, templateId, isPostHearingsEnabled);
        }

        return response;
    }

}
