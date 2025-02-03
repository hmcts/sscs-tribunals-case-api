package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.time.LocalDateTime;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;


@RunWith(JUnitParamsRunner.class)
public class AmendHearingOutcomeMidEventHandlerTest {

    private AmendHearingOutcomeMidEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new AmendHearingOutcomeMidEventHandler();

        when(callback.getEvent()).thenReturn(EventType.AMEND_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdDd").appeal(Appeal.builder().build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(CallbackType.MID_EVENT, callback)).isTrue();
    }

    @Test
    public void givenAnInvalidAboutToSubmitEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertThat(handler.canHandle(CallbackType.MID_EVENT, callback)).isFalse();
    }

    @Test
    public void givenAHearingWasAdded_ThenReturnError() {
        sscsCaseData.setHearingOutcomes(List.of(
            HearingOutcome.builder().value(HearingOutcomeDetails.builder().completedHearingId(null).build()).build(),
            HearingOutcome.builder().value(
                    HearingOutcomeDetails.builder()
                            .hearingOutcomeId("17")
                            .hearingChannelId(HearingChannel.FACE_TO_FACE)
                            .hearingStartDateTime(LocalDateTime.now().minusHours(1))
                            .hearingEndDateTime(LocalDateTime.now())
                            .completedHearingId("123").build()).build()
        ));
        var response = handler.handle(CallbackType.MID_EVENT, callback, "userAuthorisation");
        assertThat(response.getErrors()).contains("You cannot create a new hearing outcome in Amend Hearing Outcome, select  ‘Add New hearing outcome’ event to add another outcome.");
    }

    @Test
    public void givenNoHearingAdded_ThenReturnResponse() {
        sscsCaseData.setHearingOutcomes(List.of(
            HearingOutcome.builder().value(
                    HearingOutcomeDetails.builder()
                            .hearingOutcomeId("17")
                            .hearingChannelId(HearingChannel.FACE_TO_FACE)
                            .hearingStartDateTime(LocalDateTime.now().minusHours(1))
                            .hearingEndDateTime(LocalDateTime.now())
                            .completedHearingId("123").build()).build()
        ));
        var response = handler.handle(CallbackType.MID_EVENT, callback, "userAuthorisation");
        assertThat(response.getErrors()).isEmpty();
    }

}
