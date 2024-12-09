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
import java.util.function.Consumer;
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
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class ReciprocalLinkHandlerTest {

    public static final String YES = "Yes";
    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private ReciprocalLinkHandler handler;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> capture;

    HashMap<String, String> map = new HashMap<String, String>();

    @Before
    public void setUp() {
        openMocks(this);
        when(callback.getEvent()).thenReturn(EventType.VALID_APPEAL_CREATED);

        handler = new ReciprocalLinkHandler(ccdService, idamService, updateCcdCaseService);

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

        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());

        capture.getValue().accept(associatedCase2);
        assertEquals("7656765", associatedCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals(YES, associatedCase2.getData().getLinkedCasesBoolean());
    }

    @Test
    public void givenAssociatedCaseWithExistingAssociatedCase_thenAddReciprocalLinkToAssociatedCase() {

        List<CaseLink> caseLinks = new ArrayList<>();
        caseLinks.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());
        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().associatedCase(caseLinks).build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        capture.getValue().accept(associatedCase2);
        assertEquals("1", associatedCase2.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", associatedCase2.getData().getAssociatedCase().get(1).getValue().getCaseReference());
        assertEquals(YES, associatedCase2.getData().getLinkedCasesBoolean());
    }

    @Test
    public void givenMultipleAssociatedCases_thenAddReciprocalLinkToAllCases() {
        List<SscsCaseDetails> associatedCaseList = new ArrayList<>();
        SscsCaseDetails associatedCase1 = SscsCaseDetails.builder().id(12345678L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase2 = SscsCaseDetails.builder().id(34343434L).data(SscsCaseData.builder().build()).build();
        SscsCaseDetails associatedCase3 = SscsCaseDetails.builder().id(7656765L).data(sscsCaseData).build();

        associatedCaseList.add(associatedCase1);
        associatedCaseList.add(associatedCase2);
        associatedCaseList.add(associatedCase3);

        given(ccdService.findCaseBy(anyString(), anyString(), any())).willReturn(associatedCaseList);

        handler.handle(SUBMITTED, callback);

        verify(updateCcdCaseService, times(2)).updateCaseV2(any(), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), any());
        verify(updateCcdCaseService).updateCaseV2(eq(12345678L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        verify(updateCcdCaseService).updateCaseV2(eq(34343434L), eq(ASSOCIATE_CASE.getCcdType()), eq("Associate case"), eq("Associated case added"), any(), capture.capture());
        capture.getValue().accept(associatedCase3);
        assertEquals("7656765", associatedCase3.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals("7656765", associatedCase3.getData().getAssociatedCase().get(0).getValue().getCaseReference());
        assertEquals(YES, associatedCase3.getData().getLinkedCasesBoolean());
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
