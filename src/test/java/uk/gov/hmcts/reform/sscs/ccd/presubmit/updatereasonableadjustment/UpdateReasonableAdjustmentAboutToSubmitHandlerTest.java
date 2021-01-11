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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

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
                .appeal(Appeal.builder().appellant(
                        Appellant.builder().isAppointee(NO.getValue()).wantsReasonableAdjustment(YES).reasonableAdjustmentRequirements(RED_FONT)
                                .appointee(Appointee.builder().build()).build())
                        .rep(Representative.builder().hasRepresentative(YES.getValue()).wantsReasonableAdjustment(NO).reasonableAdjustmentRequirements(RED_FONT).build())
                        .build()
                )
                .jointParty(YES.getValue())
                .jointPartyWantsReasonableAdjustment(NO)
                .jointPartyReasonableAdjustmentRequirements(RED_FONT)
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonUpdateReasonableAdjustmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAppellantUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getAppeal().getRep().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getRep().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getJointPartyWantsReasonableAdjustment());
        assertNull(response.getData().getJointPartyReasonableAdjustmentRequirements());
        assertEquals(YES, response.getData().getAppeal().getAppellant().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getAppeal().getAppellant().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenAppointeeUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().isAppointee(YES.getValue()).appointee(
                Appointee.builder().wantsReasonableAdjustment(YES).reasonableAdjustmentRequirements(RED_FONT).build()
        ).build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getAppeal().getRep().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getRep().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getAppeal().getAppellant().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getJointPartyWantsReasonableAdjustment());
        assertNull(response.getData().getJointPartyReasonableAdjustmentRequirements());
        assertEquals(YES, response.getData().getAppeal().getAppellant().getAppointee().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getAppeal().getAppellant().getAppointee().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenRepresentativeUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().isAppointee(YES.getValue()).appointee(
                Appointee.builder().wantsReasonableAdjustment(NO).reasonableAdjustmentRequirements(RED_FONT).build()

        ).build());
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative(YES.getValue())
                .wantsReasonableAdjustment(YES).reasonableAdjustmentRequirements(RED_FONT).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getAppeal().getAppellant().getAppointee().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getAppointee().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getJointPartyWantsReasonableAdjustment());
        assertNull(response.getData().getJointPartyReasonableAdjustmentRequirements());
        assertEquals(YES, response.getData().getAppeal().getRep().getWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getAppeal().getRep().getReasonableAdjustmentRequirements());
    }

    @Test
    public void givenJointPartyUpdateReasonableAdjustment_thenClearAlternativeLetterFieldsSetToNo() {
        sscsCaseData.getAppeal().setAppellant(Appellant.builder().isAppointee(YES.getValue()).appointee(
                Appointee.builder().wantsReasonableAdjustment(NO).reasonableAdjustmentRequirements(RED_FONT).build()

        ).build());
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative(YES.getValue())
                .wantsReasonableAdjustment(NO).reasonableAdjustmentRequirements(RED_FONT).build());
        sscsCaseData.setJointParty(YES.getValue());
        sscsCaseData.setJointPartyReasonableAdjustmentRequirements(RED_FONT);
        sscsCaseData.setJointPartyWantsReasonableAdjustment(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getUpdateReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getAppeal().getAppellant().getAppointee().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getAppellant().getAppointee().getReasonableAdjustmentRequirements());
        assertNull(response.getData().getAppeal().getRep().getWantsReasonableAdjustment());
        assertNull(response.getData().getAppeal().getRep().getReasonableAdjustmentRequirements());
        assertEquals(YES, response.getData().getJointPartyWantsReasonableAdjustment());
        assertEquals(RED_FONT, response.getData().getJointPartyReasonableAdjustmentRequirements());
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
