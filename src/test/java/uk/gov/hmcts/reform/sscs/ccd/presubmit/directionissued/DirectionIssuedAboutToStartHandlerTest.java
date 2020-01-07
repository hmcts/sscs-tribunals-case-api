package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

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
public class DirectionIssuedAboutToStartHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private DirectionIssuedAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new DirectionIssuedAboutToStartHandler();

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenValidAppeal_populateExtensionNextEventDropdown() {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("sendToListing", "List for hearing"));
        listOptions.add(new DynamicListItem("noFurtherAction", "No further action"));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, listOptions.size());
    }

    @Test
    @Parameters({"INCOMPLETE_APPLICATION", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED", "INTERLOCUTORY_REVIEW_STATE", "PENDING_APPEAL", "INCOMPLETE_APPLICATION_VOID_STATE", "VOID_STATE"})
    public void givenNonValidAppeal_populateExtensionNextEventDropdown(State state) {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(state);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("sendToListing", "List for hearing"));
        listOptions.add(new DynamicListItem("noFurtherAction", "No further action"));
        listOptions.add(new DynamicListItem("sendToValidAppeal", "Make valid appeal"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(3, listOptions.size());
    }

    @Test
    public void givenValidAppealWithTimeExtension_populateDirectionTypeDropdown() {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setTimeExtensionRequested("Yes");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("provideInformation", "Provide information"));
        listOptions.add(new DynamicListItem("grantExtension", "Allow time extension"));
        listOptions.add(new DynamicListItem("refuseExtension", "Refuse time extension"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(3, listOptions.size());
    }

    @Test
    @Parameters({"INCOMPLETE_APPLICATION", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED", "INTERLOCUTORY_REVIEW_STATE", "PENDING_APPEAL", "INCOMPLETE_APPLICATION_VOID_STATE", "VOID_STATE"})
    public void givenNonValidAppealWithTimeExtension_populateDirectionTypeDropdown(State state) {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(state);
        sscsCaseData.setTimeExtensionRequested("Yes");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("appealToProceed", "Appeal to Proceed"));
        listOptions.add(new DynamicListItem("provideInformation", "Provide information"));
        listOptions.add(new DynamicListItem("grantExtension", "Allow time extension"));
        listOptions.add(new DynamicListItem("refuseExtension", "Refuse time extension"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(4, listOptions.size());
    }

    @Test
    public void givenValidAppealWithNoTimeExtension_populateDirectionTypeDropdown() {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("provideInformation", "Provide information"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(1, listOptions.size());
    }

    @Test
    @Parameters({"INCOMPLETE_APPLICATION", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED", "INTERLOCUTORY_REVIEW_STATE", "PENDING_APPEAL", "INCOMPLETE_APPLICATION_VOID_STATE", "VOID_STATE"})
    public void givenNonValidAppealWithNoTimeExtension_populateDirectionTypeDropdown(State state) {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(state);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem("appealToProceed", "Appeal to Proceed"));
        listOptions.add(new DynamicListItem("provideInformation", "Provide information"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(2, listOptions.size());
    }

}
