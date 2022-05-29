package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.DATE_FORMAT_YYYYMMDD;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@Service
public class PostponementRequestService {

    public void processPostponementRequest(SscsCaseData sscsCaseData, UploadParty uploadParty) {
        ensureSscsDocumentsIsNotNull(sscsCaseData);
        final SscsDocument sscsDocument = buildNewSscsDocumentFromPostponementRequest(sscsCaseData, uploadParty);
        addToSscsDocuments(sscsCaseData, sscsDocument);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());
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

    public void setHearingDateAsExcludeDate(Hearing hearing, SscsCaseData sscsCaseData) {
        List<ExcludeDate> newExcludeDates = new ArrayList<>();
        if (sscsCaseData.getAppeal().getHearingOptions().getExcludeDates() != null) {
            newExcludeDates.addAll(sscsCaseData.getAppeal().getHearingOptions().getExcludeDates());
        }

        DateRange dateRange = DateRange.builder()
                .start(getLocalDate(hearing.getValue().getHearingDate()))
                .end(getLocalDate(hearing.getValue().getHearingDate()))
                .build();
        newExcludeDates.add(ExcludeDate.builder().value(dateRange).build());

        sscsCaseData.getAppeal().getHearingOptions().setExcludeDates(newExcludeDates);
    }

    private static String getLocalDate(String dateStr) {
        LocalDate localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(DATE_FORMAT_YYYYMMDD));
        return localDate.toString();
    }
}
