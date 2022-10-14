package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionposthearingapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ACTION_POST_HEARING_APPLICATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.GAPS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute.LIST_ASSIST;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(MockitoJUnitRunner.class)
public class ActionPostHearingApplicationAboutToSubmitHandlerTest {

    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private static final String USER_AUTHORISATION = "Bearer token";

    private ActionPostHearingApplicationAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData caseData;

    @Before
    public void setUp() {
        handler = new ActionPostHearingApplicationAboutToSubmitHandler(true);

        when(callback.getEvent()).thenReturn(ACTION_POST_HEARING_APPLICATION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        caseData = SscsCaseData.builder()
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(LIST_ASSIST).build())
            .ccdCaseId("1234")
            .documentGeneration(DocumentGeneration.builder()
                .directionNoticeContent("Body Content")
                .build())
            .documentStaging(DocumentStaging.builder()
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("decisionIssued.pdf")
                    .build())
                .build())
            .build();

        when(caseDetails.getCaseData()).thenReturn(caseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenPostHearingsEnabledFalse_thenReturnFalse() {
        handler = new ActionPostHearingApplicationAboutToSubmitHandler(false);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void givenLaCase_shouldReturnWithoutError() {

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void givenNonLaCase_shouldReturnErrorWithCorrectMessage() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(GAPS)
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("Cannot process Action Post Hearing Application on non Scheduling & Listing Case");
    }
}
