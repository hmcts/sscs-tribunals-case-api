package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DwpChallengeValidityHandlerTest {

    private final DwpChallengeValidityHandler handler = new DwpChallengeValidityHandler();

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(callback.getEvent()).thenReturn(EventType.DWP_CHALLENGE_VALIDITY);
    }

    @Test
    public void setField() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .interlocReviewState("awaitingInformation")
            .build();

        SscsCaseData actualSscsCaseData = handler.setField(sscsCaseData, "reviewByJudge",
            EventType.DWP_CHALLENGE_VALIDITY);

        assertEquals("reviewByJudge", actualSscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters({
        "DWP_CHALLENGE_VALIDITY, true", "TCW_DIRECTION_ISSUED, false", "INTERLOC_INFORMATION_RECEIVED, false"
    })
    public void givenEvent_thenCanHandle(EventType eventType, boolean expected) {
        when(callback.getEvent()).thenReturn(eventType);
        assertEquals(expected, handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }
}