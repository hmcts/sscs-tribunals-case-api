package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.GENERATE;

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
    private final String templateId;

    WriteStatementOfReasonsMidEventHandler(
        @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled,
        GenerateFile generateFile,
        @Value("${doc_assembly.write_statementofreasons}") String templateId
    ) {
        this.isPostHearingsEnabled = isPostHearingsEnabled;
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.WRITE_STATEMENT_OF_REASONS
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

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && GENERATE.equals(caseData.getPostHearing().getRequestFormat())) {
            log.info("Write Statement of Reasons: Generating notice for caseId {}", caseId);
            PdfRequestUtil.processRequestPdfAndSetPreviewDocument(PdfRequestUtil.PdfType.POST_HEARING,
                userAuthorisation, caseData, response, generateFile, templateId, isPostHearingsEnabled);
        }

        return response;
    }

}
