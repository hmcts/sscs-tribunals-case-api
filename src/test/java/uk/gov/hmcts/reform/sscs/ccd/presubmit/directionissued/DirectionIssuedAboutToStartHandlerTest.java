package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType.*;

import java.util.ArrayList;
import java.util.Collections;
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

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

        sscsCaseData = SscsCaseData.builder()
                .generateNotice("Yes")
                .regionalProcessingCenter(RegionalProcessingCenter.builder().name("Birmingham").build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().firstName("APPELLANT")
                                        .lastName("LastNamE")
                                        .build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenNoDwpState_showAppealToProceedAndProvideInformation() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        List<DynamicListItem> listItems = new ArrayList<>();
        listItems.add(new DynamicListItem(APPEAL_TO_PROCEED.getId(), APPEAL_TO_PROCEED.getLabel()));
        listItems.add(new DynamicListItem(PROVIDE_INFORMATION.getId(), PROVIDE_INFORMATION.getLabel()));
        DynamicList expectedDirectionType = new DynamicList(listItems.get(0), listItems);
        assertEquals(expectedDirectionType, response.getData().getSelectDirectionType());
    }

    @Test
    public void givenDwpStateExtensionRequested_showGrantExtensionAndDenyExtension() {
        sscsCaseData.setDwpState("extensionRequested");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(Collections.EMPTY_SET, response.getErrors());
        List<DynamicListItem> listItems = new ArrayList<>();
        listItems.add(new DynamicListItem(GRANT_EXTENSION.getId(), GRANT_EXTENSION.getLabel()));
        listItems.add(new DynamicListItem(DENY_EXTENSION.getId(), DENY_EXTENSION.getLabel()));
        DynamicList expectedDirectionType = new DynamicList(listItems.get(0), listItems);
        assertEquals(expectedDirectionType, response.getData().getSelectDirectionType());
    }

}

