package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostHearingReviewAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Value("${feature.postHearings.enabled}")
    private final boolean isPostHearingsEnabled;

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

        PostHearingReviewType typeSelected = caseData.getPostHearing().getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        addDocument(caseData);

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void addDocument(SscsCaseData caseData) {
        DocumentLink previewDocument = caseData.getDocumentStaging().getPreviewDocument();

        if (nonNull(previewDocument)) {
            SscsDocumentDetails documentDetails = SscsDocumentDetails.builder()
                .dateApproved(LocalDate.now().toString())
                .documentFileName(getPostHearingFileName(caseData.getPostHearing()))
                .documentLink(previewDocument)
                .build();

            List<SscsDocument> documents = caseData.getSscsDocument();

            if (isNull(documents)) {
                documents = new LinkedList<>();
            }

            documents.add(new SscsDocument(documentDetails));
            caseData.setSscsDocument(documents);
        }
    }

    private String getPostHearingFileName(PostHearing postHearing) {
        if (SetAsideActions.REFUSE.equals(postHearing.getSetAside().getAction())) {
            return "Set aside refused decision notice";
        } else if (CorrectionActions.REFUSE.equals(postHearing.getCorrection().getAction())) {
            return "Correction refused decision notice";
        }

        throw new RuntimeException("Can't get preview document name");
    }
}
