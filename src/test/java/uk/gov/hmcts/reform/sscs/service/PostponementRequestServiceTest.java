package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;

@RunWith(JUnitParamsRunner.class)
public class PostponementRequestServiceTest {

    private final PostponementRequestService service = new PostponementRequestService();

    @Test
    @Parameters({"REP", "APPELLANT", "APPOINTEE"})
    public void processPostponementRequest(UploadParty uploadParty) {
        DynamicListItem value = new DynamicListItem(uploadParty.getValue(), uploadParty.getLabel());
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));
        SscsCaseData sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .directionDueDate("01/02/2020")
                .postponementRequest(PostponementRequest.builder()
                        .postponementRequestDetails("Here are some details")
                        .postponementRequestHearingVenue("Venue 1")
                        .postponementPreviewDocument(DocumentLink.builder()
                                .documentBinaryUrl("http://example.com")
                                .documentFilename("example.pdf")
                                .build()).build())
                .originalSender(originalSender).build();

        service.processPostponementRequest(sscsCaseData, uploadParty);

        assertThat(sscsCaseData.getInterlocReviewState(), is(InterlocReviewState.REVIEW_BY_TCW.getId()));
        assertThat(sscsCaseData.getInterlocReferralReason(), is(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId()));
        assertNull(sscsCaseData.getPostponementRequest().getPostponementRequestDetails());
        assertNull(sscsCaseData.getPostponementRequest().getPostponementPreviewDocument());

        assertEquals(1, sscsCaseData.getSscsDocument().size());
        final SscsDocument document = sscsCaseData.getSscsDocument().get(0);
        assertEquals(DocumentType.POSTPONEMENT_REQUEST.getValue(), document.getValue().getDocumentType());
        assertEquals("example.pdf", document.getValue().getDocumentLink().getDocumentFilename());
        assertEquals(uploadParty.getValue(), document.getValue().getOriginalPartySender());
    }
}
