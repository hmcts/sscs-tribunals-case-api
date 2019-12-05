package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(MockitoJUnitRunner.class)
public class FeHandledOfflineHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Test
    public void givenEventIsTriggered_thenCanHandle() {
        FeHandledOfflineHandler feHandledOfflineHandler = new FeHandledOfflineHandler();
        given(callback.getEvent()).willReturn(EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE);

        boolean actual = feHandledOfflineHandler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback);

        assertThat(actual, is(true));
    }

    @Test
    public void handle() {
    }
}