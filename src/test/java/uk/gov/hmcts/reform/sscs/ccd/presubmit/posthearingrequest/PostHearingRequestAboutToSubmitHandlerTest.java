package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
class PostHearingRequestAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    public static final String SET_ASIDE_APPLICATION_FROM_FTA_PDF = "Set Aside Application from FTA.pdf";

    private PostHearingRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    private SscsDocument expectedDocument;

    @Mock
    private FooterService footerService;

    @BeforeEach
    void setUp() {
        handler = new PostHearingRequestAboutToSubmitHandler(true, footerService);

        DocumentLink documentLink = DocumentLink.builder()
            .documentFilename(SET_ASIDE_APPLICATION_FROM_FTA_PDF)
            .build();
        DocumentStaging documentStaging = DocumentStaging.builder()
            .previewDocument(documentLink)
            .build();
        Appeal appeal = Appeal.builder()
            .mrnDetails(MrnDetails.builder()
                .dwpIssuingOffice("3")
                .build())
            .build();
        caseData = SscsCaseData.builder()
            .appeal(appeal)
            .state(State.DORMANT_APPEAL_STATE)
            .ccdCaseId("1234")
            .postHearing(PostHearing.builder().build())
            .documentStaging(documentStaging)
            .build();

        expectedDocument = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentLink(documentLink)
                .documentFileName(documentLink.getDocumentFilename())
                .documentDateAdded(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
                .build())
            .build();
    }

    @Test
    void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REQUEST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenAInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(READY_TO_LIST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAInvalidCallbackType_thenReturnFalse() {
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new PostHearingRequestAboutToSubmitHandler(false, footerService);
        when(callback.getEvent()).thenReturn(POST_HEARING_REQUEST);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class, names = {"SET_ASIDE"})
    void shouldReturnWithoutError(PostHearingRequestType requestType) {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().setRequestType(requestType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"SET_ASIDE,Set Aside Application from FTA.pdf,SET_ASIDE_APPLICATION"})
    void givenAPostHearingRequest_footerServiceIsCalledToCreateDocAndAddToBundle(
        PostHearingRequestType requestType,
        String filename,
        DocumentType documentType
    ) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().setRequestType(requestType);
        expectedDocument.getValue().setDocumentType(documentType.getValue());
        DocumentLink postHearingDoc = DocumentLink.builder()
            .documentFilename(filename)
            .build();
        caseData.getDocumentStaging().setPreviewDocument(postHearingDoc);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
            eq(documentType), any(), any(), eq(null), eq(null));
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = {"SET_ASIDE"})
    void givenPreviewDocumentIsNull_doesNotGenerateADocument(PostHearingRequestType requestType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().setRequestType(requestType);
        caseData.getDocumentStaging().setPreviewDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1)
            .containsOnly("There is no post hearing request document");
        verifyNoInteractions(footerService);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = {"SET_ASIDE"})
    void givenPreviewDocumentIsNotAPostHearingDoc_andRequestFormatIsNotUpload_doesNotGenerateADocument(PostHearingRequestType requestType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        caseData.getPostHearing().setRequestType(requestType);
        DocumentLink notPostHearingDoc = DocumentLink.builder()
            .documentFilename("Not a post hearing doc.xml")
            .build();
        caseData.getDocumentStaging().setPreviewDocument(notPostHearingDoc);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1)
            .containsOnly("There is no post hearing request document");
        verifyNoInteractions(footerService);
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class,
        names = {"SET_ASIDE"})
    void givenUploadedDocument_previewDocumentIsRenamedToExpectedPostHearingFormat(PostHearingRequestType postHearingRequestType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        caseData.getPostHearing().setRequestType(postHearingRequestType);
        caseData.getPostHearing().getSetAside().setRequestFormat(RequestFormat.UPLOAD);
        String dmUrl = "http://dm-store/documents/123";
        DocumentLink uploadedDocument = DocumentLink.builder()
            .documentFilename("A random filename.pdf")
            .documentUrl(dmUrl)
            .documentBinaryUrl(dmUrl + "/binary")
            .build();
        caseData.getDocumentStaging().setPreviewDocument(uploadedDocument);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        String expectedFileName = String.format("%s Application from FTA.pdf", postHearingRequestType.getDescriptionEn());
        DocumentLink expectedDocument = uploadedDocument.toBuilder()
            .documentFilename(expectedFileName)
            .build();
        assertThat(response.getData().getDocumentStaging().getPreviewDocument()).isEqualTo(expectedDocument);
    }
}
