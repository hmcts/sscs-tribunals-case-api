package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpRequestTimeExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpRequestTimeExtensionAboutToSubmitHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Test
    @Parameters({
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false",
        "DWP_REQUEST_TIME_EXTENSION, ABOUT_TO_SUBMIT, true"
    })
    public void canHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        when(callback.getEvent()).thenReturn(eventType);

        DwpRequestTimeExtensionAboutToSubmitHandler handler = new DwpRequestTimeExtensionAboutToSubmitHandler();

        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expected, actualResult);
    }

    @Test
    public void handle() {
    }
}