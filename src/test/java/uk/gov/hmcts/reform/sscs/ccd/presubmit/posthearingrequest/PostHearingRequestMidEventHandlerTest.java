package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat.GENERATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@ExtendWith(MockitoExtension.class)
class PostHearingRequestMidEventHandlerTest {
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

    private PostHearingRequestMidEventHandler handler;

    private final String templateId = "templateId.docx";


    @BeforeEach
    void setUp() {
        handler = new PostHearingRequestMidEventHandler(true, generateFile, templateId);

        caseData = SscsCaseData.builder()
            .ccdCaseId(CASE_ID)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .build())
            .issueFinalDecisionDate(LocalDate.now())
            .build();
    }

    @Test
    void givenAValidMidEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(POST_HEARING_REQUEST);
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
        handler = new PostHearingRequestMidEventHandler(false, generateFile, templateId);
        when(callback.getEvent()).thenReturn(POST_HEARING_REQUEST);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class, names = {"SET_ASIDE", "CORRECTION", "STATEMENT_OF_REASONS", "LIBERTY_TO_APPLY", "PERMISSION_TO_APPEAL"})
    void givenGenerateNoticeNo_doNothing(PostHearingRequestType requestType) {
        caseData.getDocumentGeneration().setGenerateNotice(NO);
        caseData.getPostHearing().setRequestType(requestType);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class, names = {"SET_ASIDE", "CORRECTION", "STATEMENT_OF_REASONS", "LIBERTY_TO_APPLY", "PERMISSION_TO_APPEAL"})
    void givenGenerateNoticeNull_doNothing(PostHearingRequestType requestType) {
        caseData.getDocumentGeneration().setGenerateNotice(null);
        caseData.getPostHearing().setRequestType(requestType);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = PostHearingRequestType.class, names = {"SET_ASIDE", "CORRECTION", "STATEMENT_OF_REASONS", "LIBERTY_TO_APPLY", "PERMISSION_TO_APPEAL"})
    void givenGenerateNoticeYes_generateNotice(PostHearingRequestType postHearingRequestType) {
        String dmUrl = "http://dm-store/documents/123";
        when(generateFile.assemble(any())).thenReturn(dmUrl);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        caseData.getPostHearing().setRequestType(postHearingRequestType);
        caseData.getPostHearing().getSetAside().setRequestFormat(GENERATE);
        caseData.getPostHearing().getCorrection().setRequestFormat(GENERATE);
        caseData.getPostHearing().getStatementOfReasons().setRequestFormat(GENERATE);
        caseData.getPostHearing().getLibertyToApply().setRequestFormat(GENERATE);
        caseData.getPostHearing().getPermissionToAppeal().setRequestFormat(GENERATE);
        String content = "Something";
        caseData.getDocumentGeneration().setBodyContent(content);
        caseData.getDocumentGeneration().setCorrectionBodyContent(content);
        caseData.getDocumentGeneration().setStatementOfReasonsBodyContent(content);
        caseData.getDocumentGeneration().setLibertyToApplyBodyContent(content);
        caseData.getDocumentGeneration().setPermissionToAppealBodyContent(content);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        String expectedFileName = String.format("%s Application from FTA.pdf", postHearingRequestType.getDescriptionEn());
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

        caseData.getPostHearing().setRequestType(PostHearingRequestType.SET_ASIDE);
        caseData.getPostHearing().getSetAside().setRequestFormat(GENERATE);
        caseData.getDocumentGeneration().setBodyContent(emptyValue);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Please enter request details to generate a post hearing application document");
    }

    @Test
    void givenIssueFinalDecisionDateIsNull_throwsException() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn(GENERATE_DOCUMENT);

        caseData.getPostHearing().setRequestType(PostHearingRequestType.SET_ASIDE);
        caseData.getPostHearing().getSetAside().setRequestFormat(GENERATE);
        caseData.setIssueFinalDecisionDate(null);

        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("issueFinalDecisionDate unexpectedly null for caseId: " + CASE_ID);
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

        caseData.getPostHearing().setRequestType(PostHearingRequestType.SET_ASIDE);
        caseData.getPostHearing().getSetAside().setRequestFormat(GENERATE);

        handler = new PostHearingRequestMidEventHandler(false, generateFile, templateId);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .isNotEmpty()
            .hasSize(1)
            .containsOnly("Post hearings is not currently enabled");
    }

}
