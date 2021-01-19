package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatereasonableadjustment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

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
public class UpdateReasonableAdjustmentAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String RED_FONT = "red font";
    private UpdateReasonableAdjustmentAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateReasonableAdjustmentAboutToSubmitHandler();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_REASONABLE_ADJUSTMENT);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .updateReasonableAdjustment("alternativeLetterFormat")
                .reasonableAdjustments(ReasonableAdjustments.builder()
                        .appellant(ReasonableAdjustmentDetails.builder().reasonableAdjustmentRequirements(RED_FONT).wantsReasonableAdjustment(YES).build())
                        .appointee(ReasonableAdjustmentDetails.builder().reasonableAdjustmentRequirements(RED_FONT).wantsReasonableAdjustment(NO).build())
                        .representative(ReasonableAdjustmentDetails.builder().reasonableAdjustmentRequirements(RED_FONT).wantsReasonableAdjustment(NO).build())
                        .jointParty(ReasonableAdjustmentDetails.builder().reasonableAdjustmentRequirements(RED_FONT).wantsReasonableAdjustment(NO).build())
                        .build())
                .appeal(Appeal.builder().appellant(
                        Appellant.builder().isAppointee(NO.getValue())
                                .appointee(Appointee.builder().build()).build())
                        .rep(Representative.builder().hasRepresentative(YES.getValue()).build())
                        .build()
                )
                .jointParty(YES.getValue())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUpdateReasonableAdjustmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenNoUpdateReasonableAdjustment_thenClearReasonableAdjustmentField() {
        sscsCaseData.getReasonableAdjustments().getAppellant().setWantsReasonableAdjustment(NO);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getReasonableAdjustments());
    }

    @Test
    public void givenAppellantUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getReasonableAdjustments().getRepresentative());
        assertNull(response.getData().getReasonableAdjustments().getJointParty());
        assertNull(response.getData().getReasonableAdjustments().getAppointee());
        assertEquals(YES, response.getData().getReasonableAdjustments().getAppellant().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getReasonableAdjustments().getAppellant().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenAppointeeUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().isAppointee(YES.getValue()).appointee(
                Appointee.builder().build()
        ).build());
        sscsCaseData.getReasonableAdjustments().setAppointee(ReasonableAdjustmentDetails.builder().wantsReasonableAdjustment(YES).reasonableAdjustmentRequirements(RED_FONT).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getReasonableAdjustments().getRepresentative());
        assertNull(response.getData().getReasonableAdjustments().getJointParty());
        assertNull(response.getData().getReasonableAdjustments().getAppellant());
        assertEquals(YES, response.getData().getReasonableAdjustments().getAppointee().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getReasonableAdjustments().getAppointee().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenRepresentativeUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {

        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative(YES.getValue()).build());
        sscsCaseData.getReasonableAdjustments().getRepresentative().setWantsReasonableAdjustment(YES);
        sscsCaseData.getReasonableAdjustments().getAppellant().setWantsReasonableAdjustment(NO);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getReasonableAdjustments().getJointParty());
        assertNull(response.getData().getReasonableAdjustments().getAppellant());
        assertNull(response.getData().getReasonableAdjustments().getAppointee());
        assertEquals(YES, response.getData().getReasonableAdjustments().getRepresentative().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getReasonableAdjustments().getRepresentative().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenJointPartyUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().isAppointee(YES.getValue()).appointee(Appointee.builder().build()).build());
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative(YES.getValue()).build());
        sscsCaseData.setJointParty(YES.getValue());
        sscsCaseData.getReasonableAdjustments().getAppellant().setWantsReasonableAdjustment(NO);
        sscsCaseData.getReasonableAdjustments().getJointParty().setWantsReasonableAdjustment(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getReasonableAdjustments().getRepresentative());
        assertNull(response.getData().getReasonableAdjustments().getAppellant());
        assertNull(response.getData().getReasonableAdjustments().getAppointee());
        assertEquals(YES, response.getData().getReasonableAdjustments().getJointParty().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getReasonableAdjustments().getJointParty().getReasonableAdjustmentRequirements());
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
