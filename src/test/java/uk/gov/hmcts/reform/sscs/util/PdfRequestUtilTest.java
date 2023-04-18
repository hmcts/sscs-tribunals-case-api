package uk.gov.hmcts.reform.sscs.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

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
        assertThat(PdfRequestUtil.getPostHearingDocumentType(sscsCaseData)).isEqualTo(documentType);
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
        assertThatThrownBy(() -> PdfRequestUtil.getPostHearingDocumentType(sscsCaseData))
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
        assertDoesNotThrow(() -> PdfRequestUtil.setRequestDetailsForPostHearingType(sscsCaseData));
    }

    @ParameterizedTest
    @EnumSource(
        value = PostHearingRequestType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION"
        },
        mode = EXCLUDE
    )
    void setRequestDetailsForPostHearingType_throwsExceptionForNotImplementedTypes(PostHearingRequestType postHearingRequestType) {
        sscsCaseData.getPostHearing().setRequestType(postHearingRequestType);
        assertThatThrownBy(() -> PdfRequestUtil.setRequestDetailsForPostHearingType(sscsCaseData))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageStartingWith("handlePostHearing has unexpected postHearingRequestType: ");
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class, names = {"GRANT", "REFUSE"})
    void givenSetAsideState_andPostHearingsIsEnabled_thenReturnSetAsideDecisionNotice(SetAsideActions setAsideActions) {
        ReflectionTestUtils.setField(PdfRequestUtil.class, "isPostHearingsEnabled", true);
        final String ORIGINAL_LABEL = "label";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(setAsideActions)
                    .build())
                .build())
            .build();

        String documentTypeLabel = PdfRequestUtil.getEmbeddedDocumentTypeLabelForPostHearing(sscsCaseData, ORIGINAL_LABEL);

        String expectedLabel = "Set Aside Decision Notice";
        assertThat(documentTypeLabel).isEqualTo(expectedLabel);
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class, names = {"GRANT", "REFUSE"})
    void givenSetAsideState_andPostHearingsIsDisabled_thenReturnOriginalLabel(SetAsideActions setAsideActions) {
        ReflectionTestUtils.setField(PdfRequestUtil.class, "isPostHearingsEnabled", false);
        final String ORIGINAL_LABEL = "label";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(setAsideActions)
                    .build())
                .build())
            .build();

        String documentTypeLabel = PdfRequestUtil.getEmbeddedDocumentTypeLabelForPostHearing(sscsCaseData, ORIGINAL_LABEL);

        assertThat(documentTypeLabel).isEqualTo(ORIGINAL_LABEL);
    }

}