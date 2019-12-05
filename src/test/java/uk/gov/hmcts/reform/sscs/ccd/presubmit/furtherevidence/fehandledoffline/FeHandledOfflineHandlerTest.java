package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class FeHandledOfflineHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;
    private final FeHandledOfflineHandler feHandledOfflineHandler = new FeHandledOfflineHandler();

    @Test
    @Parameters({
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, true",
        "APPEAL_RECEIVED, ABOUT_TO_SUBMIT, false",
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_START, false",
        "null, ABOUT_TO_SUBMIT, false",
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, null, false"
    })
    public void givenEventIsTriggered_thenCanHandle(@Nullable EventType eventType, @Nullable CallbackType callbackType,
                                                    boolean expectation) {
        given(callback.getEvent()).willReturn(eventType);

        boolean actual = feHandledOfflineHandler.canHandle(callbackType, callback);

        assertThat(actual, is(expectation));
    }

    @Test
    public void givenCallbackIsNull_shouldReturnFalse() {
        assertFalse(feHandledOfflineHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT, null));
    }

    @Test
    public void handle() {
    }
}