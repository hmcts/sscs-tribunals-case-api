package uk.gov.hmcts.reform.sscs.ccd.presubmit.requestinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class RequestForInformationAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private RequestForInformationAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new RequestForInformationAboutToStartHandler();

        when(callback.getEvent()).thenReturn(EventType.REQUEST_FOR_INFORMATION);

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonRequestForInformationEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenARequestForInformationRequestWithRep_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getInformationFromPartySelected();
        assertEquals(2, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getInformationFromPartySelected());
    }

    @Test
    public void givenARequestForInformationRequestWithJointParty_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getJointParty().setHasJointParty(YES);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getInformationFromPartySelected();
        assertEquals(2, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getInformationFromPartySelected());
    }

    @Test
    public void givenARequestForInformationRequestWithJointPartyAndRep_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getInformationFromPartySelected();
        assertEquals(3, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel());
        DynamicListItem expectedListItem3 = new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);
        expectedList.add(expectedListItem3);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getInformationFromPartySelected());
    }


    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
    }
}
