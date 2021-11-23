package uk.gov.hmcts.reform.sscs.ccd.presubmit.confirmpanelcomposition;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.panelcomposition.PanelCompositionService;

import java.time.LocalDate;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

@RunWith(JUnitParamsRunner.class)
public class ConfirmPanelCompositionSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ConfirmPanelCompositionSubmittedHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);

        handler = new ConfirmPanelCompositionSubmittedHandler(new PanelCompositionService(ccdService, idamService));

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.CONFIRM_PANEL_COMPOSITION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"YES", "NO"})
    public void givenFqpmSetAndDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(LocalDate.now().toString());
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")));

        given(ccdService.updateCase(any(SscsCaseData.class), anyLong(), eq("readyToList"),
                anyString(), anyString(), any(IdamTokens.class)))
                .willReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().state(State.READY_TO_LIST).build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
    }

//    @Test
//    @Parameters({"YES", "NO"})
//    public void givenFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {
//
//        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
//        sscsCaseData.setDwpDueDate(null);
//        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")));
//
//        PreSubmitCallbackResponse<SscsCaseData> response =
//                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
//
//        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
//    }
//
//    @Test
//    @Parameters({"YES", "NO"})
//    public void givenFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {
//
//        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
//        sscsCaseData.setDwpDueDate(null);
//        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));
//
//        PreSubmitCallbackResponse<SscsCaseData> response =
//                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
//
//        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
//    }
//
//    @Test
//    @Parameters({"YES", "NO"})
//    public void givenFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {
//
//        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
//        sscsCaseData.setDwpDueDate(LocalDate.now().toString());
//        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));
//
//        PreSubmitCallbackResponse<SscsCaseData> response =
//                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
//
//        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
//    }

    private CcdValue<OtherParty> buildOtherPartyWithHearing(String id) {
        return buildOtherParty(id, HearingOptions.builder().excludeDates(Arrays.asList(ExcludeDate.builder().build())).build());
    }

    private CcdValue<OtherParty> buildOtherParty(String id, HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .unacceptableCustomerBehaviour(YesNo.YES)
                        .hearingOptions(hearingOptions)
                        .build()).build();
    }
}