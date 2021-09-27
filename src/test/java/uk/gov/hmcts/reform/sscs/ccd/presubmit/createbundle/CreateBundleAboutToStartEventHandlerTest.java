package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;


@RunWith(JUnitParamsRunner.class)
public class CreateBundleAboutToStartEventHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private CreateBundleAboutToStartEventHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private BundleAudioVideoPdfService bundleAudioVideoPdfService;

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;

    @Mock
    private IdamService idamService;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new CreateBundleAboutToStartEventHandler(serviceRequestExecutor, dwpDocumentService, bundleAudioVideoPdfService, "bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleEnglishEditedConfig", "bundleWelshEditedConfig", idamService);

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList("caseworker-sscs-clerk")));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);

    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenOldPattern_thenNoError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsPattern_thenReturnError() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithOldPattern_thenReturnError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream()
                .findFirst()
                .orElse("");
        assertEquals("The bundle cannot be created as mandatory DWP documents are missing", error);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsPatternForSuperUser_thenReturnWarning() {

        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList("caseworker-sscs-superuser")));

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertEquals("The bundle cannot be created as mandatory DWP documents are missing, do you want to proceed?", warning);
        verifyNoInteractions(serviceRequestExecutor);

    }

    @Test
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithOldPatternForSuperUser_thenReturnWarning() {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList("caseworker-sscs-superuser")));

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertEquals("The bundle cannot be created as mandatory DWP documents are missing, do you want to proceed?", warning);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsPatternForSuperUser_thenReturnWarning() {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList("caseworker-sscs-superuser")));
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertEquals("The bundle cannot be created as mandatory DWP documents are missing, do you want to proceed?", warning);
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenEmptyDwpResponseDocumentLinkWithOldPatternForSuperUser_thenReturnWarning() {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList("caseworker-sscs-superuser")));
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertEquals("The bundle cannot be created as mandatory DWP documents are missing, do you want to proceed?", warning);
        verifyNoInteractions(serviceRequestExecutor);
    }
}
