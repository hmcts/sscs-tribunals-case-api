package uk.gov.hmcts.reform.sscs.ccd.presubmit.getfirsttierdocuments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class GetFirstTierDocumentsAboutToStartEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private GetFirstTierDocumentsAboutToStartEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new GetFirstTierDocumentsAboutToStartEventHandler(true, true);

        when(callback.getEvent()).thenReturn(EventType.GET_FIRST_TIER_DOCUMENTS);
        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    public void givenANonGetFirstTierDocumentsEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
    }

    @Test
    public void givenEmptyUploadedHearingRecordings_thenReturnWarning() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getWarnings().stream()
                .findFirst()
                .orElse("");
        assertThat(error).isEqualTo("There is no hearing recording uploaded on this case, please email the RPC to upload before completing this event");
    }

    @Test
    public void givenUploadedHearingRecordings_thenNoWarning() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        callback.getCaseDetails().getCaseData().getSscsHearingRecordingCaseData().setSscsHearingRecordings(
                List.of(SscsHearingRecording.builder().build())
        );

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }


}
