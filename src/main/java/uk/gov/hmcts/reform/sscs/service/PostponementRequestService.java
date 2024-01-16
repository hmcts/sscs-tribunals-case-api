package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;

@Service
public class PostponementRequestService {

    public void processPostponementRequest(SscsCaseData sscsCaseData, UploadParty uploadParty) {
        ensureSscsDocumentsIsNotNull(sscsCaseData);
        final SscsDocument sscsDocument = buildNewSscsDocumentFromPostponementRequest(sscsCaseData, uploadParty);
        addToSscsDocuments(sscsCaseData, sscsDocument);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST);
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(YES);
        clearTransientFields(sscsCaseData);
    }

    private SscsDocument buildNewSscsDocumentFromPostponementRequest(SscsCaseData sscsCaseData, UploadParty uploadParty) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentLink(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument())
                .documentFileName(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument().getDocumentFilename())
                .documentType(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .originalPartySender(uploadParty.getValue())
                .build()).build();
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        sscsCaseData.getSscsDocument().add(sscsDocument);
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getPostponementRequest().setPostponementRequestDetails(null);
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingVenue(null);
        sscsCaseData.getPostponementRequest().setPostponementPreviewDocument(null);
        sscsCaseData.getPostponementRequest().setPostponementRequestHearingDateAndTime(null);
    }

    private void ensureSscsDocumentsIsNotNull(SscsCaseData sscsCaseData) {
        final List<SscsDocument> sscsDocuments = (sscsCaseData.getSscsDocument() == null) ? new ArrayList<>() : sscsCaseData.getSscsDocument();
        sscsCaseData.setSscsDocument(sscsDocuments);
    }

    public void addCurrentHearingToExcludeDates(PreSubmitCallbackResponse<SscsCaseData> response) {
        SscsCaseData caseData = response.getData();
        Hearing hearing = caseData.getLatestHearing();
        if (hearing == null) {
            response.addError("There are no hearing to postpone");
            return;
        }

        ExcludeDate excludedDate = ExcludeDate.builder()
            .value(DateRange.builder()
                .start(hearing.getValue().getStart().toLocalDate().toString())
                .end(hearing.getValue().getEnd().toLocalDate().toString())
                .build())
            .build();

        List<ExcludeDate> excludeDates = Optional.ofNullable(caseData.getAppeal().getHearingOptions().getExcludeDates())
            .orElse(new ArrayList<>());

        excludeDates.add(excludedDate);

        caseData.getAppeal().getHearingOptions().setExcludeDates(excludeDates);
    }
}
