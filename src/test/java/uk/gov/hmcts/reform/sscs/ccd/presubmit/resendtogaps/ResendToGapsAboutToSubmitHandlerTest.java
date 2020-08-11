package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonValidator;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsValidationException;

@RunWith(JUnitParamsRunner.class)
public class ResendToGapsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ResendToGapsAboutToSubmitHandler handler;

    @Mock RoboticsJsonValidator jsonValidator;

    @Mock RoboticsJsonMapper jsonMapper;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    private JSONObject jsonObj = new JSONObject();

    @Before
    public void setUp() {
        initMocks(this);

        handler = new ResendToGapsAboutToSubmitHandler(jsonMapper, jsonValidator);
        when(callback.getEvent()).thenReturn(EventType.RESEND_CASE_TO_GAPS2);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        when(jsonMapper.map(any())).thenReturn(jsonObj);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED"})
    public void givenANonHandledEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"RESEND_CASE_TO_GAPS2"})
    public void givenAHandledEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenValidJsonReturnNoErrors() {

        ResendToGapsAboutToSubmitHandler resendHandler = new ResendToGapsAboutToSubmitHandler(jsonMapper, jsonValidator);

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = resendHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        verify(jsonValidator, atLeastOnce()).validate(any());
    }

    @Test
    public void givenInvalidJsonReturnErrors() {
        ResendToGapsAboutToSubmitHandler resendHandler = new ResendToGapsAboutToSubmitHandler(jsonMapper, jsonValidator);

        doThrow(new RoboticsValidationException(new Throwable("Effed it"))).when(jsonValidator).validate(any());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = resendHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().iterator().next().contains("Effed it"));
        verify(jsonValidator, atLeastOnce()).validate(any());
    }
}
