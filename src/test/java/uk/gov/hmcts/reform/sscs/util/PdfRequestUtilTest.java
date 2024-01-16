package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingReviewType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;

class PdfRequestUtilTest {

    public static final String EXPECTED_CONTENT = "Expected body content";
    SscsCaseData sscsCaseData;

    static boolean postHearingRequestTypeHasMoreThan5Values() {
        return PostHearingRequestType.values().length > 5;
    }

    static boolean postHearingReviewTypeHasMoreThan5Values() {
        return PostHearingReviewType.values().length > 5;
    }

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
        "PERMISSION_TO_APPEAL,PERMISSION_TO_APPEAL_APPLICATION"
    })
    void getPostHearingDocumentType_returnsDocumentType(PostHearingRequestType postHearingRequestType, DocumentType documentType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThat(PdfRequestUtil.getPostHearingDocumentType(postHearingRequestType)).isEqualTo(documentType);
    }

    @EnabledIf("postHearingRequestTypeHasMoreThan5Values")
    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY",
            "PERMISSION_TO_APPEAL"
        },
        mode = EXCLUDE)
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
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_doesNotThrowExceptionForImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertDoesNotThrow(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData));
    }

    @EnabledIf("postHearingRequestTypeHasMoreThan5Values")
    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY",
            "PERMISSION_TO_APPEAL"
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_throwsExceptionForNotImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("getRequestDetailsForPostHearingType has unexpected postHearingRequestType: ");
    }

    @EnabledIf("postHearingRequestTypeHasMoreThan5Values")
    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class)
    void getNoticeBody_doesNotThrowExceptionForImplementedTypes(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertDoesNotThrow(() -> PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(),true, true));
    }

    @EnabledIf("postHearingRequestTypeHasMoreThan5Values") // TODO
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
        assertThatThrownBy(() -> PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("caseData has unexpected postHearingReviewType: ");
    }

    @Test
    void getNoticeBody_throwsExceptionWhenLibertyToApplyAndIsPostHearingsBEnabledIsFalse() {
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        assertThatThrownBy(() -> PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .isEqualTo(PdfRequestUtil.POST_HEARINGS_B_EXCEPTION);
    }

    @Test
    void getNoticeBody_throwsExceptionWhenPermissionToAppealAndIsPostHearingsBEnabledIsFalse() {
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        assertThatThrownBy(() -> PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .isEqualTo(PdfRequestUtil.POST_HEARINGS_B_EXCEPTION);
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
        },
        mode = EXCLUDE
    )
    void getNoticeBody_throwsExceptionWhenPostHearingsBEnabledIsFalse(PostHearingReviewType postHearingReviewType) {
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);
        assertThatThrownBy(() -> PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .isEqualTo(PdfRequestUtil.POST_HEARINGS_B_EXCEPTION);
    }
  
    @Test
    void getNoticeBody_returnsBodyContentWhenPostHearingReviewTypeIsNull() {
        sscsCaseData.getDocumentGeneration().setBodyContent(EXPECTED_CONTENT);
        sscsCaseData.getPostHearing().setReviewType(null);
        assertThat(PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, true)
                .getNoticeBody()).isEqualTo(EXPECTED_CONTENT);
    }

    @Test
    void getNoticeBody_returnsBodyContentWhenPostHearingsIsDisabled() {
        sscsCaseData.getDocumentGeneration().setBodyContent(EXPECTED_CONTENT);
        assertThat(PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), false, false)
                .getNoticeBody()).isEqualTo(EXPECTED_CONTENT);
    }

    @Test
    void getNoticeBody_returnsDirectionNoticeContentWhenBodyContentIsNull() {
        sscsCaseData.getDocumentGeneration().setDirectionNoticeContent(EXPECTED_CONTENT);
        assertThat(PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), false, false)
                .getNoticeBody()).isEqualTo(EXPECTED_CONTENT);
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

    @Test
    void getGenerateNoticeReturnsExpected_withPostHearingReviewTypePta() {
        sscsCaseData.getDocumentGeneration().setPermissionToAppealGenerateNotice(YES);
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        assertThat(PdfRequestUtil.getGenerateNotice(sscsCaseData, true, true)).isEqualTo(YES);
    }

    @EnabledIf("postHearingReviewTypeHasMoreThan5Values")
    @ParameterizedTest
    @EnumSource(value = PostHearingReviewType.class, names = {})
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
            .isEqualTo(PdfRequestUtil.POST_HEARINGS_B_EXCEPTION);
    }

    @Test
    void getGenerateNoticeThrowsError_whenPermissionToAppealndIsPostHearingsBEnabledFalse() {
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        assertThatThrownBy(() -> PdfRequestUtil.getGenerateNotice(sscsCaseData, true, false))
            .isInstanceOf(IllegalArgumentException.class)
            .isEqualTo(PdfRequestUtil.POST_HEARINGS_B_EXCEPTION);
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

    @ParameterizedTest
    @MethodSource("caseDataByPostHearingReviewType")
    void signedBySignedRole_populatesFromCorrectFields(SscsCaseData sscsCaseData) {
        NoticeIssuedTemplateBody templateBody = PdfRequestUtil.populateNoticeBodySignedByAndSignedRole(sscsCaseData,
                NoticeIssuedTemplateBody.builder().build(), true, true);
        assertThat(templateBody.getNoticeBody()).isEqualTo("body content");
        assertThat(templateBody.getUserName()).isEqualTo("signed by");
        assertThat(templateBody.getUserRole()).isEqualTo("signed role");
    }

    private static Stream<Arguments> caseDataByPostHearingReviewType() {
        return Stream.of(
                Arguments.of(SscsCaseData.builder()
                        .postHearing(PostHearing.builder().reviewType(PostHearingReviewType.SET_ASIDE).build())
                        .documentGeneration(DocumentGeneration.builder()
                                .bodyContent("body content")
                                .signedBy("signed by")
                                .signedRole("signed role")
                                .build())
                        .build()),
                Arguments.of(SscsCaseData.builder()
                        .postHearing(PostHearing.builder().reviewType(PostHearingReviewType.CORRECTION).build())
                        .documentGeneration(DocumentGeneration.builder()
                                .correctionBodyContent("body content")
                                .correctionSignedBy("signed by")
                                .correctionSignedRole("signed role")
                                .build())
                        .build()),
                Arguments.of(SscsCaseData.builder()
                        .postHearing(PostHearing.builder().reviewType(PostHearingReviewType.STATEMENT_OF_REASONS).build())
                        .documentGeneration(DocumentGeneration.builder()
                                .statementOfReasonsBodyContent("body content")
                                .statementOfReasonsSignedBy("signed by")
                                .statementOfReasonsSignedRole("signed role")
                                .build())
                        .build()),
                Arguments.of(SscsCaseData.builder()
                        .postHearing(PostHearing.builder().reviewType(PostHearingReviewType.LIBERTY_TO_APPLY).build())
                        .documentGeneration(DocumentGeneration.builder()
                                .libertyToApplyBodyContent("body content")
                                .libertyToApplySignedBy("signed by")
                                .libertyToApplySignedRole("signed role")
                                .build())
                        .build())
        );
    }

}