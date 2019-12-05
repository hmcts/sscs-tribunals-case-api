package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
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

    @Test
    @Parameters({
        "FURTHER_EVIDENCE_HANDLED_OFFLINE, ABOUT_TO_SUBMIT, true"
    })
    public void givenEventIsTriggered_thenCanHandle(EventType eventType, CallbackType callbackType,
                                                    boolean expectation) {
        FeHandledOfflineHandler feHandledOfflineHandler = new FeHandledOfflineHandler();
        given(callback.getEvent()).willReturn(eventType);

        boolean actual = feHandledOfflineHandler.canHandle(callbackType, callback);

        assertThat(actual, is(expectation));
    }

    @Test
    public void handle() {
    }
}