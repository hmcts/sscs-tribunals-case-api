package uk.gov.hmcts.reform.sscs.ccd.presubmit.associatecase;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;

import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.linkcase.LinkCaseAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class LinkCaseAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private LinkCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsA;

    @Mock
    private CcdService ccdService;

    @Mock
    private IdamService idamService;

    private IdamTokens idamTokens;

    private SscsCaseData sscsCaseDataA;
    private SscsCaseData sscsCaseDataB;
    private SscsCaseData sscsCaseDataC;
    private SscsCaseData sscsCaseDataD;

    private SscsCaseDetails sscsCaseDetailsA;
    private SscsCaseDetails sscsCaseDetailsB;
    private SscsCaseDetails sscsCaseDetailsC;
    private SscsCaseDetails sscsCaseDetailsD;

    @Captor
    private ArgumentCaptor<SscsCaseData> capture;

    @Before
    public void setUp() {
        initMocks(this);
        handler = new LinkCaseAboutToSubmitHandler(ccdService, idamService);

        when(callback.getEvent()).thenReturn(EventType.LINK_A_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetailsA);
        sscsCaseDataA = SscsCaseData.builder().ccdCaseId("1").appeal(Appeal.builder().build()).build();
        sscsCaseDataB = SscsCaseData.builder().ccdCaseId("2").appeal(Appeal.builder().build()).build();
        sscsCaseDataC = SscsCaseData.builder().ccdCaseId("3").appeal(Appeal.builder().build()).build();
        sscsCaseDataD = SscsCaseData.builder().ccdCaseId("4").appeal(Appeal.builder().build()).build();
        when(caseDetailsA.getCaseData()).thenReturn(sscsCaseDataA);
        sscsCaseDetailsA = SscsCaseDetails.builder().id(1L).data(sscsCaseDataA).build();
        sscsCaseDetailsB = SscsCaseDetails.builder().id(2L).data(sscsCaseDataB).build();
        sscsCaseDetailsC = SscsCaseDetails.builder().id(3L).data(sscsCaseDataC).build();
        sscsCaseDetailsD = SscsCaseDetails.builder().id(4L).data(sscsCaseDataD).build();

        when(ccdService.getByCaseId(eq(1L), any())).thenReturn(sscsCaseDetailsA);
        when(ccdService.getByCaseId(eq(2L), any())).thenReturn(sscsCaseDetailsB);
        when(ccdService.getByCaseId(eq(3L), any())).thenReturn(sscsCaseDetailsC);
        when(ccdService.getByCaseId(eq(4L), any())).thenReturn(sscsCaseDetailsD);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenCaseAWithLinkedCaseB_thenLinkCaseBBackToCaseA() {
        List<CaseLink> linkedCase = new ArrayList<>();
        linkedCase.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        sscsCaseDataA.setLinkedCase(linkedCase);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(capture.capture(), eq(2L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(1)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        assertEquals(1, capture.getValue().getLinkedCase().size());
        assertEquals("1", capture.getValue().getLinkedCase().get(0).getValue().getCaseReference());
    }

    @Test
    public void givenCaseBAlreadyLinkedToCaseA_thenDoNotUpdateCaseB() {
        List<CaseLink> linkedCaseA = new ArrayList<>();
        linkedCaseA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        sscsCaseDataA.setLinkedCase(linkedCaseA);

        List<CaseLink> linkedCaseB = new ArrayList<>();
        linkedCaseB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());
        sscsCaseDataB.setLinkedCase(linkedCaseB);

        sscsCaseDetailsB.getData().setLinkedCase(linkedCaseB);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
    }

    @Test
    public void givenCaseAWithLinkedCaseBAndC_thenLinkCaseBAndCaseCBackToCaseA() {
        List<CaseLink> linkedCases = new ArrayList<>();
        linkedCases.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        linkedCases.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());
        sscsCaseDataA.setLinkedCase(linkedCases);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(capture.capture(), eq(2L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(3L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(2)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        assertEquals(2, capture.getAllValues().get(0).getLinkedCase().size());
        assertEquals(2, capture.getAllValues().get(1).getLinkedCase().size());
        assertEquals("1", capture.getAllValues().get(0).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(0).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(1).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(1).getLinkedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void givenCaseAWithLinkedCaseBAndCaseBLinkedToCaseC_thenLinkAllCases() {
        List<CaseLink> linkedCasesA = new ArrayList<>();
        List<CaseLink> linkedCasesB = new ArrayList<>();
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());
        sscsCaseDataA.setLinkedCase(linkedCasesA);
        sscsCaseDataB.setLinkedCase(linkedCasesB);

        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("2", result.getData().getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", result.getData().getLinkedCase().get(1).getValue().getCaseReference());

        verify(ccdService, times(0)).updateCase(capture.capture(), eq(1L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(2L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(3L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(2)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());

        assertEquals(2, capture.getAllValues().get(0).getLinkedCase().size());
        assertEquals(2, capture.getAllValues().get(1).getLinkedCase().size());

        assertEquals("1", capture.getAllValues().get(0).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(0).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(1).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(1).getLinkedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void givenCaseAWithLinkedCaseBAndCaseBAlreadyLinkedToCaseAAndC_thenLinkAllCasesAndDoNotUpdateCaseB() {
        List<CaseLink> linkedCasesA = new ArrayList<>();
        List<CaseLink> linkedCasesB = new ArrayList<>();
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());
        sscsCaseDataA.setLinkedCase(linkedCasesA);
        sscsCaseDataB.setLinkedCase(linkedCasesB);

        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("2", result.getData().getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", result.getData().getLinkedCase().get(1).getValue().getCaseReference());

        verify(ccdService, times(0)).updateCase(capture.capture(), eq(1L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(3L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(1)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());

        assertEquals(2, capture.getAllValues().get(0).getLinkedCase().size());
        assertEquals("1", capture.getAllValues().get(0).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(0).getLinkedCase().get(1).getValue().getCaseReference());
    }

    @Test
    public void givenCaseAWithLinkedCaseBAndCaseBLinkedToCaseCAndCaseCLinkedToCaseD_thenLinkAllCases() {
        List<CaseLink> linkedCasesA = new ArrayList<>();
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("4").build()).build());

        List<CaseLink> linkedCasesB = new ArrayList<>();
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("4").build()).build());

        List<CaseLink> linkedCasesC = new ArrayList<>();
        linkedCasesC.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("4").build()).build());

        sscsCaseDataA.setLinkedCase(linkedCasesA);
        sscsCaseDataB.setLinkedCase(linkedCasesB);
        sscsCaseDataC.setLinkedCase(linkedCasesC);

        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("2", result.getData().getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", result.getData().getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", result.getData().getLinkedCase().get(2).getValue().getCaseReference());

        verify(ccdService, times(0)).updateCase(capture.capture(), eq(1L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(2L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(3L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(4L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(3)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());

        assertEquals(3, capture.getAllValues().get(0).getLinkedCase().size());
        assertEquals(3, capture.getAllValues().get(1).getLinkedCase().size());
        assertEquals(3, capture.getAllValues().get(2).getLinkedCase().size());

        assertEquals("1", capture.getAllValues().get(0).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(0).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", capture.getAllValues().get(0).getLinkedCase().get(2).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(1).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(1).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", capture.getAllValues().get(1).getLinkedCase().get(2).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(2).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(2).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(2).getLinkedCase().get(2).getValue().getCaseReference());
    }

    @Test
    public void givenCaseAWithLinkedCaseBAndCaseBLinkedToCaseCAndCaseCLinkedToCaseD_thenLinkAllCasesTest2() {
        List<CaseLink> linkedCasesA = new ArrayList<>();
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());

        List<CaseLink> linkedCasesB = new ArrayList<>();
        linkedCasesB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());

        List<CaseLink> linkedCasesC = new ArrayList<>();
        linkedCasesC.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("4").build()).build());

        sscsCaseDataA.setLinkedCase(linkedCasesA);
        sscsCaseDataB.setLinkedCase(linkedCasesB);
        sscsCaseDataC.setLinkedCase(linkedCasesC);

        PreSubmitCallbackResponse<SscsCaseData> result = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("2", result.getData().getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", result.getData().getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", result.getData().getLinkedCase().get(2).getValue().getCaseReference());

        verify(ccdService, times(0)).updateCase(capture.capture(), eq(1L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(2L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(3L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService).updateCase(capture.capture(), eq(4L), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());
        verify(ccdService, times(3)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());

        assertEquals(3, capture.getAllValues().get(0).getLinkedCase().size());
        assertEquals(3, capture.getAllValues().get(1).getLinkedCase().size());
        assertEquals(3, capture.getAllValues().get(2).getLinkedCase().size());

        assertEquals("1", capture.getAllValues().get(0).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(0).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", capture.getAllValues().get(0).getLinkedCase().get(2).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(1).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(1).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("4", capture.getAllValues().get(1).getLinkedCase().get(2).getValue().getCaseReference());
        assertEquals("1", capture.getAllValues().get(2).getLinkedCase().get(0).getValue().getCaseReference());
        assertEquals("2", capture.getAllValues().get(2).getLinkedCase().get(1).getValue().getCaseReference());
        assertEquals("3", capture.getAllValues().get(2).getLinkedCase().get(2).getValue().getCaseReference());
    }

    @Test
    public void givenCaseExceedsNumberOfAllowedLinkedCases_thenShowAnError() {
        List<CaseLink> linkedCasesA = new ArrayList<>();
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("3").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("4").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("5").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("6").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("7").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("8").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("9").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("10").build()).build());
        linkedCasesA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("11").build()).build());

        when(ccdService.getByCaseId(any(), any())).thenReturn(sscsCaseDetailsA);

        sscsCaseDataA.setLinkedCase(linkedCasesA);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(ccdService, times(0)).updateCase(any(), any(), eq(CASE_UPDATED.getCcdType()), eq("Case updated"), eq("Linked case added"), any());

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Case cannot be linked as number of linked cases exceeds the limit", error);
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
