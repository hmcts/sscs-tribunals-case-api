package uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import com.networknt.schema.ValidationMessage;
import java.util.HashSet;
import java.util.Set;
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

@RunWith(JUnitParamsRunner.class)
public class ResendToGapsAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private RoboticsJsonValidator jsonValidator;

    @Mock
    private RoboticsJsonMapper jsonMapper;

    @Mock
    private ResendToGapsMessageHandler messageHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ValidationMessage validationMessage;

    private SscsCaseData sscsCaseData;

    private ResendToGapsAboutToSubmitHandler handler;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new ResendToGapsAboutToSubmitHandler(jsonMapper, jsonValidator, messageHandler);
        when(callback.getEvent()).thenReturn(EventType.RESEND_CASE_TO_GAPS2);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .hmctsDwpState("failedRobotics")
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(jsonMapper.map(any())).thenReturn(new JSONObject());

        when(validationMessage.getMessage()).thenReturn("Invalid field");
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
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("sentToRobotics", response.getData().getHmctsDwpState());
        verify(jsonValidator, atLeastOnce()).validate(any(), any());
    }

    @Test
    @Parameters({"required,$.apellant,nino,apellant.nino is missing/not populated - please correct.",
            "required,,nino,nino is missing/not populated - please correct.",
            "minLength,$.apellant.nino,,apellant.nino is missing/not populated - please correct.",
            "pattern,$.apellant.nino,,apellant.nino is invalid - please correct.",
            "whoknows,$.apellant.nino,,An unexpected error has occurred. Please raise a ServiceNow ticket - the following field has caused the issue: apellant.nino"
    })
    public void givenInvalidJsonReturnErrors(String errorType, String path, String field, String expectedError) {

        when(validationMessage.getType()).thenReturn(errorType);
        when(validationMessage.getPath()).thenReturn(path);
        when(validationMessage.getArguments()).thenReturn(new String[]{field});

        Set<String> errorSet = new HashSet<>();
        errorSet.add(expectedError);
        when(jsonValidator.validate(any(), any())).thenReturn(errorSet);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        String firstError = response.getErrors().iterator().next();
        assertTrue(firstError.contains(expectedError));

        assertEquals("failedRobotics", response.getData().getHmctsDwpState());
        verify(jsonValidator, atLeastOnce()).validate(any(), any());
    }

    @Test
    public void jsonMapperFailsReturnErrors() {
        doThrow(new NullPointerException("Use an option dude")).when(jsonMapper).map(any());
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("failedRobotics", response.getData().getHmctsDwpState());
        assertTrue(response.getErrors().iterator().next().contains("Json Mapper unable to build robotics json due to missing fields"));
        verify(jsonMapper, atLeastOnce()).map(any());
    }
}
