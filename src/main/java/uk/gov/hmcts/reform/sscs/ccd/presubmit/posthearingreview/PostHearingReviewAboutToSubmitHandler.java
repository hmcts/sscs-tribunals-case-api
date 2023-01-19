package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SET_ASIDE_REFUSED;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrespondenceType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.IssueDocumentHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler
    extends IssueDocumentHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;
    private final GenerateFile generateFile;
    private final DocumentConfiguration documentConfiguration;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent() == EventType.POST_HEARING_REVIEW
            && isPostHearingsEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        String caseId = caseData.getCcdCaseId();
        PostHearingReviewType typeSelected = caseData.getPostHearing().getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        handleDocuments(caseData, userAuthorisation);

        SscsUtil.clearDocumentTransientFields(caseData);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void handleDocuments(SscsCaseData caseData, String userAuthorisation) {
        SetAsideActions action = caseData.getPostHearing().getSetAside().getAction();

        if (nonNull(action)) {
            DocumentLink previewDocument = caseData.getDocumentStaging().getPreviewDocument();

            SscsDocumentDetails documentDetails = SscsDocumentDetails.builder()
                .documentDateAdded(LocalDate.now().toString())
                .documentType(DECISION_NOTICE.getValue())
                .documentFileName(action.getCallbackSummary() + ".pdf")
                .documentLink(previewDocument).build();

            caseData.getSscsDocument().add(new SscsDocument(documentDetails));

            // TODO: add cover letter to notifications sent
            String templateId = documentConfiguration.getDocuments()
                .get(caseData.getLanguagePreference()).get(SET_ASIDE_REFUSED);
            issueDocument(caseData, DECISION_NOTICE, templateId, generateFile, userAuthorisation);

            //TODO: add to process documents tab and send out

            CorrespondenceDetails correspondenceDetails = CorrespondenceDetails.builder()
                .sentOn(LocalDate.now().toString())
                .to("")
                .documentLink(previewDocument)
                .correspondenceType(CorrespondenceType.Email).build();

            caseData.getCorrespondence().add(new Correspondence(correspondenceDetails));
        }
    }
}
