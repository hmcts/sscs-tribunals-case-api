package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@RunWith(JUnitParamsRunner.class)
public class PanelCompositionServiceTest {

    private PanelCompositionService panelCompositionService;
    private SscsCaseData sscsCaseData;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    CcdService ccdService;

    @Mock
    IdamService idamService;


    @Before
    public void setup() {
        openMocks(this);
        panelCompositionService = new PanelCompositionService(ccdService, idamService);
        sscsCaseData = buildCaseData("Bloggs");
        sscsCaseData.setCcdCaseId("1");
        sscsCaseData.getAppeal().getAppellant().getIdentity().setNino("789123");
        sscsCaseData.setDirectionDueDate("11/01/2023");
        sscsCaseData.setState(State.APPEAL_CREATED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    public void givenResponseReceivedCase_thenInterLocReviewIsNone() {
        sscsCaseData.setState(State.RESPONSE_RECEIVED);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        panelCompositionService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        assertEquals(sscsCaseData.getInterlocReviewState(), InterlocReviewState.NONE);
    }

    @Test
    public void givenDormantCase_caseShouldNotUpdate() {
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        panelCompositionService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(ccdService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenNonDormantCase_caseShouldUpdate() {
        panelCompositionService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(ccdService, times(1)).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenCaseFqpmRequiredWithOtherPartHearing_thenRemoveDirectionDueDate() {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id("1")
                .hearingOptions(HearingOptions.builder().scheduleHearing("Yes").build())
                .build())
            .build();
        sscsCaseData.setOtherParties(Collections.singletonList(otherParty));
        sscsCaseData.setIsFqpmRequired(YesNo.YES);
        panelCompositionService.processCaseState(callback,sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);
        assertNull(sscsCaseData.getDirectionDueDate());

    }


}
