package uk.gov.hmcts.reform.sscs.ccd.presubmit.createbundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SYSTEM_USER;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;
import uk.gov.hmcts.reform.sscs.service.bundle.BundleAudioVideoPdfService;


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

    @BeforeEach
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new CreateBundleAboutToStartEventHandler(serviceRequestExecutor, dwpDocumentService, bundleAudioVideoPdfService, "bundleUrl.com", "bundleEnglishConfig", "bundleWelshConfig",
                "bundleEnglishEditedConfig", "bundleWelshEditedConfig", idamService);

        when(callback.getEvent()).thenReturn(EventType.CREATE_BUNDLE);
        sscsCaseData = SscsCaseData.builder().createdInGapsFrom("readyToList").appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList(SYSTEM_USER.getValue())));
    }

    @Test
    public void givenAValidEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isTrue();
    }

    @Test
    public void givenANonCreateBundleEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(handler.canHandle(ABOUT_TO_START, callback)).isFalse();
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
        assertThat(error).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing");
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
        assertThat(error).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenOldPattern_thenNoError() {
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
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
        assertThat(error).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing");
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
        assertThat(error).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = { "SUPER_USER", "CTSC_CLERK" })
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithDwpDocumentsPatternForSuperUser_thenReturnWarning(UserRole role) {

        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList(role.getValue())));

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertThat(warning).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing, do you want to proceed?");
        verifyNoInteractions(serviceRequestExecutor);

    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = { "SUPER_USER", "CTSC_CLERK" })
    public void givenEmptyDwpEvidenceBundleDocumentLinkWithOldPatternForSuperUser_thenReturnWarning(UserRole role) {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList(role.getValue())));

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertThat(warning).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing, do you want to proceed?");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = { "SUPER_USER", "CTSC_CLERK" })
    public void givenEmptyDwpResponseDocumentLinkWithDwpDocumentsPatternForSuperUser_thenReturnWarning(UserRole role) {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList(role.getValue())));
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_RESPONSE.getValue()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DWP_EVIDENCE_BUNDLE.getValue()).documentLink(DocumentLink.builder().documentFilename("Testing").build()).build()).build());
        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertThat(warning).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing, do you want to proceed?");
        verifyNoInteractions(serviceRequestExecutor);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = { "SUPER_USER", "CTSC_CLERK" })
    public void givenEmptyDwpResponseDocumentLinkWithOldPatternForSuperUser_thenReturnWarning(UserRole role) {
        when(idamService.getUserDetails(any())).thenReturn(new UserDetails("id", "email@email.com", "first last", "first", "last", Arrays.asList(role.getValue())));
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentFilename("Testing").build()).build());
        callback.getCaseDetails().getCaseData().setDwpResponseDocument(DwpResponseDocument.builder().build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String warning = response.getWarnings().stream()
                .findFirst()
                .orElse("");

        assertThat(warning).isEqualTo("The bundle cannot be created as mandatory FTA documents are missing, do you want to proceed?");
        verifyNoInteractions(serviceRequestExecutor);
    }
}
