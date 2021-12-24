package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
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

    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateOtherPartyAboutToSubmitHandler();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        Appeal appeal = Appeal.builder()
                .benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build();
        sscsCaseData.setAppeal(appeal);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenANonUpdateOtherPartyEvent_thenReturnFalse() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("2"), buildOtherParty("1")));
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnUpdateOtherPartyEvent_thenReturnTrue() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("2"), buildOtherParty("1")));
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAUpdateOtherPartyEventWithNullOtherPartiesList_thenReturnFalse() {
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonAboutToSubmitEvent_thenReturnFalse(CallbackType callbackType) {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenOtherPartiesUcbIsYes_thenUpdateCaseDataOtherPartyUcb() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("2"), buildOtherParty("1")));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getOtherPartyUcb(), is(YesNo.YES.getValue()));
    }

    @Test
    public void givenNewOtherPartyAdded_thenAssignAnId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getData().getOtherParties().size());
        assertEquals("1", response.getData().getOtherParties().get(0).getValue().getId());
        assertEquals("2", response.getData().getOtherParties().get(0).getValue().getAppointee().getId());
        assertEquals("3", response.getData().getOtherParties().get(0).getValue().getRep().getId());
        assertTrue(YesNo.isYes(response.getData().getOtherParties().get(0).getValue().getSendNewOtherPartyNotification()));
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAssignedNextId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("2"), buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("3", response.getData().getOtherParties().get(2).getValue().getId());
        assertEquals("4", response.getData().getOtherParties().get(2).getValue().getAppointee().getId());
        assertEquals("5", response.getData().getOtherParties().get(2).getValue().getRep().getId());
        assertFalse(YesNo.isYes(response.getData().getOtherParties().get(0).getValue().getSendNewOtherPartyNotification()));
        assertFalse(YesNo.isYes(response.getData().getOtherParties().get(1).getValue().getSendNewOtherPartyNotification()));
        assertTrue(YesNo.isYes(response.getData().getOtherParties().get(2).getValue().getSendNewOtherPartyNotification()));
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNextId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherParty("2"), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherPartyWithAppointeeAndRep(null, null, null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("5", response.getData().getOtherParties().get(2).getValue().getId());
        assertEquals("6", response.getData().getOtherParties().get(2).getValue().getAppointee().getId());
        assertEquals("7", response.getData().getOtherParties().get(2).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNextId() {
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithAppointeeAndRep("2", null, null), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherParty(null)));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(3, response.getData().getOtherParties().size());
        assertEquals("5", response.getData().getOtherParties().get(0).getValue().getAppointee().getId());
        assertEquals("6", response.getData().getOtherParties().get(0).getValue().getRep().getId());
        assertEquals("7", response.getData().getOtherParties().get(2).getValue().getId());
    }

    private CcdValue<OtherParty> buildConfidentialOtherParty(String id, YesNo confidentialityRequired) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .confidentialityRequired(confidentialityRequired)
                        .build())
                .build();
    }

    @Test
    public void givenOtherPartyWantsConfidentiality_thenCaseIsConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty("2", YES), buildConfidentialOtherParty("1", NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getIsConfidentialCase());
    }

    @Test
    public void givenNoOtherPartyWantsConfidentiality_thenCaseIsNotConfidential() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build()).build())
                .otherParties(Arrays.asList(buildConfidentialOtherParty("2", NO), buildConfidentialOtherParty("1", NO))).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(null, response.getData().getIsConfidentialCase());
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
