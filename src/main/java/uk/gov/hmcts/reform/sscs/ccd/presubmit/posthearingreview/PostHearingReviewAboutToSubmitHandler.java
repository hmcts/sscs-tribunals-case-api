package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingreview;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType.CORRECTION;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

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

        PostHearing postHearing = caseData.getPostHearing();

        PostHearingReviewType typeSelected = postHearing.getReviewType();
        log.info("Review Post Hearing App: handling action {} for case {}", typeSelected,  caseId);

        if (CORRECTION.equals(typeSelected) && GRANT.equals(postHearing.getCorrection().getAction())) {
            DocumentLink correctedDecision = caseData.getDocumentStaging().getPreviewDocument();

            SscsDocumentDetails documentDetails = SscsDocumentDetails.builder()
                .dateApproved(LocalDate.now().toString())
                .documentFileName("Corrected decision notice")
                .documentLink(correctedDecision)
                .build();

            List<SscsDocument> documents = caseData.getSscsDocument();

            if (isNull(documents)) {
                documents = new LinkedList<>();
                caseData.setSscsDocument(documents);
            }

            documents.add(new SscsDocument(documentDetails));

        }

        SscsUtil.clearDocumentTransientFields(caseData);

        return new PreSubmitCallbackResponse<>(caseData);
    }
}
