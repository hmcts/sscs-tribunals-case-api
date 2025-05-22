package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CONFIRM_PANEL_COMPOSITION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@ExtendWith(MockitoExtension.class)
public class ConfirmPanelCompositionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String FQPM = PanelMemberType.TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED.toRef();
    private static final String TRIBUNAL_MEDICAL_MEMBER_REF = PanelMemberType.TRIBUNAL_MEMBER_MEDICAL.toRef();
    private static final String REGIONAL_MEDICAL_MEMBER_REF = PanelMemberType.REGIONAL_MEDICAL_MEMBER.toRef();
    private static final String IBCA_BENEFIT_CODE = Benefit.INFECTED_BLOOD_COMPENSATION.getBenefitCode();

    @InjectMocks
    private ConfirmPanelCompositionAboutToSubmitHandler handler;

    private SscsCaseData sscsCaseData;
    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(handler, "isDefaultPanelCompEnabled", true);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();
        caseDetails = new CaseDetails<>(
            1234L,
            "SSCS",
            State.READY_TO_LIST,
            sscsCaseData,
            now(),
            "Benefit");

        callback = new Callback<>(caseDetails, empty(), CONFIRM_PANEL_COMPOSITION, false);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, empty(), EventType.CASE_UPDATED, false);
        assertThatIllegalStateException().isThrownBy(() -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenFqpmRequiredNull_thenNoChange() {
        sscsCaseData.setIsFqpmRequired(null);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = {"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndInterlocByJudge_thenClearInterloc(YesNo isFqpmRequired) {
        sscsCaseData.setIsFqpmRequired(isFqpmRequired);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = {"YES", "NO"})
    public void givenFqpmRequiredYesOrNoAndNoInterlocByJudge_thenInterlocNotChanged(YesNo isFqpmRequired) {
        sscsCaseData.setIsFqpmRequired(isFqpmRequired);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_TCW);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @Test
    public void givenNoFqpmRequiredSet_thenInterlocNotChanged() {
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(response.getData().getInterlocReferralReason()).isEqualTo(InterlocReferralReason.NONE);
    }

    @Test
    public void givenFqpmRequiredSet_thenUpdateFqpmInPanelMemberComposition() {
        sscsCaseData.setIsFqpmRequired(YES);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember())
            .containsOnlyOnce(FQPM);
    }

    @Test
    public void givenFqpmRequiredNo_thenPanelMemberCompositionHasNoFqpm() {
        sscsCaseData.setIsFqpmRequired(NO);
        sscsCaseData.setPanelMemberComposition(PanelMemberComposition.builder()
            .panelCompositionDisabilityAndFqMember(new ArrayList<>(
                List.of(PanelMemberType.TRIBUNAL_MEMBER_FINANCIALLY_QUALIFIED.getReference())))
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionDisabilityAndFqMember())
            .doesNotContain(FQPM);
    }

    @Test
    public void givenMedicalMemberRequiredNoOnIbcaCase_thenPanelMemberCompositionMedicalMembersAreNull() {
        sscsCaseData.setBenefitCode(IBCA_BENEFIT_CODE);
        sscsCaseData.setIsMedicalMemberRequired(NO);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1()).isNull();
        assertThat(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical2()).isNull();
    }

    @Test
    public void givenMedicalMemberRequiredYesOnIbcaCase_thenPanelMemberCompositionHasMedicalMember() {
        sscsCaseData.setBenefitCode(IBCA_BENEFIT_CODE);
        sscsCaseData.setIsMedicalMemberRequired(YES);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        List<String> medicalMembers = new ArrayList<>();
        medicalMembers.add(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical1());
        medicalMembers.add(response.getData().getPanelMemberComposition().getPanelCompositionMemberMedical2());

        assertThat(medicalMembers).containsAnyOf(TRIBUNAL_MEDICAL_MEMBER_REF, REGIONAL_MEDICAL_MEMBER_REF);
    }

}
