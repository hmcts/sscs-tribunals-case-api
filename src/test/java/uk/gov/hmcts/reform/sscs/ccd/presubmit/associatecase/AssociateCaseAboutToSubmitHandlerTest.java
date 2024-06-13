package uk.gov.hmcts.reform.sscs.ccd.presubmit.associatecase;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseLinkDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.AssociatedCaseLinkHelper;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class AssociateCaseAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private AssociateCaseAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsA;

    @Mock
    private CcdService ccdService;

    @Mock
    private AssociatedCaseLinkHelper associatedCaseLinkHelper;

    @Mock
    private IdamService idamService;

    private IdamTokens idamTokens;

    private SscsCaseData sscsCaseDataA;
    private SscsCaseData sscsCaseDataB;

    private SscsCaseDetails sscsCaseDetailsA;
    private SscsCaseDetails sscsCaseDetailsB;


    @Before
    public void setUp() {
        openMocks(this);
        handler = new AssociateCaseAboutToSubmitHandler(ccdService, associatedCaseLinkHelper, idamService);

        when(callback.getEvent()).thenReturn(EventType.ASSOCIATE_CASE);
        when(callback.getCaseDetails()).thenReturn(caseDetailsA);
        sscsCaseDataA = SscsCaseData.builder().ccdCaseId("1").appeal(Appeal.builder().build()).build();
        sscsCaseDataB = SscsCaseData.builder().ccdCaseId("2").appeal(Appeal.builder().build()).build();
        when(caseDetailsA.getCaseData()).thenReturn(sscsCaseDataA);
        sscsCaseDetailsA = SscsCaseDetails.builder().id(1L).data(sscsCaseDataA).build();
        sscsCaseDetailsB = SscsCaseDetails.builder().id(2L).data(sscsCaseDataB).build();

        when(ccdService.getByCaseId(eq(1L), any())).thenReturn(sscsCaseDetailsA);
        when(ccdService.getByCaseId(eq(2L), any())).thenReturn(sscsCaseDetailsB);

        idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenCaseAWithLinkedCaseB_andCaseBHasNoLink_thenCallAssociatedCaseLinkHelper() {
        List<CaseLink> linkedCase = new ArrayList<>();
        linkedCase.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        sscsCaseDataA.setAssociatedCase(linkedCase);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, times(1)).addLinkToOtherAssociatedCases(any(), eq("1"));
    }

    @Test
    public void givenCaseAWithLinkedCaseB_butCaseBHasLink_thenDoNotCallAssociatedCaseLinkHelper() {
        List<CaseLink> linkedCaseA = new ArrayList<>();
        linkedCaseA.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("2").build()).build());
        sscsCaseDataA.setAssociatedCase(linkedCaseA);

        List<CaseLink> linkedCaseB = new ArrayList<>();
        linkedCaseB.add(CaseLink.builder().value(CaseLinkDetails.builder().caseReference("1").build()).build());
        sscsCaseDataB.setAssociatedCase(linkedCaseB);


        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(associatedCaseLinkHelper, times(0)).addLinkToOtherAssociatedCases(any(), any());
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }


}
