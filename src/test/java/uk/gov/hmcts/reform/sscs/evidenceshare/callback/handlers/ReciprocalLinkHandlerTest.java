package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ASSOCIATE_CASE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class ReciprocalLinkHandlerTest {

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    private ReciprocalLinkHandler handler;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Captor
    private ArgumentCaptor<SscsCaseData> capture;

    HashMap<String, String> map = new HashMap<String, String>();

    @Before
    public void setUp() {
        openMocks(this);
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);

        handler = new ReciprocalLinkHandler(ccdService, idamService);

        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(
                Appellant.builder().identity(Identity.builder().nino("AB00000Y").build()).build())
            .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getId()).thenReturn(7656765L);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        map.put("case.appeal.appellant.identity.nino", "AB00000Y");
    }

    @Test
    @Parameters({"VALID_APPEAL_CREATED", "DRAFT_TO_VALID_APPEAL_CREATED", "NON_COMPLIANT", "DRAFT_TO_NON_COMPLIANT", "INCOMPLETE_APPLICATION_RECEIVED", "DRAFT_TO_INCOMPLETE_APPLICATION"})
    public void givenAValidEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonReciprocalLinkEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenANonReciprocalLinkCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAReciprocalLinkCallbackType_thenReturnTrue() {
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenAssociatedCase_thenAddReciprocalLinkToAssociatedCase() {

        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build());

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService).updateCase(capture.capture(), eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());

        assertEquals("7656765", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenAssociatedCaseWithExistingAssociatedCase_thenAddReciprocalLinkToAssociatedCase() {
        List<CaseLink> caseLinks = new ArrayList<>();
        caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());

        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().associatedCase(caseLinks).build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build());

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService).updateCase(capture.capture(), eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
        assertEquals("1", capture.getValue().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", capture.getValue().getAssociatedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddReciprocalLinkToAllCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(34343434L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build());

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService, times(2)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(34343434L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
        assertEquals("7656765", capture.getAllValues().get(0).getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", capture.getAllValues().get(1).getAssociatedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenMoreThan10AssociatedCases_thenDoNotAddReciprocalLinkToAllCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765671L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765672L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765673L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765674L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765675L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765676L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765677L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765678L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(56765679L).data(SscsCaseData.builder().build()).build());
        associatedCaseList.add(SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build());

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

    @Test
    public void addNoAssociatedCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any());
    }

}
