package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatelistingrequirements;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_LISTING_REQUIREMENTS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingState.UPDATE_HEARING;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.updatelistingrequirements.UpdateListingRequirementsRequestSubmittedHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OverrideFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

@ExtendWith(MockitoExtension.class)
public class UpdateListingRequirementsRequestSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private ListAssistHearingMessageHelper listAssistHearingMessageHelper;
    @InjectMocks
    private UpdateListingRequirementsRequestSubmittedHandler handler;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build()).dwpIsOfficerAttending("Yes").build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), UPDATE_LISTING_REQUIREMENTS, false);
    }

    @Test
    void givenValidCallback_thenReturnTrue() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, empty(), READY_TO_LIST, false);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void handleUpdateListingRequirementsSendSuccessfulWithOverrideFields() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData()).isNotNull();
        SscsCaseData caseData = response.getData();
        assertThat(UPDATE_HEARING).isEqualTo(caseData.getSchedulingAndListingFields().getHearingState());
        verify(listAssistHearingMessageHelper, times(1)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }

    @Test
    void handleUpdateListingRequirementsSendSuccessfulWithPanelComposition() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").panelCompositionMemberMedical1("58").build());
        sscsCaseData.setCcdCaseId("1234");

        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
                .appeal(Appeal.builder().build())
                .ccdCaseId("1234")
                .dwpIsOfficerAttending("Yes")
                .panelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build())
                .build();

        Optional<CaseDetails<SscsCaseData>> caseDetailsBefore =
                Optional.of(new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseDataBefore, now(), "Benefit"));

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(true);

        callback = new Callback<>(caseDetails, caseDetailsBefore, UPDATE_LISTING_REQUIREMENTS, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getData()).isNotNull();
        SscsCaseData caseData = response.getData();
        assertThat(UPDATE_HEARING).isEqualTo(caseData.getSchedulingAndListingFields().getHearingState());
        verify(listAssistHearingMessageHelper, times(1)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }

    @Test
    void handleUpdateListingRequirementsSendUnsuccessful() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.getSchedulingAndListingFields().setOverrideFields(OverrideFields.builder().build());
        sscsCaseData.setCcdCaseId("1234");

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(1).isEqualTo(response.getErrors().size());
        assertThat(response.getErrors()).contains("An error occurred during message publish. Please try again.");
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void handleUpdateListingRequirementsWrongState() {
        sscsCaseData = CaseDataUtils.buildCaseData();

        caseDetails =
                new CaseDetails<>(1234L, "SSCS", State.UNKNOWN, sscsCaseData, now(), "Benefit");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void handleUpdateListingRequirementsShouldNotSendMessageWhenNoPanelCompositionOrOverrideFields() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.setCcdCaseId("1234");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(0)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }

    @Test
    void whenCaseDetailsBeforePanelCompositionIsNullThenDoNotThrowErrorOrSendMessage() {
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
                .appeal(Appeal.builder().build())
                .ccdCaseId("1234")
                .dwpIsOfficerAttending("Yes")
                .panelMemberComposition(null)
                .build();

        sscsCaseData.setCcdCaseId("1234");
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);

        Optional<CaseDetails<SscsCaseData>> caseDetailsBefore =
                Optional.of(new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseDataBefore, now(), "Benefit"));

        callback = new Callback<>(caseDetails, caseDetailsBefore, UPDATE_LISTING_REQUIREMENTS, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(0)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }

    @Test
    void whenCaseDetailsBeforePanelCompositionIsNullAndCaseDetailsPanelCompositionIsNotNullThenSendMessage() {
        sscsCaseData.setCcdCaseId("1234");
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(LIST_ASSIST);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());

        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder()
                .appeal(Appeal.builder().build())
                .ccdCaseId("1234")
                .dwpIsOfficerAttending("Yes")
                .panelMemberComposition(null)
                .build();

        Optional<CaseDetails<SscsCaseData>> caseDetailsBefore =
                Optional.of(new CaseDetails<>(1234L, "SSCS", State.READY_TO_LIST, sscsCaseDataBefore, now(), "Benefit"));

        callback = new Callback<>(caseDetails, caseDetailsBefore, UPDATE_LISTING_REQUIREMENTS, false);

        given(listAssistHearingMessageHelper.sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null)))
                .willReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
                SUBMITTED,
                callback,
                USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        verify(listAssistHearingMessageHelper, times(1)).sendHearingMessage(
                anyString(), any(HearingRoute.class), any(HearingState.class), eq(null));
    }
}
