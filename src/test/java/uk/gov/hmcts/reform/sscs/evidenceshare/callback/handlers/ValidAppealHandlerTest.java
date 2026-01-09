package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority.LATEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.featureflag.FeatureFlag.SSCS_CHILD_MAINTENANCE_FT;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FeatureToggleService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class ValidAppealHandlerTest {

    private static final String CHILD_SUPPORT = "childSupport";
    private static final long CCD_CASE_ID = 1234567890L;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;
    @Mock
    private IdamService idamService;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private FeatureToggleService featureToggleService;

    @InjectMocks
    private ValidAppealHandler handler;

    @Test
    void canHandle_shouldReturnTrue_forSubmittedValidAppealChildSupport() {
        when(featureToggleService.isNotEnabled(SSCS_CHILD_MAINTENANCE_FT)).thenReturn(false);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(VALID_APPEAL);
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(CHILD_SUPPORT));

        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @ParameterizedTest(name = "benefit={0}, event={1}, callbackType={2} => cannot handle")
    @MethodSource("unsupportedScenarios")
    void canHandle_shouldReturnFalse_forUnsupportedScenarios(String benefitCode, EventType eventType, CallbackType callbackType) {
        when(featureToggleService.isNotEnabled(SSCS_CHILD_MAINTENANCE_FT)).thenReturn(false);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        if (callbackType == SUBMITTED) {
            when(callback.getEvent()).thenReturn(eventType);
        }
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefitCode));

        assertThat(handler.canHandle(callbackType, callback)).isFalse();
    }

    @Test
    void getPriority_shouldBeLatest() {
        assertThat(handler.getPriority()).isEqualTo(LATEST);
    }

    @Test
    void handle_shouldTriggerRequestOtherPartyDataEvent_forSupportedScenario() {
        var caseData = SscsCaseData.builder()
            .ccdCaseId(String.valueOf(CCD_CASE_ID))
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(CHILD_SUPPORT).build()).build())
            .build();

        var tokens = IdamTokens.builder().userId("user-id").email("test@example.com").build();

        when(featureToggleService.isNotEnabled(SSCS_CHILD_MAINTENANCE_FT)).thenReturn(false);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(VALID_APPEAL);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(idamService.getIdamTokens()).thenReturn(tokens);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(CCD_CASE_ID), eq("requestOtherPartyData"),
            eq("REQUEST_OTHER_PARTY_DATA"), eq("Requesting other party data"), eq(tokens), any());
    }

    @ParameterizedTest(name = "unsupported: benefit={0}, event={1}, callbackType={2} => does not update CCD")
    @MethodSource("unsupportedScenarios")
    void handle_shouldNotUpdateCcd_forUnsupportedScenarios(String benefitCode, EventType eventType, CallbackType callbackType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(featureToggleService.isNotEnabled(SSCS_CHILD_MAINTENANCE_FT)).thenReturn(false);
        if (eventType != VALID_APPEAL) {
            when(callback.getEvent()).thenReturn(eventType);
        }
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefitCode));

        handler.handle(callbackType, callback);

        verify(updateCcdCaseService, never()).updateCaseV2(any(), any(), any(), any(), any(), any());
    }

    @Test
    void handle_shouldNotUpdateCcd_whenToggledOff() {
        when(featureToggleService.isNotEnabled(SSCS_CHILD_MAINTENANCE_FT)).thenReturn(true);

        handler.canHandle(SUBMITTED, callback);

        verify(updateCcdCaseService, never()).updateCaseV2(any(), any(), any(), any(), any(), any());
    }

    private static Stream<Arguments> unsupportedScenarios() {
        return Stream.of(Arguments.of("PIP", VALID_APPEAL, SUBMITTED),
            Arguments.of(CHILD_SUPPORT, APPEAL_RECEIVED, SUBMITTED),
            Arguments.of(CHILD_SUPPORT, VALID_APPEAL, MID_EVENT));
    }

    private static SscsCaseData caseDataWithBenefit(String benefitCode) {
        return SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build())
            .build();
    }
}
