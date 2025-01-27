package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOutcomeDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Venue;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@RunWith(JUnitParamsRunner.class)
public class AmendHearingOutcomeAboutToSubmitHandlerTest {

    @InjectMocks
    AmendHearingOutcomeAboutToSubmitHandler handler;

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    @BeforeEach
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.AMEND_HEARING_OUTCOME);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

    }


    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenDifferentHearingOutcomeSelectedMoreThanOnce_thenAlterHearingOutcome() {

        List<String> hearingsSelected = new ArrayList<String>();

        sscsCaseData = SscsCaseData.builder()
                .hearingOutcomes(new ArrayList<>())
                .completedHearingsList(new ArrayList<>())
                .build();

        List completedHearingList = Collections.singletonList(Hearing.builder()
                        .value(HearingDetails.builder()
                                .hearingId("2")
                                .venue(Venue.builder().name("venue 1 name").build())
                                .start(LocalDateTime.of(2024,6,30,10,00))
                                .end(LocalDateTime.of(2024,6,30,13,00))
                                .epimsId("123456")
                                .hearingChannel(HearingChannel.FACE_TO_FACE)
                                .build())
                .build());

        DynamicList dynamicList = new DynamicList(new DynamicListItem("2", "test"), List.of(new DynamicListItem("1", "test"), new DynamicListItem("2", "test2")));
        sscsCaseData.setCompletedHearingsList(completedHearingList);
        sscsCaseData.getHearingOutcomes().add(HearingOutcome.builder()
                .value(
                        HearingOutcomeDetails.builder()
                                .completedHearingId("2")
                                .completedHearings(dynamicList)
                                .build()
                ).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT,callback,USER_AUTHORISATION);

        assertThat(response.getData().getHearingOutcomes().get(0).getValue().getCompletedHearingId()).isEqualTo("2");

    }

}
