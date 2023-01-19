package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;

@Service
public class PostHearingRequestService {

    public void processPostHearingRequest(SscsCaseData sscsCaseData, UploadParty uploadParty) {
        ensureSscsDocumentsIsNotNull(sscsCaseData);
        final SscsDocument sscsDocument = buildNewSscsDocumentFromPostponementRequest(sscsCaseData, uploadParty);
        addToSscsDocuments(sscsCaseData, sscsDocument);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        clearTransientFields(sscsCaseData);
    }

    private void ensureSscsDocumentsIsNotNull(SscsCaseData sscsCaseData) {
        final List<SscsDocument> sscsDocuments = (sscsCaseData.getSscsDocument() == null) ? new ArrayList<>() : sscsCaseData.getSscsDocument();
        sscsCaseData.setSscsDocument(sscsDocuments);
    }

    private SscsDocument buildNewSscsDocumentFromPostponementRequest(SscsCaseData sscsCaseData, UploadParty uploadParty) {
        PostHearing postHearing = sscsCaseData.getPostHearing();
        DocumentType documentType;

        switch (postHearing.getRequestType()) {
            case SET_ASIDE:
                documentType = DocumentType.SET_ASIDE_APPLICATION;
                break;
            default:
                throw new IllegalArgumentException("Unexpected request type: " + postHearing.getRequestType());
        }

        return SscsDocument.builder().value(SscsDocumentDetails.builder()
            .documentLink(postHearing.getPreviewDocument())
            .documentFileName(postHearing.getPreviewDocument().getDocumentFilename())
            .documentType(documentType.getValue())
            .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
            .originalPartySender(uploadParty.getValue())
            .build()).build();
    }

    private void addToSscsDocuments(SscsCaseData sscsCaseData, SscsDocument sscsDocument) {
        sscsCaseData.getSscsDocument().add(sscsDocument);
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getPostHearing().setRequestReason(null);
        sscsCaseData.getPostHearing().setPreviewDocument(null);
    }
}
