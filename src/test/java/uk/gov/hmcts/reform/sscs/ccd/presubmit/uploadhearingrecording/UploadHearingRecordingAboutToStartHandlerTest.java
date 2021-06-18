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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;

@RunWith(JUnitParamsRunner.class)
public class UploadHearingRecordingAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private final UserDetails
        userDetails =
        UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();
    private UploadHearingRecordingAboutToStartHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private IdamService idamService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UploadHearingRecordingAboutToStartHandler(idamService);
        sscsCaseData = SscsCaseData.builder().appeal(
            Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_HEARING_RECORDING);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
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
                .hearingDate("2022-03-20")
                .time("15:15").build()).build()));
        assertNoHearingsInThePastError();
    }

    @Ignore
    @Test
    public void givenHearingsListHasPastDate_ReturnList() {
        List<Hearing> hearingList = new ArrayList<>();
        hearingList.add(Hearing.builder().value(
            HearingDetails.builder()
                .venue(Venue.builder().name("some value").build())
                .hearingDate("2022-03-20")
                .time("15:15").build()).build());
        hearingList.add(Hearing.builder().value(
            HearingDetails.builder()
                .hearingId("1111")
                .venue(Venue.builder().name("good value").build())
                .hearingDate("2021-06-06")
                .time("09:00").build()).build());
        hearingList.add(Hearing.builder().value(
            HearingDetails.builder()
                .venue(Venue.builder().name("some value").build())
                .hearingDate("2022-05-20")
                .time("10:10").build()).build());
        sscsCaseData.setHearings(unmodifiableList(hearingList));
        final PreSubmitCallbackResponse<SscsCaseData>
            response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        List<DynamicListItem> selectHearingDetails = response.getData().getHearingRecordingsData().getSelectHearingDetails().getListItems();
        assertEquals(1, selectHearingDetails.size());
        assertEquals("1111", selectHearingDetails.get(0).getCode());
        assertEquals("good value 09:00 06 Jun 2021", selectHearingDetails.get(0).getLabel());
    }

    private void assertNoHearingsInThePastError() {
        final PreSubmitCallbackResponse<SscsCaseData>
            response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("No hearing has been conducted on this case", response.getErrors().toArray()[0]);
    }

}
