package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPostHearingReviewDocumentType;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SetAsideActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.StatementOfReasonsActions;

@RunWith(JUnitParamsRunner.class)
class SscsUtilTest {
    private PostHearing postHearing;

    @BeforeEach
    void setUp() {
        postHearing = new PostHearing();
    }

    @Test
    public void givenActionTypeSetAsideGrantedSelected_shouldThrowError() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.GRANT);

        assertThrows(IllegalArgumentException.class, () -> getPostHearingReviewDocumentType(postHearing, true));
    }

    @Test
    public void givenActionTypeSetAsideRefusedSelected_shouldSetAsideRefusedDocument() {
        postHearing.setReviewType(PostHearingReviewType.SET_ASIDE);
        postHearing.getSetAside().setAction(SetAsideActions.REFUSE);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(DocumentType.SET_ASIDE_REFUSED);
    }

    @Test
    public void givenActionTypeCorrectionGrantedSelected_shouldThrowError() {
        postHearing.setReviewType(PostHearingReviewType.CORRECTION);
        postHearing.getCorrection().setAction(CorrectionActions.GRANT);

        assertThrows(IllegalArgumentException.class, () -> getPostHearingReviewDocumentType(postHearing, true));
    }

    @Test
    public void givenActionTypeCorrectionRefusedSelected_shouldCorrectionRefusedDocument() {
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
    public void givenActionTypeSor_shouldSorDocument(StatementOfReasonsActions action, DocumentType correctDocumentType) {
        postHearing.setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        postHearing.getStatementOfReasons().setAction(action);

        DocumentType documentType = getPostHearingReviewDocumentType(postHearing, true);

        assertThat(documentType).isEqualTo(correctDocumentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"PERMISSION_TO_APPEAL", "LIBERTY_TO_APPLY"})
    public void givenActionTypeNotSupported_throwError(PostHearingReviewType postHearingReviewType) {
        postHearing.setReviewType(postHearingReviewType);

        assertThrows(IllegalArgumentException.class, () -> getPostHearingReviewDocumentType(postHearing, true));
    }
}
