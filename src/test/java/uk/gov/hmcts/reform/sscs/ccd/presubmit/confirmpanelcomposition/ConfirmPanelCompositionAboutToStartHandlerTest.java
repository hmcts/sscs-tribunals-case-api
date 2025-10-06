package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.readytolist.ReadyToListAboutToSubmitHandler.EXISTING_HEARING_WARNING;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.hmc.reference.HmcStatus;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.CaseHearing;
import uk.gov.hmcts.reform.sscs.model.multi.hearing.HearingsGetResponse;
import uk.gov.hmcts.reform.sscs.service.HmcHearingApiService;

@ExtendWith(MockitoExtension.class)
public class ConfirmPanelCompositionAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private ConfirmPanelCompositionAboutToStartHandler handler;

    @Mock
    private HmcHearingApiService hmcHearingApiService;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), CONFIRM_PANEL_COMPOSITION, false);
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
    void throwsExceptionIfANonConfirmPanelCompositionEvent() {
        callback = new Callback<>(caseDetails, empty(), EventType.CREATE_BUNDLE, false);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    @DisplayName("Throw an Error if existing hearing in Listed state")
    void throwsErrorIfHearingInListedState() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        given(hmcHearingApiService.getHearingsRequest(anyString(), eq(HmcStatus.LISTED)))
                .willReturn(HearingsGetResponse.builder().caseHearings(List.of(CaseHearing.builder().hearingId(1L).build())).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains(EXISTING_HEARING_WARNING));
    }

    @Test
    @DisplayName("Handle successfully if no existing hearing in Listed state")
    void givenNoHearingInListedState_thenHandle() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        given(hmcHearingApiService.getHearingsRequest(anyString(), eq(HmcStatus.LISTED)))
                .willReturn(HearingsGetResponse.builder().caseHearings(List.of()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }
}
