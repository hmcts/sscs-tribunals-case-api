package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
        "CORRECTION,CORRECTION_APPLICATION",
        "STATEMENT_OF_REASONS,STATEMENT_OF_REASONS_APPLICATION",
        "LIBERTY_TO_APPLY,LIBERTY_TO_APPLY_APPLICATION",
    })
    void getPostHearingDocumentType_returnsDocumentType(PostHearingRequestType postHearingRequestType, DocumentType documentType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThat(PdfRequestUtil.getPostHearingDocumentType(postHearingRequestType)).isEqualTo(documentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = { // TODO remove as each type is implemented
            "PERMISSION_TO_APPEAL"
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
            "PERMISSION_TO_APPEAL"
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_doesNotThrowExceptionForImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertDoesNotThrow(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData));
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY"
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_throwsExceptionForNotImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getRequestDetailsForPostHearingType has unexpected postHearingRequestType: ");
    }

    @Disabled("Re-enable when rest of post hearings B types are implemented into the enum")
    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = { // TODO add names of unimplemented post hearings B types
        },
        mode = EXCLUDE
    )
    void getNoticeBody_doesNotThrowExceptionForImplementedTypes(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertDoesNotThrow(() -> PdfRequestUtil.getNoticeBody(sscsCaseData, true, true));
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY",
            "PERMISSION_TO_APPEAL"
        },
        mode = EXCLUDE
    )
    void getNoticeBody_throwsExceptionForNotImplementedTypes(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertThatThrownBy(() -> PdfRequestUtil.getNoticeBody(sscsCaseData, true, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getNoticeBody has unexpected postHearingReviewType: ");
    }

    @Test
    void getNoticeBody_throwsExceptionWhenLibertyToApplyAndIsPostHearingsBEnabledIsFalse() {
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        assertThatThrownBy(() -> PdfRequestUtil.getNoticeBody(sscsCaseData, true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("isPostHearingsBEnabled is false - Liberty to Apply is not available");
    }


    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeSetAside() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.SET_ASIDE);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, false)).isEqualTo(YES);
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeCorrection() {
        sscsCaseData.getDocumentGeneration().setCorrectionGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.CORRECTION);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, false)).isEqualTo(YES);
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeSor() {
        sscsCaseData.getDocumentGeneration().setStatementOfReasonsGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, false)).isEqualTo(YES);
    }

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypeLta() {
        sscsCaseData.getDocumentGeneration().setLibertyToApplyGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, true)).isEqualTo(YES);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {"PERMISSION_TO_APPEAL"})
    void getGenerateNoticeThrowsError_whenUnimplementedPostHearingReviewType(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertThatThrownBy(() -> PdfRequestUtil.getGenerateNotice(sscsCaseData, true, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getGenerateNotice has unexpected PostHearingReviewType: ");
    }

    @Test
    void getGenerateNoticeThrowsError_whenLibertyToApplyAndIsPostHearingsBEnabledFalse() {
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        assertThatThrownBy(() -> PdfRequestUtil.getGenerateNotice(sscsCaseData, true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("isPostHearingsBEnabled is false - Liberty to Apply is not available");
    }

    @Test
    void givenPostHearingsEnabledIsFalse_getGenerateNoticeReturnsGetGenerateNotice() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, false, false)).isEqualTo(YES);
    }

    @Test
    void givenPostHearingReviewTypeIsNull_getGenerateNoticeReturnsGetGenerateNotice() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(YES);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, true)).isEqualTo(YES);
    }

}