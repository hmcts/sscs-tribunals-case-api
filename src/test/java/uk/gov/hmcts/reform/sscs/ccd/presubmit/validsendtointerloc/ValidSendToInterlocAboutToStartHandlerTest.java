package uk.gov.hmcts.reform.sscs.ccd.presubmit.validsendtointerloc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_SEND_TO_INTERLOC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.SelectWhoReviewsCase.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@ExtendWith(MockitoExtension.class)
public class ValidSendToInterlocAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private ValidSendToInterlocAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {
        handler = new ValidSendToInterlocAboutToStartHandler(false, false);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
    }

    private void setupCallback() {
        when(callback.getEvent()).thenReturn(VALID_SEND_TO_INTERLOC);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    void populatesSelectWhoReviewsCaseDropDown(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        setupCallback();

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(expected, response.getData().getSelectWhoReviewsCase());
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"VALID_SEND_TO_INTERLOC", "ADMIN_SEND_TO_INTERLOCUTORY_REVIEW_STATE"})
    public void givenPostponementsFeatureOn_populatesSelectWhoReviewsCaseDropDown(EventType eventType) {
        ReflectionTestUtils.setField(handler, "postponementsFeature", true);

        when(callback.getEvent()).thenReturn(eventType);
        setupCallback();

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
        setupCallback();
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
        setupCallback();
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
        setupCallback();
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
