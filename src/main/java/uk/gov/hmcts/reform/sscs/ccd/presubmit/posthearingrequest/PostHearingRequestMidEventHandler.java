package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.GENERATE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;

@Component
@Slf4j
public class PostHearingRequestMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    public static final String PAGE_ID_GENERATE_DOCUMENT = "generateDocument";

    private final boolean isPostHearingsEnabled;
    private final GenerateFile generateFile;
    private final String templateId;

    PostHearingRequestMidEventHandler(
        @Value("${feature.postHearings.enabled}") boolean isPostHearingsEnabled,
        GenerateFile generateFile,
        @Value("${doc_assembly.posthearingrequest}") String templateId
    ) {
        this.isPostHearingsEnabled = isPostHearingsEnabled;
        this.generateFile = generateFile;
        this.templateId = templateId;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
            && callback.getEvent() == EventType.POST_HEARING_REQUEST
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String pageId = callback.getPageId();
        String caseId = caseData.getCcdCaseId();
        log.info("Post Hearing Request: handling callback with pageId {} for caseId {}", pageId, caseId);

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        PostHearingRequestType typeSelected = caseData.getPostHearing().getRequestType();
        log.info("Post Hearing Request: handling action {} for case {}", typeSelected,  caseId);

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId)) {
            caseData.getDocumentStaging().setPreviewDocument(null);

            if (GENERATE.equals(caseData.getPostHearing().getRequestFormat())) {
                log.info("Post Hearing Request: Generating notice for caseId {}", caseId);
                return PdfRequestUtil.processRequestPdfAndSetPreviewDocument(PdfRequestUtil.PdfType.POST_HEARING,
                        userAuthorisation, caseData, response, generateFile, templateId, isPostHearingsEnabled);
            }
        }

        return response;
    }

}
