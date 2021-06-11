package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.AT_38;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;

import com.google.common.collect.Lists;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
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

    private DwpDocumentService dwpDocumentService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new ManageDwpDocumentsAboutToStartHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.MANAGE_DWP_DOCUMENTS);

        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList")
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.PIP.getShortName()).build())
                        .mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build())
                .dwpDocuments(Lists.newArrayList(newDwpDocument(DWP_RESPONSE)))
                .build();

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
    public void willMoveDwpResponseDocumentToDwpDocumentsCollection() {
        sscsCaseData.setDwpResponseDocument(newDwpResponseDocument(DWP_RESPONSE));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpResponseDocument(), is(nullValue()));
        assertThat(response.getData().getDwpDocuments().size(), is(2));
        assertThat(response.getData().getDwpDocuments().stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).count(), is(2L));
    }

    @Test
    public void willMoveDwpEvidenceBundleDocumentToDwpDocumentsCollection() {
        sscsCaseData.setDwpEvidenceBundleDocument(newDwpResponseDocument(DWP_EVIDENCE_BUNDLE));
        sscsCaseData.setDwpDocuments(Lists.newArrayList(newDwpDocument(DWP_EVIDENCE_BUNDLE)));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpResponseDocument(), is(nullValue()));
        assertThat(response.getData().getDwpDocuments().size(), is(2));
        assertThat(response.getData().getDwpDocuments().stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).count(), is(2L));
    }

    @Test
    public void willMoveAllDocsFromOldLocationsToDwpCollections() {
        sscsCaseData.setDwpEvidenceBundleDocument(newDwpResponseDocument(DWP_EVIDENCE_BUNDLE));
        sscsCaseData.setDwpResponseDocument(newDwpResponseDocument(DWP_RESPONSE));
        sscsCaseData.setDwpAT38Document(newDwpResponseDocument(AT_38));
        sscsCaseData.setDwpDocuments(Lists.newArrayList(newDwpDocument(DWP_EVIDENCE_BUNDLE), newDwpDocument(DWP_RESPONSE), newDwpDocument(AT_38)));
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpResponseDocument(), is(nullValue()));
        assertThat(response.getData().getDwpDocuments().size(), is(6));
        assertThat(response.getData().getDwpDocuments().stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(DWP_EVIDENCE_BUNDLE.getValue())).count(), is(2L));
        assertThat(response.getData().getDwpDocuments().stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(DWP_RESPONSE.getValue())).count(), is(2L));
        assertThat(response.getData().getDwpDocuments().stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(AT_38.getValue())).count(), is(2L));
    }

    private DwpDocument newDwpDocument(DwpDocumentType dwpDocumentType) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType(dwpDocumentType.getValue())
                .documentLink(DocumentLink.builder().documentFilename(dwpDocumentType.getValue() + ".pdf").documentUrl("docUrl").build())
                .build())
                .build();
    }

    private DwpResponseDocument newDwpResponseDocument(DwpDocumentType dwpDocumentType) {
        return DwpResponseDocument.builder()
                .documentFileName(dwpDocumentType.getValue() + ".pdf")
                .documentLink(DocumentLink.builder().documentFilename(dwpDocumentType.getValue() + ".pdf").documentUrl("docUrl").build())
                .build();
    }

}