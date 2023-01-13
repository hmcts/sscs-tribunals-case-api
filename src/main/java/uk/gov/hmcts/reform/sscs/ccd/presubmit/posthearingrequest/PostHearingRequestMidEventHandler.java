package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.GENERATE;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

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

        RequestFormat requestFormat = getRequestFormat(caseData.getPostHearing());

        if (PAGE_ID_GENERATE_DOCUMENT.equals(pageId) && GENERATE.equals(requestFormat)) {
            log.info("Post Hearing Request: Generating notice for caseId {}", caseId);
            SscsUtil.processPostHearingRequestPdfAndSetPreviewDocument(userAuthorisation, caseData, response, generateFile, templateId);
        }

        return response;
    }

    @Nullable
    private static RequestFormat getRequestFormat(PostHearing postHearing) {
        PostHearingRequestType typeSelected = postHearing.getRequestType();
        if (isNull(typeSelected)) {
            return null;
        }

        switch (typeSelected) {
            case SET_ASIDE:
                return postHearing.getSetAside().getRequestFormat();
            case CORRECTION:
                //return postHearing.getCorrection().getRequestFormat();
                break;
            case STATEMENT_OF_REASONS:
                //return postHearing.getStatementOfReasons().getRequestFormat();
                break;
            case PERMISSION_TO_APPEAL:
                //return postHearing.getPermissionToAppeal().getRequestFormat();
                break;
            case LIBERTY_TO_APPLY:
                //return postHearing.getLibertyToApply().getRequestFormat();
                break;
            default:
                return null;
        }
        return null;
    }
}
