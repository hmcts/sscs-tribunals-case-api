package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.confirmpanelcomposition.ConfirmPanelCompositionAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
public class ConfirmPanelCompositionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String FQPM = PanelMemberType.TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED.toRef();
    private static final String IBCA_BENEFIT_CODE = Benefit.INFECTED_BLOOD_COMPENSATION.getBenefitCode();

    @InjectMocks
    private ConfirmPanelCompositionAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), CONFIRM_PANEL_COMPOSITION, false);
    }

    @Test
    @DisplayName("Return true if about to submit event is valid")
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    @DisplayName("Return false if callback type is invalid")
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    @DisplayName("Throw exception if cannot handle")
    void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, empty(), EventType.CASE_UPDATED, false);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    @DisplayName("No interlock change if FQPM required is null")
    void givenFqpmRequiredNull_thenNoChange() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @ParameterizedTest
    @DisplayName("Clear interlock if FQPM Required is set and interlock is reviewed by judge")
    @EnumSource(value = YesNo.class, names = {"YES", "NO"})
    void givenFqpmRequiredYesOrNoAndInterlocByJudge_thenClearInterloc(YesNo isFqpmRequired) {
        sscsCaseData.setIsFqpmRequired(isFqpmRequired);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isNull();
    }

    @ParameterizedTest
    @DisplayName("Interlock shouldn't change if FQPM Required is set but interlock is not reviewed by judge")
    @EnumSource(value = YesNo.class, names = {"YES", "NO"})
    void givenFqpmRequiredYesOrNoAndNoInterlocByJudge_thenInterlocNotChanged(YesNo isFqpmRequired) {
        sscsCaseData.setIsFqpmRequired(isFqpmRequired);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_TCW);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @Test
    @DisplayName("No interlock change if FQPM required is not set")
    void givenNoFqpmRequiredSet_thenInterlocNotChanged() {
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @Test
    @DisplayName("If panel composition is null or empty, then no updates are made")
    void givenPanelCompositionEmpty_thenNoUpdatesAreMade() {
        sscsCaseData.setIsFqpmRequired(YES);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember());
    }

    @Test
    @DisplayName("If FQPM Required is set to yes, then panel member composition should contain FQPM")
    void givenFqpmRequiredSet_thenUpdateFqpmInPanelMemberComposition() {
        sscsCaseData.setIsFqpmRequired(YES);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember())
            .containsOnlyOnce(FQPM);
    }

    @Test
    @DisplayName("If FQPM Required is no, then panel member composition should not contain FQPM")
    void givenFqpmRequiredNo_thenPanelMemberCompositionHasNoFqpm() {
        sscsCaseData.setIsFqpmRequired(NO);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder()
            .panelCompositionDisabilityAndFqMember(new ArrayList<>(
                List.of(PanelMemberType.TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED.getReference())))
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember()).isNotNull();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember())
            .doesNotContain(FQPM);
    }

    @Test
    @DisplayName("If no Medical Member Required on an IBCA case, then Medical Members should be null on Panel Member Composition")
    void givenMedicalMemberRequiredNoOnIbcaCase_thenPanelMemberCompositionMedicalMembersAreNull() {
        sscsCaseData.setBenefitCode(IBCA_BENEFIT_CODE);
        sscsCaseData.setIsMedicalMemberRequired(NO);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder()
            .panelCompositionMemberMedical2("Member one")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1()).isNull();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical2()).isNull();
    }

    @Test
    @DisplayName("At least one Medical member should be tribunal or regional medical member on "
        + "panel member composition if Medical Member Required is yes on an IBCA case")
    void givenMedicalMemberRequiredYesOnIbcaCase_thenPanelMemberCompositionHasMedicalMember() {
        sscsCaseData.setBenefitCode(IBCA_BENEFIT_CODE);
        sscsCaseData.setIsMedicalMemberRequired(YES);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder().panelCompositionJudge("84").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().hasMedicalMember()).isTrue();
    }

    @Test
    @DisplayName("Medical members on panel member composition should not change if Medical Member Required "
        + "is set on a non IBCA case")
    void givenMedicalMemberRequiredYesOnNonIbcaCase_thenMedicalMemberOnPanelMemberCompositionIsUnchanged() {
        sscsCaseData.setBenefitCode(Benefit.PIP.getBenefitCode());
        sscsCaseData.setIsMedicalMemberRequired(YES);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder()
            .panelCompositionMemberMedical1(null)
            .panelCompositionMemberMedical2(null)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1()).isNull();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical2()).isNull();
    }

}
