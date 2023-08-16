package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.CORRECTION_GRANTED;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_CORRECTED_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.FINAL_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getIssueFinalDecisionDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPostHearingReviewDocumentType;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getWriteFinalDecisionDocumentType;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
class SscsUtilTest {
    public static final String UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION = "getting the document type has an unexpected postHearingReviewType and action";
    private PostHearing postHearing;
    private SscsCaseData caseData;

    @BeforeEach
    void setUp() {
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

    @ParameterizedTest
    @CsvSource(value = {
        "GRANT,SET_ASIDE_GRANTED",
        "REFUSE,SET_ASIDE_REFUSED"
    })
    void givenActionTypeSetAside_shouldReturnSetAsideDocument(SetAsideActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
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
    @CsvSource(value = {
        "GRANT,LIBERTY_TO_APPLY_GRANTED",
        "REFUSE,LIBERTY_TO_APPLY_REFUSED"
    })
    void givenActionTypeLta_shouldReturnLtaDocument(LibertyToApplyActions action, DocumentType expectedDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        postHearing.getLibertyToApply().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(expectedDocumentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"PERMISSION_TO_APPEAL"})
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
  
    @Test
    void givenPostHearingsEnabledFalse_clearPostHearingsFieldClearsDocumentFields_butDoesNotAlterPostHearing() {
        postHearing.setRequestType(PostHearingRequestType.SET_ASIDE);
        SscsCaseData caseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(caseData, false);

        assertThat(caseData.getPostHearing().getRequestType()).isNotNull();
        assertThat(caseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(caseData.getDocumentStaging().getDateAdded()).isNull();
    }

    @Test
    void givenPostHearingsEnabledTrue_clearPostHearingsFieldClearsDocumentFields_andClearsPostHearing() {
        postHearing.setRequestType(PostHearingRequestType.SET_ASIDE);
        SscsCaseData caseData = SscsCaseData.builder()
            .postHearing(postHearing)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YesNo.YES)
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now())
                .build())
            .build();

        SscsUtil.clearPostHearingFields(caseData, true);

        assertThat(caseData.getPostHearing().getRequestType()).isNull();
        assertThat(caseData.getDocumentGeneration().getGenerateNotice()).isNull();
        assertThat(caseData.getDocumentStaging().getDateAdded()).isNull();
    }
}
