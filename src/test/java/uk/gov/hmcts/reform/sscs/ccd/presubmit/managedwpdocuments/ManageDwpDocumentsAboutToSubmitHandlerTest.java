package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_EVIDENCE_BUNDLE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.DWP_RESPONSE;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class ManageDwpDocumentsAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    private ManageDwpDocumentsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Spy
    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        openMocks(this);

        when(callback.getEvent()).thenReturn(EventType.MANAGE_DWP_DOCUMENTS);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .dwpDocuments(Lists.newArrayList())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAnInvalidEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void shouldHaveNoErrorsWhenAllMandatoryDwpDocumentsAreUploaded() {
        addMandatoryDwpDocuments();
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().isEmpty(), is(true));
    }

    @Test
    public void shouldHaveNoErrorsWhenAllMandatoryAndEditedDwpDocumentsAreUploaded() {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().isEmpty(), is(true));
        verify(dwpDocumentService).removeOldDwpDocuments(eq(sscsCaseData));
    }

    @Test
    @Parameters({
        "DWP_EVIDENCE_BUNDLE, true, You must upload an edited DWP evidence bundle",
        "DWP_RESPONSE, true, You must upload an edited DWP response document"
    })
    public void shouldHaveErrorWhenDwpDocumentIsNotUploaded(DwpDocumentType documentType, boolean isEdited, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        if (isEdited) {
            sscsCaseData.setDwpEditedEvidenceReason("phme");
            addEditedDwpDocuments();
            Optional<DwpDocument> documentOptional = sscsCaseData.getDwpDocuments().stream()
                    .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(documentType.getValue()))
                    .findFirst();
            documentOptional.ifPresent(document -> document.getValue().setEditedDocumentLink(null));
        } else {
            sscsCaseData.setDwpDocuments(sscsCaseData.getDwpDocuments().stream()
                    .filter(dwpDocument -> !dwpDocument.getValue().getDocumentType().equals(documentType.getValue()))
                    .collect(Collectors.toList()));
        }
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
    }

    @Test
    @Parameters({
        "DWP_EVIDENCE_BUNDLE, true, You must upload an edited DWP evidence bundle",
        "DWP_RESPONSE, true, You must upload an edited DWP response document"
    })
    public void shouldHaveErrorWhenDwpDocumentHasNoDocumentLink(DwpDocumentType documentType, boolean isEdited, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();
        Optional<DwpDocument> documentOptional = sscsCaseData.getDwpDocuments().stream()
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(documentType.getValue()))
                .findFirst();
        if (isEdited) {
            documentOptional.ifPresent(document -> document.getValue().setEditedDocumentLink(null));
        } else {
            documentOptional.ifPresent(document -> document.getValue().setDocumentLink(null));
        }

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
    }

    @Test
    @Parameters({
        "DWP_EVIDENCE_BUNDLE, true, Only one edited DWP evidence bundle should be seen against each case\\, please correct",
        "DWP_EVIDENCE_BUNDLE, false, Only one DWP evidence bundle should be seen against each case\\, please correct",
        "DWP_RESPONSE, false, Only one DWP response should be seen against each case\\, please correct",
        "DWP_RESPONSE, true, Only one edited DWP response should be seen against each case\\, please correct"
    })
    public void shouldHaveErrorWhenMoreThanOneEditedDwpResponseDocumentsAreUploaded(DwpDocumentType documentType, boolean isEdited, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();
        if (isEdited) {
            sscsCaseData.getDwpDocuments().add(newEditedDwpDocument(documentType));
        } else {
            sscsCaseData.getDwpDocuments().add(newDwpDocument(documentType));
        }

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(isEdited ? 2 : 1));
        assertThat(response.getErrors().contains(expectedErrorMessage), is(true));
    }

    @Test
    public void shouldShowErrorsWhenBothEditedDwpDocumentsAreMissing() {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(2));
        assertThat(response.getErrors().contains("You must upload an edited DWP evidence bundle"), is(true));
        assertThat(response.getErrors().contains("You must upload an edited DWP response document"), is(true));
    }

    @Test
    public void givenADwpDocumentPmheEditedReasonInChildSupportCaseThenErrorAdded() {
        addMandatoryDwpDocuments();
        addEditedDwpDocuments();

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder()
                .code(Benefit.CHILD_SUPPORT.getShortName())
                .description(Benefit.CHILD_SUPPORT.getDescription()).build());

        sscsCaseData.getDwpDocuments().get(0).getValue().setDwpEditedEvidenceReason("phme");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(),
                is("Potential harmful evidence is not a valid selection for child support cases"));
    }

    @Test
    public void givenADwpDocumentChildSupConfEditedReasonInNonChildSupportCaseThenErrorAdded() {
        addMandatoryDwpDocuments();
        addEditedDwpDocuments();

        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder()
                .code(Benefit.PENSION_CREDIT.getShortName())
                .description(Benefit.PENSION_CREDIT.getDescription()).build());

        sscsCaseData.getDwpDocuments().get(0).getValue().setDwpEditedEvidenceReason("childSupportConfidentiality");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().iterator().next(),
                is("Child support - Confidentiality is not a valid selection for this case"));
    }

    private void addEditedDwpDocuments() {
        editedDwpDocuments(sscsCaseData.getDwpDocuments(), DWP_RESPONSE);
        editedDwpDocuments(sscsCaseData.getDwpDocuments(), DWP_EVIDENCE_BUNDLE);
    }

    private void editedDwpDocuments(List<DwpDocument> dwpDocuments, DwpDocumentType documentType) {
        dwpDocuments.stream()
                .filter(doc -> doc.getValue().getDocumentType().equals(documentType.getValue()))
                .findFirst()
                .ifPresent(doc -> doc.getValue().setEditedDocumentLink(DocumentLink.builder().documentUrl("docUrl").build()));
    }

    private void addMandatoryDwpDocuments() {
        sscsCaseData.getDwpDocuments().add(newDwpDocument(DWP_RESPONSE));
        sscsCaseData.getDwpDocuments().add(newDwpDocument(DWP_EVIDENCE_BUNDLE));
    }


    private DwpDocument newDwpDocument(DwpDocumentType dwpDocumentType) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType(dwpDocumentType.getValue())
                .documentLink(DocumentLink.builder().documentUrl("docUrl").build())
                .build())
                .build();
    }

    private DwpDocument newEditedDwpDocument(DwpDocumentType dwpDocumentType) {
        return DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentType(dwpDocumentType.getValue())
                .editedDocumentLink(DocumentLink.builder().documentUrl("docUrl").build())
                .build())
                .build();
    }

}
