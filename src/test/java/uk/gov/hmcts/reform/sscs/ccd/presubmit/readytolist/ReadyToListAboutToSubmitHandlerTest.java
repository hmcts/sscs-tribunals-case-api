package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.HashMap;
import java.util.Map;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.RegionalProcessingCenterService;

@RunWith(JUnitParamsRunner.class)
public class ReadyToListAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private RegionalProcessingCenterService regionalProcessingCenterService;

    private ReadyToListAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ReadyToListAboutToSubmitHandler(false, regionalProcessingCenterService);
        when(callback.getEvent()).thenReturn(EventType.READY_TO_LIST);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1234")
            .createdInGapsFrom(State.READY_TO_LIST.getId())
            .appeal(Appeal.builder().build())
            .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }


    @Test
    public void returnAnErrorIfCreatedInGapsFromIsAtValidAppeal() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        sscsCaseData = sscsCaseData.toBuilder().region("TEST").createdInGapsFrom(State.VALID_APPEAL.getId()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Case already created in GAPS at valid appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    public void returnAnErrorIfCreatedInGapsFromIsNull() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        sscsCaseData = sscsCaseData.toBuilder().region("TEST").createdInGapsFrom(null).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("Case already created in GAPS at valid appeal.", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenHearingCreated_withListAssist_thenCheckIfHearingType_isListAssist() {
        buildRegionalProcessingCentreMap(HearingRoute.LIST_ASSIST);
        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService);

        sscsCaseData = sscsCaseData.toBuilder().region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(regionalProcessingCenterService.getHearingRoute(caseDetails.getCaseData().getRegion())).thenReturn(HearingRoute.LIST_ASSIST);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.LIST_ASSIST, sscsCaseData.getHearingRoute());
        assertEquals(HearingRoute.LIST_ASSIST, response.getData().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, sscsCaseData.getHearingState());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getHearingState());
    }

    @Test
    public void givenHearingCreated_withListAssist_thenCheckIfHearingType_isGaps() {
        buildRegionalProcessingCentreMap(HearingRoute.GAPS);
        handler = new ReadyToListAboutToSubmitHandler(true, regionalProcessingCenterService);

        sscsCaseData = sscsCaseData.toBuilder().region("TEST").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(regionalProcessingCenterService.getHearingRoute(caseDetails.getCaseData().getRegion())).thenReturn(HearingRoute.GAPS);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(HearingRoute.GAPS, sscsCaseData.getHearingRoute());
        assertEquals(HearingRoute.GAPS, response.getData().getHearingRoute());
        assertEquals(HearingState.CREATE_HEARING, sscsCaseData.getHearingState());
        assertEquals(HearingState.CREATE_HEARING, response.getData().getHearingState());
    }

    private void buildRegionalProcessingCentreMap(HearingRoute route) {
        Map<String, RegionalProcessingCenter> rpcMap = new HashMap<>();
        rpcMap.put("SSCS TEST", RegionalProcessingCenter.builder().hearingRoute(route)
            .name("TEST")
            .build());
        when(regionalProcessingCenterService.getRegionalProcessingCenterMap()).thenReturn(rpcMap);
    }

    @Test
    public void respondWithNoErrorsIfCreatedFromGapsIsAtReadyToList() {
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
