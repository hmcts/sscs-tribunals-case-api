package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.DirectionTypeItemList.*;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued.ExtensionNextEventItemList.*;

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
        openMocks(this);
        handler = new DirectionIssuedAboutToStartHandler(false);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT"})
    public void givenAValidCallbackType_thenReturnTrue(CallbackType callbackType) {
        assertTrue(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenValidAppeal_populateExtensionNextEventDropdown() {
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, listOptions.size());
    }

    @Test
    public void givenValidAppealWithExtensionNextEventDropdownAlreadyPopulated_thenAutomaticallySelectExtensionNextEventDropdownValue() {
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        sscsCaseData = SscsCaseData.builder().extensionNextEventDl(new DynamicList(NO_FURTHER_ACTION.getCode())).appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));

        DynamicList expected = new DynamicList(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getCode()), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(2, listOptions.size());
    }

    @Test
    @Parameters({"INCOMPLETE_APPLICATION", "INCOMPLETE_APPLICATION_INFORMATION_REQUESTED", "INTERLOCUTORY_REVIEW_STATE"})
    public void givenNonValidAppeal_populateExtensionNextEventDropdown(State state) {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(state);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(SEND_TO_LISTING.getCode(), SEND_TO_LISTING.getLabel()));
        listOptions.add(new DynamicListItem(NO_FURTHER_ACTION.getCode(), NO_FURTHER_ACTION.getLabel()));
        listOptions.add(new DynamicListItem(SEND_TO_VALID_APPEAL.getCode(), SEND_TO_VALID_APPEAL.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getExtensionNextEventDl());
        assertEquals(3, listOptions.size());
    }

    @Test
    public void givenAppealWithTimeExtension_populateDirectionTypeDropdown() {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setTimeExtensionRequested("Yes");

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getCode(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getCode(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_EXTENSION.getCode(), GRANT_EXTENSION.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_EXTENSION.getCode(), REFUSE_EXTENSION.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(4, listOptions.size());
    }

    @Test
    public void givenAppealWithReinstatementRequest_populateDirectionTypeDropdown() {

        handler = new DirectionIssuedAboutToStartHandler(true);

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getCode(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getCode(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_REINSTATEMENT.getCode(), GRANT_REINSTATEMENT.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_REINSTATEMENT.getCode(), REFUSE_REINSTATEMENT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(4, listOptions.size());
    }

    @Test
    public void givenValidAppealWithTimeExtensionAndDirectionTypeDropdownAlreadyPopulated_thenAutomaticallySelectDirectionTypeDropdownValue() {
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        sscsCaseData = SscsCaseData.builder().timeExtensionRequested("Yes").directionTypeDl(new DynamicList(GRANT_EXTENSION.getCode())).appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);


        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getCode(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getCode(), PROVIDE_INFORMATION.getLabel()));
        listOptions.add(new DynamicListItem(GRANT_EXTENSION.getCode(), GRANT_EXTENSION.getLabel()));
        listOptions.add(new DynamicListItem(REFUSE_EXTENSION.getCode(), REFUSE_EXTENSION.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem(GRANT_EXTENSION.getCode(), GRANT_EXTENSION.getCode()), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(4, listOptions.size());
    }

    @Test
    public void givenAppealWithNoTimeExtension_populateDirectionTypeDropdown() {
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        when(callback.getCaseDetails().getState()).thenReturn(State.WITH_DWP);

        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(APPEAL_TO_PROCEED.getCode(), APPEAL_TO_PROCEED.getLabel()));
        listOptions.add(new DynamicListItem(PROVIDE_INFORMATION.getCode(), PROVIDE_INFORMATION.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        DynamicList expected = new DynamicList(new DynamicListItem("", ""), listOptions);
        assertEquals(expected, response.getData().getDirectionTypeDl());
        assertEquals(2, listOptions.size());
    }
}
