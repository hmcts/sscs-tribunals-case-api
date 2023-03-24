package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getCcdCallbackMap;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getDocumentTypeFromReviewType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getEventTypeFromDocumentReviewTypeAndAction;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    private final DocumentConfiguration documentConfiguration;
    private final GenerateFile generateFile;
    private final FooterService footerService;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.POST_HEARING_REVIEW
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String caseId = caseData.getCcdCaseId();
        PostHearing postHearing = caseData.getPostHearing();
        PostHearingReviewType reviewType = postHearing.getReviewType();
        CcdCallbackMap action = getCcdCallbackMap(postHearing, reviewType);
        log.info("Review Post Hearing App: handling action {} for case {}", reviewType,  caseId);

        handlePostHearingReview(caseData, action, reviewType, userAuthorisation);

        SscsUtil.clearDocumentTransientFields(caseData);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void handlePostHearingReview(SscsCaseData caseData,
                                        CcdCallbackMap action,
                                        PostHearingReviewType reviewType,
                                        String userAuthorisation) {

        DocumentLink previewDocument = caseData.getDocumentStaging().getPreviewDocument();
        DocumentType documentType = getDocumentTypeFromReviewType(reviewType);
        String actionName = action.getCcdDefinition();
        String docFileName = action.getCallbackSummary() + ".pdf";

        SscsDocumentDetails documentDetails = SscsDocumentDetails.builder()
            .documentDateAdded(LocalDate.now().toString())
            .documentType(documentType.getValue())
            .documentFileName(docFileName)
            .documentLink(previewDocument)
            .build();

        SscsDocument document = new SscsDocument(documentDetails);
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(document);

        SscsUtil.addDocumentToBundle(footerService, caseData, documents.get(0), docFileName);

        EventType eventType = getEventTypeFromDocumentReviewTypeAndAction(reviewType, actionName);
        String templateId = documentConfiguration.getDocuments()
            .get(caseData.getLanguagePreference())
            .get(eventType);
        issueDocument(caseData, documentType, templateId, generateFile, userAuthorisation);
    }
}
