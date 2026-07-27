package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIDENTIALITY_CONFIRMED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SENT_TO_DWP;

import java.time.LocalDate;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
class ConfidentialityConfirmedHandlerTest {

    private ConfidentialityConfirmedHandler handler;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private SscsCaseDetails sscsCaseDetails;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> sscsCaseDataCaptor;

    @BeforeEach
    void setUp() {
        handler = new ConfidentialityConfirmedHandler(updateCcdCaseService, idamService);
    }

    @Test
    void givenNonSubmittedEvent_thenReturnFalse() {
        assertThat(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, mode = EnumSource.Mode.INCLUDE, names = {"CASE_UPDATED"})
    void givenNonConfidentialityConfirmedEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, mode = EnumSource.Mode.EXCLUDE, names = {"CHILD_SUPPORT", "UC"})
    void givenNonSupportedBenefit_thenReturnFalse(Benefit benefit) {
        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefit.getShortName()));

        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "UC"})
    void givenConfidentialityConfirmedEventAndSupportedBenefit_thenReturnTrue(Benefit benefit) {
        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseDataWithBenefit(benefit.getShortName()));

        assertThat(handler.canHandle(SUBMITTED, callback)).isTrue();
    }

    @Test
    void shouldCallUpdateCaseWithCorrectEvent() {
        SscsCaseData sscsCaseData = caseDataWithBenefit(UC.getShortName());
        when(callback.getEvent()).thenReturn(CONFIDENTIALITY_CONFIRMED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getId()).thenReturn(1234L);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(updateCcdCaseService.updateCaseV2(eq(1234L), eq(SENT_TO_DWP.getCcdType()),
                eq("Case sent to FTA"), eq("Case sent to FTA"), eq(idamTokens), any()))
                .thenReturn(sscsCaseDetails);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(1234L), eq(SENT_TO_DWP.getCcdType()),
                eq("Case sent to FTA"), eq("Case sent to FTA"), eq(idamTokens), sscsCaseDataCaptor.capture());

        Consumer<SscsCaseDetails> consumer = sscsCaseDataCaptor.getValue();
        SscsCaseData dataToUpdate = SscsCaseData.builder().build();
        when(sscsCaseDetails.getData()).thenReturn(dataToUpdate);

        consumer.accept(sscsCaseDetails);

        assertThat(dataToUpdate.getDateSentToDwp()).isEqualTo(LocalDate.now().toString());
        assertThat(dataToUpdate.getHmctsDwpState()).isEqualTo("sentToDwp");
    }

    @Test
    void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.ADD_OTHER_PARTY_DATA);

        assertThatThrownBy(() -> handler.handle(SUBMITTED, callback))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldReturnCorrectPriority() {
        assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
    }

    private SscsCaseData caseDataWithBenefit(String benefitCode) {
        return SscsCaseData
            .builder()
            .ccdCaseId("1234")
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build())
            .build();
    }
}
