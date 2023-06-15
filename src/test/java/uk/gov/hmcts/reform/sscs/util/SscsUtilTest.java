package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correction;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

class SscsUtilTest {
    public static final String UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION = "getting the document type has an unexpected postHearingReviewType and action";
    private PostHearing postHearing;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
        openMocks(this);
        postHearing = PostHearing.builder()
            .correction(Correction.builder()
                .correctionFinalDecisionInProgress(YesNo.NO)
                .build())
            .build();

        caseData = new SscsCaseData();
        caseData.setPostHearing(postHearing);
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
    void givenPostHearingsFlagIsTrueAndCorrectionNotInProgress_shouldReturnDraftDecisionNotice() {
        assertThat(getWriteFinalDecisionDocumentType(caseData, true)).isEqualTo(DRAFT_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionInProgress_shouldReturnDraftCorrectedDecisionNotice() {
        postHearing.getCorrection().setCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getWriteFinalDecisionDocumentType(caseData, true)).isEqualTo(DRAFT_CORRECTED_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndCorrectionInProgress_shouldReturnDraftDecisionNotice() {
        postHearing.getCorrection().setCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getWriteFinalDecisionDocumentType(caseData, false)).isEqualTo(DRAFT_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionInProgress_shouldReturnCorrectionGranted() {
        postHearing.getCorrection().setCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getIssueFinalDecisionDocumentType(caseData, true)).isEqualTo(CORRECTION_GRANTED);
    }

    @Test
    void givenPostHearingsFlagIsTrueAndCorrectionNotInProgress_shouldReturnFinalDecisionNotice() {
        assertThat(getIssueFinalDecisionDocumentType(caseData, true)).isEqualTo(FINAL_DECISION_NOTICE);
    }

    @Test
    void givenPostHearingsFlagIsFalseAndCorrectionInProgress_shouldReturnFinalDecisionNotice() {
        postHearing.getCorrection().setCorrectionFinalDecisionInProgress(YesNo.YES);
        assertThat(getIssueFinalDecisionDocumentType(caseData, false)).isEqualTo(FINAL_DECISION_NOTICE);
    }
}
