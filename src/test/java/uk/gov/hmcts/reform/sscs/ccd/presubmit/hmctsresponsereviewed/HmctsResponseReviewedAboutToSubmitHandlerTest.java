package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@RunWith(JUnitParamsRunner.class)
public class HmctsResponseReviewedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private HmctsResponseReviewedAboutToSubmitHandler handler;

    private DwpDocumentService dwpDocumentService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        dwpDocumentService = new DwpDocumentService();
        handler = new HmctsResponseReviewedAboutToSubmitHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .benefitCode("002")
                .issueCode("CC")
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonHmctsResponseReviewedEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithNoDwpResponseDate_thenSetCaseCodeAndDefaultDwpResponseDateToToday() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithDwpResponseDate_thenSetCaseCodeAndUseProvidedDwpResponseDate() {
        callback.getCaseDetails().getCaseData().setDwpResponseDate(LocalDate.now().minusDays(1).toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().minusDays(1).toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be set to the default value of DD", error);
        }
    }

    @Test
    public void givenAUcCaseWithSingleElementSelected_thenSetCaseCodeToUs() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        sscsCaseData.setIssueCode("DD");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("US", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001US", response.getData().getCaseCode());
    }

    @Test
    public void givenAUcCaseWithMultipleElementSelected_thenSetCaseCodeToUm() {
        List<String> elementList = new ArrayList<>();
        elementList.add("testElement");
        elementList.add("testElement2");
        sscsCaseData.setIssueCode("DD");
        sscsCaseData.setElementsDisputedList(elementList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("uc").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(nullValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments(), is(nullValue()));
    }

    @Test
    public void givenUcbSelectedAndUploadedUcbDoc_thenNoErrors() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(DocumentLink.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(YES.getValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(1));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenDwpDocs_thenClearDwpDocFields() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("response.pdf").documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38.pdf").documentBinaryUrl("/binaryurl").documentUrl("/url").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("evidence.pdf").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        DwpDocument dwpEditedResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("editedresponse.pdf").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EDITED_RESPONSE.getValue()).build()).build();
        DwpDocument dwpEditedEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("editedevidence.pdf").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EDITED_EVIDENCE_BUNDLE.getValue()).build()).build();
        DwpDocument dwpAppendix12Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("appendix12.pdf").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.APPENDIX_12.getValue()).build()).build();
        DwpDocument ucbDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("ucb.pdf").documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.UCB.getValue()).build()).build();


        callback.getCaseDetails().getCaseData().setDwpResponseDocument(new DwpResponseDocument(dwpResponseDocument.getValue().getDocumentLink(), dwpResponseDocument.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setDwpAT38Document(new DwpResponseDocument(dwpAt38Document.getValue().getDocumentLink(), dwpAt38Document.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(new DwpResponseDocument(dwpEvidenceDocument.getValue().getDocumentLink(), dwpEvidenceDocument.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setDwpEditedResponseDocument(new DwpResponseDocument(dwpEditedResponseDocument.getValue().getDocumentLink(), dwpEditedResponseDocument.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceBundleDocument(new DwpResponseDocument(dwpEditedEvidenceDocument.getValue().getDocumentLink(), dwpEditedEvidenceDocument.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setAppendix12Doc(new DwpResponseDocument(dwpAppendix12Document.getValue().getDocumentLink(), dwpAppendix12Document.getValue().getDocumentFileName()));
        callback.getCaseDetails().getCaseData().setDwpUcbEvidenceDocument(ucbDocument.getValue().getDocumentLink());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDwpResponseDocument());
        assertNull(response.getData().getDwpAT38Document());
        assertNull(response.getData().getDwpEvidenceBundleDocument());
        assertNull(response.getData().getDwpEditedResponseDocument());
        assertNull(response.getData().getDwpEditedEvidenceBundleDocument());
        assertNull(response.getData().getAppendix12Doc());
        assertNull(response.getData().getDwpUcbEvidenceDocument());
    }


}
