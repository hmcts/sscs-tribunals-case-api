package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingCaseData;

public class ActionPostponementRequestAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private ActionPostponementRequestAboutToStartHandler handler;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new ActionPostponementRequestAboutToStartHandler();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build())
                .documentGeneration(DocumentGeneration.builder()
                        .directionNoticeContent("Body Content")
                        .build())
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build())
                .schedulingAndListingFields(SchedulingAndListingFields.builder()
                        .hearingRoute(HearingRoute.LIST_ASSIST).build())
                .hearings(List.of(Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingDate(LocalDate.now().plusDays(1).toString())
                                .time("10:00")
                                .build())
                        .build()))
                .postponementRequest(PostponementRequest.builder()
                        .actionPostponementRequestSelected("grant")
                        .build())
                .build();
    }

    @Test
    void givenCaseHasActionPostponementRequestSelected_thenClearActionPostponementRequestSelected() {
        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(sscsCaseData.getPostponementRequest().getActionPostponementRequestSelected()).isNull();
    }
}
