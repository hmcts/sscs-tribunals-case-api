package uk.gov.hmcts.reform.sscs.ccd.presubmit.sethearingroute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class SetHearingRouteAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private static final String ERROR_MESSAGE = "The hearing route must be set to List assist on an IBC case";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SetHearingRouteAboutToSubmitHandler handler;

    @Before
    public void setUp() throws Exception {
        openMocks(this);
        handler = new SetHearingRouteAboutToSubmitHandler();
    }

    @Test
    public void shouldReturnTrueForCanHandle() {
        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .build();

        when(callback.getEvent()).thenReturn(EventType.SET_HEARING_ROUTE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void shouldReturnFalseWhenCaseIsNotIbcForCanHandle() {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode("053")
                .build();

        when(callback.getEvent()).thenReturn(EventType.SET_HEARING_ROUTE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({
        "EVENTS_UPDATES",
        "APPEAL_RECEIVED",
        "CASE_UPDATED",
        "SET_ASIDE_GRANTED"
    })
    public void shouldReturnFalseForCanHandleWhenNotSetHearingEvent(EventType eventType) {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();

        when(callback.getEvent()).thenReturn(eventType);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({
        "ABOUT_TO_START",
        "MID_EVENT",
        "SUBMITTED",
    })
    public void shouldReturnFalseForCanHandleWhenNotAboutToSubmit(CallbackType callbackType) {
        SscsCaseData caseData = SscsCaseData.builder()
                .benefitCode(IBCA_BENEFIT_CODE)
                .build();

        when(callback.getEvent()).thenReturn(EventType.SET_HEARING_ROUTE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void shouldReturnErrorWhenHearingRouteIsGaps() {
        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.GAPS).build())
            .build();

        when(callback.getEvent()).thenReturn(EventType.SET_HEARING_ROUTE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(ERROR_MESSAGE));
    }

    @Test
    public void shouldNotReturnErrorWhenHearingRouteIsNotGaps() {
        SscsCaseData caseData = SscsCaseData.builder()
            .benefitCode(IBCA_BENEFIT_CODE)
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.LIST_ASSIST).build())
            .build();

        when(callback.getEvent()).thenReturn(EventType.SET_HEARING_ROUTE);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertTrue(response.getErrors().isEmpty());
    }
}
