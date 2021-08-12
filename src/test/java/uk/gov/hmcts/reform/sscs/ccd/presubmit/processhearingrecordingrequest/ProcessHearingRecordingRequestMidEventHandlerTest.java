package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest.ProcessHearingRecordingRequestAboutToStartHandlerTest.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest.ProcessHearingRecordingRequestAboutToStartHandlerTest.recording;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.processhearingrecordingrequest.ProcessHearingRecordingRequestService;

@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private final ProcessHearingRecordingRequestMidEventHandler handler = new ProcessHearingRecordingRequestMidEventHandler(new ProcessHearingRecordingRequestService());

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private IdamService idamService;

    private SscsCaseData sscsCaseData;
    private final UserDetails userDetails = UserDetails.builder().roles(new ArrayList<>(asList("caseworker-sscs", UserRole.CTSC_CLERK.getValue()))).build();


    @Before
    public void setUp() {
        openMocks(this);

        sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .hearings(List.of(HEARING))

                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder()
                        .sscsHearingRecordings(List.of(recording(1), recording(2)))
                        .build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);
    }


    @Test
    @Parameters({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
