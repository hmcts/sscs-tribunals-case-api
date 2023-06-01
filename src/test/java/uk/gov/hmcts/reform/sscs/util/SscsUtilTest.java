package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_CORRECTED_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getIssueFinalDecisionDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPostHearingReviewDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getWriteFinalDecisionDocumentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;

class SscsUtilTest {
    public static final String UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION = "getting the document type has an unexpected postHearingReviewType and action";
    private PostHearing postHearing;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @BeforeEach
    void setUp() {
        openMocks(this);
        postHearing = new PostHearing();
    }

    @Test
    void givenPostHearingsEnabledFalse_returnDecisionNotice() {
        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, false);
        assertThat(documentType).isEqualTo(DocumentType.DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsReviewTypeIsNull_returnDecisionNotice() {
        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);
        assertThat(documentType).isEqualTo(DocumentType.DECISION_NOTICE);
    }

    @Test
    void givenActionTypeSetAsideGrantedSelected_shouldThrowError() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.GRANT);

        assertThatThrownBy(() -> getPostHearingReviewDocumentType(postHearing, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("getting the document type has an unexpected postHearingReviewType and action");
    }

    @Test
    void givenActionTypeSetAsideRefusedSelected_shouldReturnSetAsideRefusedDocument() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.REFUSE);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.SET_ASIDE_REFUSED);
    }

    @Test
    void givenActionTypeCorrectionGrantedSelected_shouldThrowError() {
        postHearing.setReviewType(PostHearingReviewType.CORRECTION);
        postHearing.getCorrection().setAction(CorrectionActions.GRANT);

        assertThatThrownBy(() -> getPostHearingReviewDocumentType(postHearing, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("getting the document type has an unexpected postHearingReviewType and action");
    }

    @Test
    void givenActionTypeCorrectionRefusedSelected_shouldReturnCorrectionRefusedDocument() {
        postHearing.setReviewType(PostHearingReviewType.CORRECTION);
        postHearing.getCorrection().setAction(CorrectionActions.REFUSE);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.CORRECTION_REFUSED);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,STATEMENT_OF_REASONS_GRANTED",
        "REFUSE,STATEMENT_OF_REASONS_REFUSED"
    })
    void givenActionTypeSor_shouldReturnSorDocument(StatementOfReasonsActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        postHearing.getStatementOfReasons().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"PERMISSION_TO_APPEAL", "LIBERTY_TO_APPLY"})
    void givenActionTypeNotSupported_throwError(PostHearingReviewType postHearingReviewType) {
        postHearing.setReviewType(postHearingReviewType);

        assertThatThrownBy(() -> getPostHearingReviewDocumentType(postHearing, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndStateIsPostHearing_shouldReturnDraftCorrectionGranted() {
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        assertThat(getWriteFinalDecisionDocumentType(caseDetails, true)).isEqualTo(DRAFT_CORRECTED_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndStateIsDormant_shouldReturnDraftCorrectionGranted() {
        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        assertThat(getWriteFinalDecisionDocumentType(caseDetails, true)).isEqualTo(DRAFT_CORRECTED_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndStateIsPostHearings_shouldReturnDraftDecisionNotice() {
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        assertThat(getWriteFinalDecisionDocumentType(caseDetails, false)).isEqualTo(DRAFT_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndDraftCorrectionNoticeHasBeenGenerated_shouldReturnCorrectionGranted() {
        assertThat(getIssueFinalDecisionDocumentType(DRAFT_CORRECTED_NOTICE.getLabel(), true)).isEqualTo(CORRECTION_GRANTED);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionNoticeHasBeenGenerated_shouldReturnCorrectionGranted() {
        assertThat(getIssueFinalDecisionDocumentType(CORRECTION_GRANTED.getLabel(), true)).isEqualTo(CORRECTION_GRANTED);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndDecisionNoticeHasBeenGenerated_shouldReturnFinalDecisionNotice() {
        assertThat(getIssueFinalDecisionDocumentType(FINAL_DECISION_NOTICE.getLabel(), true)).isEqualTo(FINAL_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndCorrectionNoticeHasBeenGenerated_shouldReturnFinalDecisionNotice() {
        assertThat(getIssueFinalDecisionDocumentType("Corrected Final Decision Notice", false)).isEqualTo(FINAL_DECISION_NOTICE);
    }
}
