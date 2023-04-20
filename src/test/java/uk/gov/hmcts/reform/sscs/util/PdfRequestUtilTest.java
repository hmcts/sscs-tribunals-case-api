package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
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
    })
    void getPostHearingDocumentType_returnsDocumentType(PostHearingRequestType postHearingRequestType, DocumentType documentType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThat(PdfRequestUtil.getPostHearingDocumentType(postHearingRequestType)).isEqualTo(documentType);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = { // TODO remove as each type is implemented
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
        names = {
            "SET_ASIDE",
            "CORRECTION"
        }
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
            "STATEMENT_OF_REASONS"
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_throwsExceptionForNotImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.getRequestDetailsForPostHearingType(sscsCaseData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("handlePostHearing has unexpected postHearingRequestType: ");
    }

}