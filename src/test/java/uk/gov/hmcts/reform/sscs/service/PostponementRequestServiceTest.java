package uk.gov.hmcts.reform.sscs.service;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest.PostponementRequestAboutToStartHandlerTest.getHearing;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.postponementrequest.PostponementRequestAboutToStartHandlerTest.getHearingOptions;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
    private SscsCaseData sscsCaseData;

    @Test
    @Parameters({"REP", "APPELLANT", "APPOINTEE"})
    public void processPostponementRequest(UploadParty uploadParty) {
        DynamicListItem value = new DynamicListItem(uploadParty.getValue(), uploadParty.getLabel());
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
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

    @Test
    public void shouldAddHearingDateToExcludeDateWhichIsNull() {
        HearingOptions hearingOptions = getHearingOptions();
        sscsCaseData = setSscsCaseData(hearingOptions);

        service.setHearingDateAsExcludeDate(getHearing(1), sscsCaseData);

        assertEquals(sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().get(0).getValue().getStart(),
                LocalDate.now().plusDays(1).toString());
        assertEquals(sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().get(0).getValue().getEnd(),
                LocalDate.now().plusDays(1).toString());
    }

    @Test
    public void shouldAddHearingDateToExistingExcludeDate() {
        HearingOptions hearingOptions = getHearingOptions();
        List<ExcludeDate> excludeDate = Arrays.asList(
                ExcludeDate.builder().value(DateRange.builder()
                                        .start(LocalDate.now().toString())
                                        .end(LocalDate.now().toString())
                                        .build()).build());

        hearingOptions.setExcludeDates(excludeDate);

        sscsCaseData = setSscsCaseData(hearingOptions);
        service.setHearingDateAsExcludeDate(getHearing(1), sscsCaseData);

        assertEquals(2, sscsCaseData.getAppeal().getHearingOptions().getExcludeDates().size());
    }

    private SscsCaseData setSscsCaseData(HearingOptions hearingOptions) {
        Hearing hearing = getHearing(1);
        List<Hearing> hearings  = List.of(hearing);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().hearingOptions(hearingOptions).build())
                .hearings(hearings).build();

        return sscsCaseData;
    }
}
