package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Arrays;
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



@RunWith(JUnitParamsRunner.class)
public class UpdateOtherPartyAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateOtherPartyAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateOtherPartyAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUpdateOtherPartyEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAUpdateOtherPartyEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAUpdateOtherPartyEventWithNullOtherPartiesList_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails().getCaseData().getOtherParties()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonAboutToSubmitEvent_thenReturnFalse(CallbackType callbackType) {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenOtherPartiesUcbIsYes_thenUpdateCasedataOtherPartyUcb() {
        when(sscsCaseData.getOtherParties()).thenReturn(Arrays.asList(buildOtherParty("2"), buildOtherParty("1")));

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        verify(sscsCaseData).setOtherPartyUcb(YesNo.YES.getValue());
    }

    @Test
    public void givenNewOtherPartyAdded_thenAssignAnId() {
        when(sscsCaseData.getOtherParties()).thenReturn(Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getData().getOtherParties().size());
        assertEquals("1", response.getData().getOtherParties().get(0).getValue().getId());
        assertEquals("2", response.getData().getOtherParties().get(0).getValue().getAppointee().getId());
        assertEquals("3", response.getData().getOtherParties().get(0).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAssignedNextId() {
        when(sscsCaseData.getOtherParties()).thenReturn(Arrays.asList(buildOtherParty("2"), buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("3", response.getData().getOtherParties().get(2).getValue().getId());
        assertEquals("4", response.getData().getOtherParties().get(2).getValue().getAppointee().getId());
        assertEquals("5", response.getData().getOtherParties().get(2).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNextId() {
        when(sscsCaseData.getOtherParties()).thenReturn(Arrays.asList(buildOtherParty("2"), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("5", response.getData().getOtherParties().get(2).getValue().getId());
        assertEquals("6", response.getData().getOtherParties().get(2).getValue().getAppointee().getId());
        assertEquals("7", response.getData().getOtherParties().get(2).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNextId() {
        when(sscsCaseData.getOtherParties()).thenReturn(Arrays.asList(buildOtherPartyWithAppointeeAndRep("2", null, null), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherParty(null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("5", response.getData().getOtherParties().get(0).getValue().getAppointee().getId());
        assertEquals("6", response.getData().getOtherParties().get(0).getValue().getRep().getId());
        assertEquals("7", response.getData().getOtherParties().get(2).getValue().getId());
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .unacceptableCustomerBehaviour(YesNo.YES)
                        .build())
                .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }


}
