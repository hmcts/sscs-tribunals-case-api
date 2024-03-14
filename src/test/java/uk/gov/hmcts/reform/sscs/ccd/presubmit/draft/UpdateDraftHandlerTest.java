package uk.gov.hmcts.reform.sscs.ccd.presubmit.draft;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateDraftHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final Long CCD_CASE_ID = 1234567890L;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private UpdateDraftHandler updateDraftHandler;

    @BeforeEach
    void setUp() {
        updateDraftHandler = new UpdateDraftHandler();
    }

    @Test
    public void givenValidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_DRAFT);

        assertTrue(updateDraftHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_CASE_ONLY);

        assertFalse(updateDraftHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAnInvalidEvent_handleThrowsIllegalStateException() {
        when(callback.getEvent()).thenReturn(EventType.UPDATE_CASE_ONLY);

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            updateDraftHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        });

        String expectedMessage = "Cannot handle callback";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void givenValidEvent_handleDoesNothing() {
        SscsCaseData caseData = CaseDataUtils.buildCaseData();
        caseData.setCcdCaseId(CCD_CASE_ID.toString());

        when(callback.getEvent()).thenReturn(EventType.UPDATE_DRAFT);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        String expected = caseData.toString();

        PreSubmitCallbackResponse<SscsCaseData> response = updateDraftHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String actual = response.getData().toString();
        assertEquals(expected, actual);
    }
}
