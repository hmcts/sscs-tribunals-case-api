package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@RunWith(JUnitParamsRunner.class)
public class UploadHearingRecordingAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String futureDate =
        LocalDate.now().plusMonths(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    private final UserDetails
            userDetails =
            UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private UploadHearingRecordingAboutToStartHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadHearingRecordingAboutToStartHandler();
        sscsCaseData = SscsCaseData.builder().appeal(
                Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        ReflectionTestUtils.setField(handler, "hearingRecordingFilterEnabled", true);
    }

    @Test
    public void givenAValidHandleAndEventType_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenNonUploadHearingRecordingEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenUploadHearingRecordingEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenHearingsListIsNull_ReturnError() {
        sscsCaseData.setHearings(null);
        assertNoHearingsInThePastError();
    }

    @Test
    public void givenHearingsListIsEmpty_ReturnError() {
        sscsCaseData.setHearings(Collections.EMPTY_LIST);
        assertNoHearingsInThePastError();
    }

    @Test
    public void givenHearingsListHasFutureDate_ReturnError() {
        sscsCaseData.setHearings(singletonList(Hearing.builder().value(
                HearingDetails.builder()
                        .hearingDate(futureDate)
                        .time("15:15").build()).build()));
        assertNoHearingsInThePastError();
    }

    @Test
    public void givenHearingsListHasPastDate_ReturnList() {
        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(Hearing.builder().value(
            HearingDetails.builder()
                .venue(Venue.builder().name("some value").build())
                .hearingDate(futureDate)
                .time("15:15:00").build()).build());
        hearingList.add(Hearing.builder().value(
                HearingDetails.builder()
                        .hearingId("1111")
                        .venue(Venue.builder().name("good value").build())
                        .hearingDate("2021-06-06")
                        .time("09:00").build()).build());
        hearingList.add(Hearing.builder().value(
            HearingDetails.builder()
                .venue(Venue.builder().name("some value").build())
                .hearingDate(futureDate)
                .time("10:10").build()).build());
        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData>
                response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        List<DynamicListItem> selectHearingDetails =
            response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems();
        assertEquals(1, selectHearingDetails.size());
        assertEquals("1111", selectHearingDetails.get(0).getCode());
        assertEquals("good value 09:00:00 06 Jun 2021", selectHearingDetails.get(0).getLabel());
    }

    @Test
    @Parameters({"null", "", " ", "  "})
    public void givenHearingsListHasPastDate_WithInvalidTime_ReturnList(@Nullable String time) {
        List<Hearing> hearingList = new ArrayList<>();

        HearingDetails hearingDetails = HearingDetails.builder()
                .hearingId("1")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time(time).build();

        hearingList.add(Hearing.builder().value(hearingDetails).build());

        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        List<DynamicListItem> selectHearingDetails = response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems();
        assertEquals(hearingList.size(), selectHearingDetails.size());
        assertTrue(selectHearingDetails.stream().allMatch(o -> "venue name 06 Jun 2021".equals(o.getLabel())));
    }

    @Test
    @Parameters({"null", "", " ", "  "})
    public void givenHearingsIsOnToday_WithInvalidTime_ReturnList(@Nullable String time) {
        List<Hearing> hearingList = new ArrayList<>();

        LocalDate now = LocalDate.now();
        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingId("1")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate(now.toString())
                .time(time).build();
        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingId("2")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate(now.toString())
                .time(time).build();

        hearingList.add(Hearing.builder().value(hearingDetails1).build());
        hearingList.add(Hearing.builder().value(hearingDetails2).build());

        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        List<DynamicListItem> selectHearingDetails = response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems();
        assertEquals(hearingList.size(), selectHearingDetails.size());
        String label = "venue name " + now.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        assertTrue(selectHearingDetails.stream().allMatch(o -> label.equals(o.getLabel())));
    }

    @Test
    public void givenHearingsListHasPastDate_WithTimeFormat_ReturnList() {
        List<Hearing> hearingList = new ArrayList<>();

        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingId("1")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00").build();
        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingId("2")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00:00").build();
        HearingDetails hearingDetails3 = HearingDetails.builder()
                .hearingId("3")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00:00.0").build();
        HearingDetails hearingDetails4 = HearingDetails.builder()
                .hearingId("4")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00:00.00").build();
        HearingDetails hearingDetails5 = HearingDetails.builder()
                .hearingId("5")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00:00.000").build();

        hearingList.add(Hearing.builder().value(hearingDetails1).build());
        hearingList.add(Hearing.builder().value(hearingDetails2).build());
        hearingList.add(Hearing.builder().value(hearingDetails3).build());
        hearingList.add(Hearing.builder().value(hearingDetails4).build());
        hearingList.add(Hearing.builder().value(hearingDetails5).build());

        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());

        List<DynamicListItem> selectHearingDetails = response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems();
        assertEquals(hearingList.size(), selectHearingDetails.size());
        assertTrue(selectHearingDetails.stream().allMatch(o -> "venue name 09:00:00 06 Jun 2021".equals(o.getLabel())));
    }

    @Test
    public void givenHearingHasNoDate_FilterFromList() {
        List<Hearing> hearingList = new ArrayList<>();

        HearingDetails hearingDetails1 = HearingDetails.builder()
                .hearingId("1")
                .venue(Venue.builder().name("venue name").build())
                .build();
        HearingDetails hearingDetails2 = HearingDetails.builder()
                .hearingId("2")
                .venue(Venue.builder().name("venue name").build())
                .hearingDate("2021-06-06")
                .time("09:00:00").build();
        hearingList.add(Hearing.builder().value(hearingDetails1).build());
        hearingList.add(Hearing.builder().value(hearingDetails2).build());
        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        List<DynamicListItem> selectHearingDetails = response.getData().getSscsHearingRecordingCaseData().getSelectHearingDetails().getListItems();
        assertEquals(0, response.getErrors().size());
        assertEquals(0, selectHearingDetails.stream().filter(x -> x.getCode().equals("1")).toList().size());

    }

    private void assertNoHearingsInThePastError() {
        final PreSubmitCallbackResponse<SscsCaseData>
                response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("No hearing has been conducted on this case", response.getErrors().toArray()[0]);
    }

}
