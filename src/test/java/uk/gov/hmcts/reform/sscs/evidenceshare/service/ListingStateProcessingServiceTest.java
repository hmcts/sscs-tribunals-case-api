package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.Collections;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;


@RunWith(JUnitParamsRunner.class)
public class ListingStateProcessingServiceTest {

    private ListingStateProcessingService listingStateProcessingService;
    private SscsCaseData sscsCaseData;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    UpdateCcdCaseService updateCcdCaseService;

    @Mock
    IdamService idamService;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;


    @Before
    public void setup() {
        openMocks(this);
        listingStateProcessingService = new ListingStateProcessingService(updateCcdCaseService, idamService);
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
        listingStateProcessingService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);

        verify(updateCcdCaseService).updateCaseV2(
                anyLong(),
                eq(EventType.INTERLOC_REVIEW_STATE_AMEND.getType()),
                eq(""),
                eq(""),
                any(),
                consumerArgumentCaptor.capture()
        );
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertEquals(sscsCaseData.getInterlocReviewState(), InterlocReviewState.NONE);
    }

    @Test
    public void givenDormantCase_caseShouldNotUpdate() {
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        when(caseDetails.getState()).thenReturn(sscsCaseData.getState());
        listingStateProcessingService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(updateCcdCaseService, never()).updateCase(any(), any(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void givenNonDormantCase_caseShouldUpdate() {
        listingStateProcessingService.processCaseState(callback,sscsCaseData, EventType.CONFIRM_PANEL_COMPOSITION);
        verify(updateCcdCaseService, times(1)).triggerCaseEventV2(any(), any(), anyString(), anyString(), any());
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
        listingStateProcessingService.processCaseState(callback,sscsCaseData, EventType.UPDATE_OTHER_PARTY_DATA);

        verify(updateCcdCaseService).updateCaseV2(
                anyLong(),
                eq(EventType.READY_TO_LIST.getType()),
                eq("Ready to list"),
                eq("Update to ready to list event as there is no further information to assist the tribunal and no dispute."),
                any(),
                consumerArgumentCaptor.capture()
        );
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(sscsCaseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertNull(sscsCaseData.getDirectionDueDate());
    }
}
