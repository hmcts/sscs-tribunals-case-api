package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPostHearingReviewDocumentType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;

class SscsUtilTest {
    public static final String UNEXPECTED_POST_HEARING_REVIEW_TYPE_AND_ACTION = "getting the document type has an unexpected postHearingReviewType and action";
    private PostHearing postHearing;

    @BeforeEach
    void setUp() {
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
    void givenActionTypeSetAsideRefusedSelected_shouldReturnSetAsideRefusedDocument() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.REFUSE);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.SET_ASIDE_REFUSED);
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
}
