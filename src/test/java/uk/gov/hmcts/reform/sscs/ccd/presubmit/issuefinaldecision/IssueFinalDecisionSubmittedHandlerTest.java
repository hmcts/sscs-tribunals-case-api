package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Optional;
import java.util.function.Consumer;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.service.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
public class IssueFinalDecisionSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private CcdCallbackMapService ccdCallbackMapService;
    @Mock
    private EventPublisher eventPublisher;
    @Captor
    private ArgumentCaptor<Consumer<SscsCaseData>> consumerArgumentCaptor;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();

    @Before
    public void setUp() {
        openMocks(this);

        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, eventPublisher, true);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.HEARING);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    public void givenANonSubmittedEvent_thenReturnFalse() {
        assertThatThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void givenPostHearingsFlagIsFalse_thenOnlyCallCallback() {
        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, eventPublisher, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(eventPublisher, times(1)).publishEvent(callback);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void givenCcdCallbackMapV2EnabledAndCorrectionInProgress_thenHandleCallbackMap() {
        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, eventPublisher, true);
        sscsCaseData.getPostHearing().getCorrection().setIsCorrectionFinalDecisionInProgress(YES);

        when(ccdCallbackMapService.handleCcdCallbackMapV2(eq(CorrectionActions.GRANT), anyLong(), any()))
                .thenReturn(SscsCaseData.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(eventPublisher, never())
                .publishEvent(callback);
        verify(ccdCallbackMapService)
                .handleCcdCallbackMapV2(eq(CorrectionActions.GRANT), anyLong(), consumerArgumentCaptor.capture());
        consumerArgumentCaptor.getValue().accept(sscsCaseData);
        assertThat(sscsCaseData.getPostHearing().getCorrection().getIsCorrectionFinalDecisionInProgress())
                .isEqualTo(NO);
        assertThat(response.getErrors())
                .isEmpty();
    }
}
