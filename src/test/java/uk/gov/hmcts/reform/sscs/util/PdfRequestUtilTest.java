package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class PdfRequestUtilTest {

    SscsCaseData sscsCaseData;

    @BeforeEach
    void setUp() {
        sscsCaseData = new SscsCaseData();
    }

    @ParameterizedTest
    @CsvSource(value = {
        "SET_ASIDE,SET_ASIDE_APPLICATION",
        "CORRECTION,CORRECTION_APPLICATION"
    })
    void getPostHearingDocumentType_returnsDocumentType(PostHearingRequestType postHearingRequestType, DocumentType documentType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThat(PdfRequestUtil.getPostHearingDocumentType(sscsCaseData.getPostHearing().getRequestType())).isEqualTo(documentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = { // TODO remove as each type is implemented
            "STATEMENT_OF_REASONS",
            "PERMISSION_TO_APPEAL",
            "LIBERTY_TO_APPLY"
        })
    void getPostHearingDocumentType_throwsExceptionWhenUnexpectedRequestType(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.getPostHearingDocumentType(postHearingRequestType))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Unexpected request type: " + postHearingRequestType);
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = { // TODO remove as each type is implemented
            "PERMISSION_TO_APPEAL",
            "LIBERTY_TO_APPLY"
        },
        mode = EXCLUDE
    )
    void getRequestDetailsForPostHearingType_doesNotThrowExceptionForImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertDoesNotThrow(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData));
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS"
        },
        mode = EXCLUDE
    )
    void getRequestDetailsForPostHearingType_throwsExceptionForNotImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getRequestDetailsForPostHearingType has unexpected postHearingRequestType: ");
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = { // TODO remove as each type is implemented
            "PERMISSION_TO_APPEAL",
            "LIBERTY_TO_APPLY"
        },
        mode = EXCLUDE
    )
    void getNoticeBody_doesNotThrowExceptionForImplementedTypes(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertDoesNotThrow(() -> PdfRequestUtil.getNoticeBody(sscsCaseData, true));
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS"
        },
        mode = EXCLUDE
    )
    void getNoticeBody_throwsExceptionForNotImplementedTypes(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertThatThrownBy(() -> PdfRequestUtil.getNoticeBody(sscsCaseData, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getNoticeBody has unexpected postHearingReviewType: ");
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeSetAside() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.SET_ASIDE);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true)).isEqualTo(YES);
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeCorrection() {
        sscsCaseData.getDocumentGeneration().setCorrectionGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.CORRECTION);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true)).isEqualTo(YES);
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeSor() {
        sscsCaseData.getDocumentGeneration().setStatementOfReasonsGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true)).isEqualTo(YES);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"PERMISSION_TO_APPEAL","LIBERTY_TO_APPLY"})
    void getGenerateNoticeThrowsError_whenUnimplementedPostHearingReviewType(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertThatThrownBy(() -> PdfRequestUtil.getGenerateNotice(sscsCaseData, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getGenerateNotice has unexpected PostHearingReviewType: ");
    }

    @Test
    void givenPostHearingsEnabledIsFalse_getGenerateNoticeReturnsGetGenerateNotice() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, false)).isEqualTo(YES);
    }

    @Test
    void givenPostHearingReviewTypeIsNull_getGenerateNoticeReturnsGetGenerateNotice() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true)).isEqualTo(YES);
    }

}