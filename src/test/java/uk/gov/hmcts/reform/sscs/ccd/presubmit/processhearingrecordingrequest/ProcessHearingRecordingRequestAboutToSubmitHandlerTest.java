package uk.gov.hmcts.reform.sscs.ccd.presubmit.processhearingrecordingrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;


@RunWith(JUnitParamsRunner.class)
public class ProcessHearingRecordingRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ProcessHearingRecordingRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ProcessHearingRecordingRequestAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAGrantedDwpHearingRecording_thenAddToReleasedListAndDwpStatusReleased() {
        SscsHearingRecording recording1 = SscsHearingRecording.builder().value(SscsHearingRecordingDetails.builder()
                .hearingId("an_id1").build()).build();
        HearingRecordingRequest request1 = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails
                .builder().sscsHearingRecordingList(Arrays.asList(recording1))
                .requestingParty(PartyItemList.DWP.getCode()).build()).build();
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(Arrays.asList(request1));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsHearingRecordingCaseData sscsHearingRecordingCaseDataResponse = response.getData()
                .getSscsHearingRecordingCaseData();
        assertThat("Check DwpReleasedHearings is populated",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings(), is(not(empty())));
        assertThat("Check RequestedHearings has been reduced",
                sscsHearingRecordingCaseDataResponse.getRequestedHearings(), is(empty()));
        assertThat("Check DwpReleasedHearings has the correct Hearing",
                sscsHearingRecordingCaseDataResponse.getDwpReleasedHearings().get(0).getValue()
                        .getSscsHearingRecordingList().get(0).getValue().getHearingId(), is("an_id1"));
        assertThat("Check DwpState is RELEASED", sscsCaseData.getDwpState(), is(DwpState.HEARING_RECORDING_RELEASED));
    }
}