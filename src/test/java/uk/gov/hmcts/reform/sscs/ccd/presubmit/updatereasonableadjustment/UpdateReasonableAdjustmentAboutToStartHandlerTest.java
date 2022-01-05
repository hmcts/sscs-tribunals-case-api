package uk.gov.hmcts.reform.sscs.ccd.presubmit.updatereasonableadjustment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
public class UpdateReasonableAdjustmentAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateReasonableAdjustmentAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateReasonableAdjustmentAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_REASONABLE_ADJUSTMENT);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
    }

    @Test
    public void givenANonUpdateReasonableAdjustmentEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenACaseWithOtherParty_thenSetShowOtherPartyDetailsFieldToYes() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().name(Name.builder().build()).build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(YES, response.getData().getShowOtherPartyDetails());
    }

    @Test
    public void givenACaseWithNoOtherParty_thenDoNotSetShowOtherPartyDetailsFieldToYes() {
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowOtherPartyDetails());
    }

    @Test
    public void givenACaseWithOtherPartyNull_thenDoNotSetShowOtherPartyDetailsFieldToYes() {
        sscsCaseData.setOtherParties(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertNull(response.getData().getShowOtherPartyDetails());
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
