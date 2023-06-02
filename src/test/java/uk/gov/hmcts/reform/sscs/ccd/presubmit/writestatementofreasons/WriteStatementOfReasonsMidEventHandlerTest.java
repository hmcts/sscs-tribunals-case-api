package uk.gov.hmcts.reform.sscs.ccd.presubmit.writestatementofreasons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.SOR_WRITE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;

@ExtendWith(MockitoExtension.class)
class WriteStatementOfReasonsMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String CASE_ID = "123123";
    public static final String GENERATE_DOCUMENT = "generateDocument";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private GenerateFile generateFile;

    private SscsCaseData caseData;

    private WriteStatementOfReasonsMidEventHandler handler;

    private final String templateIdEnglish = "templateIdEnglish.docx";
    private final String templateIdWelsh = "templateIdWelsh.docx";


    @BeforeEach
    void setUp() {
        handler = new WriteStatementOfReasonsMidEventHandler(true, generateFile, templateIdEnglish, templateIdWelsh);

        caseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .documentGeneration(DocumentGeneration.builder()
                .writeStatementOfReasonsGenerateNotice(YES)
                .build())
            .build();
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(SUBMITTED, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new WriteStatementOfReasonsMidEventHandler(false, generateFile, templateIdEnglish, templateIdWelsh);
        when(callback.getEvent()).thenReturn(SOR_WRITE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = YesNo.class, names = "NO")
    @NullSource
    void givenGenerateNoticeIsNoOrNull_doNothing(YesNo value) {
        caseData.getDocumentGeneration().setWriteStatementOfReasonsGenerateNotice(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenGenerateNoticeYes_generateNotice() {
        String dmUrl = "http://dm-store/documents/123";
        when(generateFile.assemble(any())).thenReturn(dmUrl);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        caseData.getDocumentGeneration().setWriteStatementOfReasonsBodyContent("Something");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        String expectedFileName = "Statement of Reasons.pdf";
        DocumentLink documentLink = DocumentLink.builder()
            .documentBinaryUrl(dmUrl + "/binary")
            .documentUrl(dmUrl)
            .documentFilename(expectedFileName)
            .build();
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isEqualTo(documentLink);
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    @NullSource
    void givenRequestDetailsIsBlank_returnResponseWithError(String emptyValue) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        caseData.getDocumentGeneration().setStatementOfReasonsBodyContent(emptyValue);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Please enter request details to generate a statement of reasons document");
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn("something else");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenPostHearingsEnabledFalse_addsErrorToResponse() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        handler = new WriteStatementOfReasonsMidEventHandler(false, generateFile, templateIdEnglish, templateIdWelsh);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .isNotEmpty()
            .hasSize(1)
            .containsOnly("Post hearings is not currently enabled");
    }

}
