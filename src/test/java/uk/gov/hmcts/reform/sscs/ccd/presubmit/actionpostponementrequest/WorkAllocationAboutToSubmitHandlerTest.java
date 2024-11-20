package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

public class WorkAllocationAboutToSubmitHandlerTest {

    private static DateTimeFormatter  DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private PreSubmitCallbackHandler<SscsCaseData> handler;

    private SscsCaseData caseData;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        caseData = new SscsCaseData();
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @Test
    public void givenFeatureDisabled_thenReturnFalse() {
        handler = new WorkAllocationAboutToSubmitHandler(false);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        handler = new WorkAllocationAboutToSubmitHandler(true);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenOtherEvent_thenReturnFalse() {
        handler = new WorkAllocationAboutToSubmitHandler(true);
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenCaseWithNoHearings_thenDaysToHearingIsNull() {
        handler = new WorkAllocationAboutToSubmitHandler(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    @Test
    public void givenCaseWithHearingInThePaste_thenDaysToHearingIsNull() {
        caseData.setHearings(Arrays.asList(
            new Hearing(HearingDetails.builder().hearingDate(today(-7)).build())
        ));

        handler = new WorkAllocationAboutToSubmitHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    @Test
    public void givenCaseWithHearingWithNoDate_thenDaysToHearingIsNull() {
        caseData.setHearings(Arrays.asList(
                new Hearing(HearingDetails.builder().build())
        ));

        handler = new WorkAllocationAboutToSubmitHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    @Test
    public void givenCaseWithHearingWithUnparsableDate_thenDaysToHearingIsNull() {
        caseData.setHearings(Arrays.asList(
                new Hearing(HearingDetails.builder().hearingDate("NOT A DATE").build())
        ));

        handler = new WorkAllocationAboutToSubmitHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertNull(response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    @Test
    public void givenCaseWithMultipleHearings_then() {
        caseData.setHearings(Arrays.asList(
                new Hearing(HearingDetails.builder().hearingDate(today(-7)).build()),
                new Hearing(HearingDetails.builder().hearingDate(today(+7)).build()),
                new Hearing(HearingDetails.builder().hearingDate(today(+14)).build())
        ));

        handler = new WorkAllocationAboutToSubmitHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertEquals(Integer.valueOf(7), response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    @Test
    public void givenCaseWithMultipleHearingsInWrongOrder_then() {
        caseData.setHearings(Arrays.asList(
                new Hearing(HearingDetails.builder().hearingDate(today(+14)).build()),
                new Hearing(HearingDetails.builder().hearingDate(today(-7)).build()),
                new Hearing(HearingDetails.builder().hearingDate(today(+7)).build())
        ));

        handler = new WorkAllocationAboutToSubmitHandler(true);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, null);

        assertEquals(Integer.valueOf(7), response.getData().getWorkAllocationFields().getDaysToHearing());
    }

    private String today(int plusDays) {
        return LocalDate.now().plusDays(plusDays).format(DATE_FORMAT);
    }
}
