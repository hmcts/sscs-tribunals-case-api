package uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class PostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.POSTPONEMENT_REQUEST
                && callback.getCaseDetails() != null;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        final SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();

        final PreSubmitCallbackResponse<SscsCaseData> response = validatePostponementRequest(sscsCaseData);

        if (response.getErrors().isEmpty()) {
            processPostponementRequest(sscsCaseData);
        }

        return response;
    }

    private void processPostponementRequest(SscsCaseData sscsCaseData) {
        ensureSscsDocumentsIsNotNull(sscsCaseData);
        final SscsDocument sscsDocument = buildNewSscsDocumentFromPostponementRequest(sscsCaseData);
        addToSscsDocuments(sscsCaseData, sscsDocument);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(YES);
        clearTransientFields(sscsCaseData);
    }

    @NotNull
    private PreSubmitCallbackResponse<SscsCaseData> validatePostponementRequest(SscsCaseData sscsCaseData) {
        final PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);
        if (sscsCaseData.getPostponementRequest().getPostponementPreviewDocument() == null) {
            response.addError("There is no postponement request document");
        }
        return response;
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingVenue(null);
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(null);
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingDateAndTime(null);
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        sscsCaseData.getSscsDocument().add(sscsDocument);
    }

    private SscsDocument buildNewSscsDocumentFromPostponementRequest(SscsCaseData sscsCaseData) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentLink(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument())
                .documentFileName(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument().getDocumentFilename())
                .documentType(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .partyUploaded(UploadParty.DWP)
                .shouldBundleIncludeDocLink(NO)
                .build()).build();
    }

    private void ensureSscsDocumentsIsNotNull(SscsCaseData sscsCaseData) {
        final List<SscsDocument> sscsDocuments = (sscsCaseData.getSscsDocument() == null) ? new ArrayList<>() : sscsCaseData.getSscsDocument();
        sscsCaseData.setSscsDocument(sscsDocuments);
    }

}
