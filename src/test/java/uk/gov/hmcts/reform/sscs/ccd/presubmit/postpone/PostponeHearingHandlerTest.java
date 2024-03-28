package uk.gov.hmcts.reform.sscs.ccd.presubmit.postpone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Postponement;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;


@RunWith(JUnitParamsRunner.class)
public class PostponeHearingHandlerTest {
    private static final String CASE_ID = "1234";
    private static final String USER_AUTHORISATION = "Bearer token";


    private IdamTokens idamTokens;
    private SscsCaseData caseData;

    private AutoCloseable closeable;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @InjectMocks
    private PostponeHearingHandler handler;

    @Before
    public void setUp() {
        closeable = openMocks(this);

        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        caseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .dwpState(DwpState.FE_ACTIONED_NR)
            .appeal(Appeal.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.LIST_ASSIST).build())
            .postponement(Postponement.builder()
                .postponementEvent(READY_TO_LIST)
                .unprocessedPostponement(YES)
                .build())
            .build();

        when(callback.getEvent()).thenReturn(EventType.POSTPONED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @After
    public void releaseMocks() throws Exception {
        closeable.close();
    }

    @DisplayName("When given an valid event and ScheduleListing Feature is Enabled canHandle returns true")
    @Test
    @Parameters({"POSTPONED"})
    public void canHandleValidEvents(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @DisplayName("When given an invalid event canHandle returns false")
    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void canHandleInvalidEvents(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @DisplayName("When given an valid event canHandle returns false")
    @Test
    public void canHandleScheduleListingDisabled() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @DisplayName("When given Ready To List event is given and postponement hearing has not been already handled "
        + "handle sets the fields correctly and sends the correct ccd update event")
    @Test
    public void handleReadyToListPostponementRelistEventType() {
        caseData.getPostponement().setPostponementEvent(READY_TO_LIST);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        SscsCaseData result = response.getData();

        assertThat(result.getPostponement()).isNotNull();
        assertThat(result.getPostponement().getPostponementEvent()).isNull();
        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(NO);
        assertThat(result.getDwpState()).isEqualTo(DwpState.HEARING_POSTPONED);

        verify(ccdService,times(1))
            .updateCase(any(SscsCaseData.class),
                eq(Long.valueOf(CASE_ID)),
                eq(READY_TO_LIST.getCcdType()),
                eq("Ready to List after Hearing Postponed"),
                eq("Setting case to Ready to List after Hearing Postponed"),
                eq(idamTokens));
    }

    @DisplayName("When given Not Listable event is given and postponement hearing has not been already handled "
        + "handle sets the fields correctly and sends the correct ccd update event")
    @Test
    public void handleNotListablePostponementRelistEventType() {
        caseData.getPostponement().setPostponementEvent(NOT_LISTABLE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        SscsCaseData result = response.getData();

        assertThat(result.getPostponement()).isNotNull();
        assertThat(result.getPostponement().getPostponementEvent()).isNull();
        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(NO);
        assertThat(result.getDwpState()).isEqualTo(DwpState.HEARING_POSTPONED);

        verify(ccdService,times(1))
            .updateCase(any(SscsCaseData.class),
                eq(Long.valueOf(CASE_ID)),
                eq(NOT_LISTABLE.getCcdType()),
                eq("Not Listable after Hearing Postponed"),
                eq("Setting case to Not Listable after Hearing Postponed"),
                eq(idamTokens));
    }

    @DisplayName("When a case is not List Assist the method handled does not update any fields or call update ccd "
        + "but returns the correct error")
    @Test
    public void handleInvalidPostponementRelistEventType() {
        caseData.getPostponement().setPostponementEvent(APPEAL_RECEIVED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .contains("Invalid event type: APPEAL_RECEIVED for Hearing Postponed Event");

        verifyNoInteractions(ccdService);

        SscsCaseData result = response.getData();

        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(YES);
        assertThat(result.getDwpState()).isEqualTo(DwpState.FE_ACTIONED_NR);
    }

    @DisplayName("When case hearing postponed has been already handled the method handled does not update any fields "
        + "or call update ccd but returns the correct error")
    @Test
    public void handleAlreadyHandled() {
        caseData.getPostponement().setUnprocessedPostponement(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .contains("Cannot process hearing postponed event on a case that has already been processed");


        verifyNoInteractions(ccdService);

        SscsCaseData result = response.getData();

        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(NO);
        assertThat(result.getDwpState()).isEqualTo(DwpState.FE_ACTIONED_NR);
    }


    @DisplayName("When a case is not List Assist the method handled does not update any fields or call update ccd but "
        + "returns the correct error")
    @Test
    public void handleNonListAssistCase() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(HearingRoute.GAPS).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .contains("Cannot process hearing postponed on non Scheduling & Listing Case");

        verifyNoInteractions(ccdService);

        SscsCaseData result = response.getData();

        assertThat(result.getPostponement().getUnprocessedPostponement()).isEqualTo(YES);
        assertThat(result.getDwpState()).isEqualTo(DwpState.FE_ACTIONED_NR);
    }
}
