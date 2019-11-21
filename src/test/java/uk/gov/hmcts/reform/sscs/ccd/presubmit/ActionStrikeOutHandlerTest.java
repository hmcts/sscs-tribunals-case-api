package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class ActionStrikeOutHandlerTest {

    private ActionStrikeOutHandler actionStrikeOutHandler;

    private SscsCaseData sscsCaseData;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        actionStrikeOutHandler = new ActionStrikeOutHandler();

        sscsCaseData = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }


    @Test
    @Parameters({
        "ACTION_STRIKE_OUT, ABOUT_TO_SUBMIT, true",
    })
    public void givenEvent_thenCanHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        when(callback.getEvent()).thenReturn(eventType);
        assertEquals(expected, actionStrikeOutHandler.canHandle(callbackType, callback));
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_shouldThrowException() {
        actionStrikeOutHandler.canHandle(ABOUT_TO_SUBMIT, null);
    }

    @Test
    public void setField() {
    }
}