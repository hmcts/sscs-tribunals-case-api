package uk.gov.hmcts.reform.sscs.ccd.presubmit.updateotherparty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


@RunWith(JUnitParamsRunner.class)
public class UpdateOtherPartySubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private UpdateOtherPartyAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;


    @Before
    public void setUp() {
        openMocks(this);
        handler = new UpdateOtherPartyAboutToSubmitHandler();

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").build();

        when(callback.getEvent()).thenReturn(EventType.UPDATE_OTHER_PARTY_DATA);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Ignore
    public void givenAValidSubmittedEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"YES", "NO"})
    @Ignore
    public void givenFqpmSetAndDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(LocalDate.now().toString());
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
        assertThat(response.getData().getDwpDueDate(), is(nullValue()));
    }

    @Test
    @Parameters({"YES", "NO"})
    @Ignore
    public void givenFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(null);
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
    }

    @Test
    @Parameters({"YES", "NO"})
    @Ignore
    public void givenFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsReadyToList(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(null);
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
    }

    @Test
    @Parameters({"YES", "NO"})
    @Ignore
    public void givenFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable(String isFqpmRequired) {

        sscsCaseData.setIsFqpmRequired(isFqpmRequired.equalsIgnoreCase("yes") ? YesNo.YES : YesNo.NO);
        sscsCaseData.setDwpDueDate(LocalDate.now().toString());
        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
    }

    @Test
    @Ignore
    public void givenNoFqpmSetAndNoDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable() {

        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
    }

    @Test
    @Ignore
    public void givenNoFqpmSetAndDueDateSetAndNotAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable() {

        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherParty("1", null)));
        sscsCaseData.setDwpDueDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
    }

    @Test
    @Ignore
    public void givenNoFqpmSetAndNoDueDateSetAndAllOtherPartyHearingOptionsSet_thenCaseStateIsNotListable() {

        sscsCaseData.setOtherParties(Arrays.asList(buildOtherPartyWithHearing("2"), buildOtherPartyWithHearing("1")));

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
    }

    private CcdValue<OtherParty> buildOtherPartyWithHearing(String id) {
        return buildOtherParty(id, HearingOptions.builder().excludeDates(Arrays.asList(ExcludeDate.builder().build())).build());
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return buildOtherParty(id, null);
    }

    private CcdValue<OtherParty> buildOtherParty(String id, HearingOptions hearingOptions) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .hearingOptions(hearingOptions)
                        .unacceptableCustomerBehaviour(YesNo.YES)
                        .build())
                .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
    }


}
