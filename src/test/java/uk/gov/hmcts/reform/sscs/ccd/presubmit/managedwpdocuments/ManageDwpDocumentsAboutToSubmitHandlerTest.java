package uk.gov.hmcts.reform.sscs.ccd.presubmit.managedwpdocuments;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType.*;

import com.google.common.collect.Lists;
import java.util.Optional;
import java.util.stream.Collectors;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class ManageDwpDocumentsAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private ManageDwpDocumentsAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ManageDwpDocumentsAboutToSubmitHandler();

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
    }

    @Test
    @Parameters({
            "DWP_EDITED_EVIDENCE_BUNDLE, You must upload an edited DWP evidence bundle",
            "DWP_EDITED_RESPONSE, You must upload an edited DWP response document",
            "DWP_RESPONSE, DWP response document cannot be empty",
            "DWP_EVIDENCE_BUNDLE, DWP evidence bundle cannot be empty"
    })
    public void shouldHaveErrorWhenDwpDocumentIsNotUploaded(DwpDocumentType documentType, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();
        sscsCaseData.setDwpDocuments(sscsCaseData.getDwpDocuments().stream()
                .filter(dwpDocument -> !dwpDocument.getValue().getDocumentType().equals(documentType.getValue()))
                .collect(Collectors.toList()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
    }

    @Test
    @Parameters({
            "DWP_EDITED_EVIDENCE_BUNDLE, You must upload an edited DWP evidence bundle",
            "DWP_EDITED_RESPONSE, You must upload an edited DWP response document",
            "DWP_RESPONSE, DWP response document cannot be empty",
            "DWP_EVIDENCE_BUNDLE, DWP evidence bundle cannot be empty"
    })
    public void shouldHaveErrorWhenDwpDocumentHasNoDocumentLink(DwpDocumentType documentType, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();
        Optional<DwpDocument> documentOptional = sscsCaseData.getDwpDocuments().stream()
                .filter(dwpDocument -> dwpDocument.getValue().getDocumentType().equals(documentType.getValue()))
                .findFirst();
        documentOptional.ifPresent(document -> document.getValue().setDocumentLink(null));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
    }

    @Test
    @Parameters({
            "DWP_EDITED_EVIDENCE_BUNDLE, Only one DWP evidence bundle should be seen against each case\\, please correct",
            "DWP_EDITED_RESPONSE, Only one DWP response should be seen against each case\\, please correct"
    })
    public void shouldHaveErrorWhenMoreThanOneEditedDwpResponseDocumentsAreUploaded(DwpDocumentType documentType, String expectedErrorMessage) {
        addMandatoryDwpDocuments();
        sscsCaseData.setDwpEditedEvidenceReason("phme");
        addEditedDwpDocuments();
        sscsCaseData.getDwpDocuments().add(newDwpDocument(documentType));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is(expectedErrorMessage));
    }

    @Test
    public void shouldShowErrorsWhenBothMandatoryDwpDocumentsAreMissing() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors().size(), is(2));
        assertThat(response.getErrors().contains("DWP response document cannot be empty"), is(true));
        assertThat(response.getErrors().contains("DWP evidence bundle cannot be empty"), is(true));
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

    private void addEditedDwpDocuments() {
        sscsCaseData.getDwpDocuments().add(newDwpDocument(DWP_EDITED_RESPONSE));
        sscsCaseData.getDwpDocuments().add(newDwpDocument(DWP_EDITED_EVIDENCE_BUNDLE));
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


}
