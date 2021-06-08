package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class ManageDwpDocumentsAboutToStartTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private ManageDwpDocumentsAboutToStartHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private DwpDocumentService dwpDocumentService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ManageDwpDocumentsAboutToStartHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.MANAGE_DWP_DOCUMENTS);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build()).mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({"MANAGE_DWP_DOCUMENTS"})
    public void givenAValidEvent_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);

        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenAnInvalidValidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void willCallMoveDocsToCorrectCollectionOnDwpDocumentService() {
        handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        verify(dwpDocumentService).moveDocsToCorrectCollection(sscsCaseData);
    }



}