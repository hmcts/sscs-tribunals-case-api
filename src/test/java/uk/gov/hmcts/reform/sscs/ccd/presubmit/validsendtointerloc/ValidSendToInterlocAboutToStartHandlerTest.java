package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ValidSendToInterlocAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ValidSendToInterlocAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ValidSendToInterlocAboutToStartHandler(false);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(VALID_SEND_TO_INTERLOC);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void populatesSelectWhoReviewsCaseDropDown(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(expected, response.getData().getSelectWhoReviewsCase());
    }

    @Test
    @Parameters({"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenPostponementsFeatureOn_populatesSelectWhoReviewsCaseDropDown(EventType eventType) {
        ReflectionTestUtils.setField(handler, "postponementsFeature", true);

        when(callback.getEvent()).thenReturn(eventType);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        listOptions.add(new DynamicListItem(POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getId(), POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(expected, response.getData().getSelectWhoReviewsCase());
    }

    @Test
    public void givenAValidSendToInterlocRequestWithRep_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getOriginalSender();
        assertEquals(2, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getOriginalSender());
    }

    @Test
    public void givenAValidSendToInterlocRequestWithJointParty_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getJointParty().setHasJointParty(YES);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getOriginalSender();
        assertEquals(2, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getOriginalSender());
    }

    @Test
    public void givenAValidSendToInterlocRequestWithJointPartyAndRep_thenPopulateDropdownWithPartiesOnCase() {
        sscsCaseData.getJointParty().setHasJointParty(YES);
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList result = response.getData().getOriginalSender();
        assertEquals(3, result.getListItems().size());

        DynamicListItem expectedListItem1 = new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel());
        DynamicListItem expectedListItem2 = new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel());
        DynamicListItem expectedListItem3 = new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel());
        List<DynamicListItem> expectedList = new ArrayList<>();
        expectedList.add(expectedListItem1);
        expectedList.add(expectedListItem2);
        expectedList.add(expectedListItem3);

        assertEquals(new DynamicList(expectedListItem1, expectedList), response.getData().getOriginalSender());
    }

}
