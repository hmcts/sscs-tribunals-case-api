package uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willAnswer;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.service.HearingsService.EXISTING_HEARING_ERROR;
import static uk.gov.hmcts.reform.sscs.service.HearingsService.REQUEST_FAILURE_WARNING;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.readytolist.ReadyToListAboutToStartHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.HearingsService;

@ExtendWith(MockitoExtension.class)
public class ReadyToListAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private HearingsService hearingsService;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private ReadyToListAboutToStartHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", RESPONSE_RECEIVED, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), EventType.READY_TO_LIST, false);
        handler = new ReadyToListAboutToStartHandler(hearingsService);
    }

    @Test
    @DisplayName("Return true if about to start event is valid")
    void givenAValidAboutToStartEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    @DisplayName("Return false if callback type is invalid")
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    @DisplayName("Throw exception if cannot handle")
    void throwsExceptionIfANonReadyToListEvent() {
        callback = new Callback<>(caseDetails, empty(), EventType.CREATE_BUNDLE, false);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    @DisplayName("Throw an Error if existing hearing in Listed state")
    void throwsErrorIfHearingInListedState() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        willAnswer(invocation -> {
            PreSubmitCallbackResponse<SscsCaseData> resp = invocation.getArgument(1);
            resp.addError(EXISTING_HEARING_ERROR);
            return null;
        }).given(hearingsService).validationCheckForListedOrExceptionHearings(any(), any());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(EXISTING_HEARING_ERROR));
    }

    @Test
    @DisplayName("Give warning if existing hearing in exception state")
    void giveWarningIfHearingInExceptionState() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        willAnswer(invocation -> {
            PreSubmitCallbackResponse<SscsCaseData> resp = invocation.getArgument(1);
            resp.addWarning(REQUEST_FAILURE_WARNING);
            return null;
        }).given(hearingsService).validationCheckForListedOrExceptionHearings(any(), any());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().contains(REQUEST_FAILURE_WARNING));
    }

    @Test
    @DisplayName("Handle successfully if no existing hearing in Listed state")
    void givenNoHearingInListedState_thenHandle() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }
}
