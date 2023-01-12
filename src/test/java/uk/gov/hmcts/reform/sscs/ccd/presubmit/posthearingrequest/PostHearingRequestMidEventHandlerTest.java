package uk.gov.hmcts.reform.sscs.ccd.presubmit.posthearingrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.POST_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.Event;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestFormat;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.docassembly.GenerateFile;

@ExtendWith(MockitoExtension.class)
class PostHearingRequestMidEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

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

        caseData = SscsCaseData.builder().build();
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
    @EnumSource(
        value = YesNo.class,
        names = {"NO"})
    @NullSource
    void givenGenerateNoticeNotYes_doNothing(YesNo value) {
        caseData.getDocumentGeneration().setGenerateNotice(value);

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(callback.getPageId()).thenReturn("generateNotice");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenGenerateNoticeYes_generateNotice() {
        String dmUrl = "http://dm-store/documents/123";
        when(generateFile.assemble(any())).thenReturn(dmUrl);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);
        when(callback.getPageId()).thenReturn("generateDocument");
        caseData.getDocumentGeneration().setGenerateNotice(YES);
        caseData.getPostHearing().setRequestType(PostHearingRequestType.SET_ASIDE);
        caseData.getPostHearing().getSetAside().setRequestFormat(RequestFormat.GENERATE);
        caseData.getPostHearing().setRequestReason("This is the reason for the set aside request");
        caseData.setEvents(List.of(new Event(new EventDetails("2017-06-20T12:00:00", "issueFinalDecision", "issued"))));

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getPostHearing().getPreviewDocument()).isNotNull();
        DocumentLink documentLink = DocumentLink.builder()
            .documentBinaryUrl(dmUrl + "/binary")
            .documentUrl(dmUrl)
            .documentFilename("Post Hearing Request.pdf")
            .build();
        assertThat(response.getData().getPostHearing().getPreviewDocument()).isEqualTo(documentLink);
    }

    @Test
    void givenOtherPageId_doNothing() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(callback.getPageId()).thenReturn("test page id");

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

}
