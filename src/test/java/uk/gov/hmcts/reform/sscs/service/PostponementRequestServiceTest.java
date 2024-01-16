package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.DateRange;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;

@RunWith(JUnitParamsRunner.class)
public class PostponementRequestServiceTest {

    private static final LocalDateTime HEARING_DATE_TIME = LocalDateTime.of(2023, 12, 1, 1, 0);
    private static final LocalDateTime EXCLUDED_DATE_TIME = LocalDateTime.of(2024, 11, 2, 1, 0);
    public static final String DOCUMENT_FILENAME = "example.pdf";
    private final PostponementRequestService postponementRequestService = new PostponementRequestService();

    private SscsCaseData caseData;

    private PreSubmitCallbackResponse<SscsCaseData> response;

    @Before
    public void setup() {

        caseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
            .directionDueDate("01/02/2020")
            .postponementRequest(PostponementRequest.builder()
                .postponementRequestDetails("Here are some details")
                .postponementRequestHearingVenue("Venue 1")
                .postponementPreviewDocument(DocumentLink.builder()
                    .documentBinaryUrl("http://example.com")
                    .documentFilename(DOCUMENT_FILENAME)
                    .build()).build())
            .appeal(Appeal.builder()
                .appellant(Appellant.builder()
                    .name(Name.builder().build())
                    .identity(Identity.builder().build())
                    .build())
                .hearingOptions(HearingOptions.builder().build())
                .build())
            .hearings(new ArrayList<>(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                    .start(HEARING_DATE_TIME)
                    .end(HEARING_DATE_TIME)
                    .build())
                .build())))
            .build();

        response = new PreSubmitCallbackResponse<>(caseData);
    }

    @Test
    @Parameters({"REP", "APPELLANT", "APPOINTEE"})
    public void testProcessPostponementRequest(UploadParty uploadParty) {
        DynamicListItem value = new DynamicListItem(uploadParty.getValue(), uploadParty.getLabel());
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));
        caseData.setOriginalSender(originalSender);

        postponementRequestService.processPostponementRequest(caseData, uploadParty);

        assertThat(caseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_TCW);
        assertThat(caseData.getInterlocReferralReason()).isEqualTo(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST);
        assertThat(caseData.getPostponementRequest().getPostponementRequestDetails()).isNull();
        assertThat(caseData.getPostponementRequest().getPostponementPreviewDocument()).isNull();

        assertThat(caseData.getSscsDocument())
            .hasSize(1)
            .extracting(SscsDocument::getValue)
            .extracting("documentType", "documentFileName", "originalPartySender")
            .containsExactly(tuple(DocumentType.POSTPONEMENT_REQUEST.getValue(), DOCUMENT_FILENAME, uploadParty.getValue()));
    }

    @DisplayName("When case has a hearing and no existing excluded dates addCurrentHearingToExcludeDates adds the "
        + "hearing date to the list correctly")
    @Test
    public void testAddCurrentHearingToExcludeDates() {
        postponementRequestService.addCurrentHearingToExcludeDates(response);

        String expected = HEARING_DATE_TIME.toLocalDate().toString();

        assertThat(caseData.getAppeal().getHearingOptions().getExcludeDates())
            .hasSize(1)
            .extracting(ExcludeDate::getValue)
            .extracting("start", "end")
            .containsExactlyInAnyOrder(Tuple.tuple(expected, expected));
    }

    @DisplayName("When case has a hearing and has existing excluded dates addCurrentHearingToExcludeDates adds the "
        + "hearing date to the list correctly without affecting the other excluded dates")
    @Test
    public void testAddCurrentHearingToExcludeDatesExistingDates() {
        String excludedExisting = EXCLUDED_DATE_TIME.toLocalDate().toString();
        List<ExcludeDate> excludeDate = new ArrayList<>(List.of(
            ExcludeDate.builder().value(DateRange.builder()
                .start(excludedExisting)
                .end(excludedExisting)
                .build()).build()));

        caseData.getAppeal().getHearingOptions().setExcludeDates(excludeDate);

        postponementRequestService.addCurrentHearingToExcludeDates(response);

        String expected = HEARING_DATE_TIME.toLocalDate().toString();

        assertThat(response.getData().getAppeal().getHearingOptions().getExcludeDates())
            .hasSize(2)
            .extracting(ExcludeDate::getValue)
            .extracting("start", "end")
            .containsExactlyInAnyOrder(
                Tuple.tuple(expected, expected),
                Tuple.tuple(excludedExisting, excludedExisting));
    }

    @DisplayName("When case has no hearing addCurrentHearingToExcludeDates adds the correct error message without "
        + "changing the excluded list")
    @Test
    public void testAddCurrentHearingToExcludeDatesNoHearing() {
        String excludedExisting = EXCLUDED_DATE_TIME.toLocalDate().toString();
        List<ExcludeDate> excludeDate =  new ArrayList<>(List.of(
            ExcludeDate.builder().value(DateRange.builder()
                .start(excludedExisting)
                .end(excludedExisting)
                .build()).build()));

        caseData.getAppeal().getHearingOptions().setExcludeDates(excludeDate);

        caseData.setHearings(null);

        postponementRequestService.addCurrentHearingToExcludeDates(response);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsExactlyInAnyOrder("There are no hearing to postpone");

        assertThat(response.getData().getAppeal().getHearingOptions().getExcludeDates())
            .hasSize(1)
            .extracting(ExcludeDate::getValue)
            .extracting("start", "end")
            .containsExactlyInAnyOrder(Tuple.tuple(excludedExisting, excludedExisting));
    }
}
