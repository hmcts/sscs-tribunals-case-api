package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpdirectionresponse;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class DwpDirectionResponseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private Callback<SscsCaseData> callback;
    private DwpDirectionResponseAboutToSubmitHandler handler;

    @Before
    public void setup() {
        callback = mock(Callback.class);
        handler = new DwpDirectionResponseAboutToSubmitHandler();
    }

    @Test
    public void canHandleAboutToSubmitDwpDirectionResponseEvent() {
        when(callback.getEvent()).thenReturn(EventType.DWP_DIRECTION_RESPONSE);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback), is(true));
    }

    @Test
    public void cannotHandleAboutToStartDwpDirectionResponseEvent() {
        when(callback.getEvent()).thenReturn(EventType.DWP_DIRECTION_RESPONSE);
        assertThat(handler.canHandle(ABOUT_TO_START, callback), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void cannotHandleNullCallback() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, null), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void cannotHandleNullCallbackType() {
        assertThat(handler.canHandle(null, callback), is(false));
    }

    @Test
    public void setsDwpStateWhenHandlingDwpDirectionResponseEvent() {
        when(callback.getEvent()).thenReturn(EventType.DWP_DIRECTION_RESPONSE);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .dwpState(DwpState.DIRECTION_ACTION_REQUIRED)
                .build();
        when(callback.getCaseDetails()).thenReturn(new CaseDetails(1, "Benefit", State.VOID_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_RESPONDED));
    }

    @Test
    public void doesNotSetDwpStateWhenHandlingDwpDirectionResponseEventButNotInDwpDirectResponseDate() {
        when(callback.getEvent()).thenReturn(EventType.DWP_DIRECTION_RESPONSE);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .dwpState(DwpState.FE_ACTIONED_NA)
                .build();
        when(callback.getCaseDetails()).thenReturn(new CaseDetails(1, "Benefit", State.VOID_STATE, sscsCaseData, LocalDateTime.now(), "Benefit"));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(DwpState.FE_ACTIONED_NA));
    }
}
