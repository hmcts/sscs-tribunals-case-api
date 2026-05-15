package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

import static java.time.LocalDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;

@ExtendWith(MockitoExtension.class)
public class DwpUploadResponseMidEventHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private PanelCompositionService panelCompositionService;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private CaseDetails<SscsCaseData> caseDetailsBefore;
    private SscsCaseData sscsCaseData;

    private DwpUploadResponseMidEventHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .benefitCode("002")
                .issueCode("CC")
                .dwpFurtherInfo("Yes")
                .dwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder()
                        .documentUrl("a.pdf").documentFilename("a.pdf").build()).build())
                .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder()
                        .documentUrl("b.pdf").documentFilename("b.pdf").build()).build())
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("taxCredit").build()).build())
                .build();
        SscsCaseData sscsCaseDataBefore = SscsCaseData.builder().build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseData, now(), "Benefit");
        caseDetailsBefore =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, sscsCaseDataBefore, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetailsBefore), DWP_UPLOAD_RESPONSE, false);
        handler = new DwpUploadResponseMidEventHandler(panelCompositionService);
    }

    @Test
    public void givenANonPostponementRequestEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetailsBefore), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    public void givenAPostponementRequest_thenReturnTrue() {
        assertTrue(handler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonPostponementRequestCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void testCaseTaxCreditWithEditedEvidenceReasonIsConfidentialityAppendix12DocHaveDocumentThenReject() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("childSupportConfidentiality");
        callback.getCaseDetails().getCaseData().setAppendix12Doc(DwpResponseDocument.builder()
                .documentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build()).build());
        callback.getCaseDetails().getCaseData().getAppendix12Doc()
                .setDocumentLink(DocumentLink.builder().documentUrl("b.pdf").documentFilename("b.pdf").build());
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
                is(DwpUploadResponseMidEventHandler.APPENDIX_12_DOC_NOT_FOR_SSCS5_CONFIDENTIALITY));
    }

    @Test
    public void testCaseTaxCreditWithEditedEvidenceReasonIsConfidentialityAppendix12DocHaveNoDocumentThenPass() {
        callback.getCaseDetails().getCaseData().setDwpEditedEvidenceReason("childSupportConfidentiality");
        callback.getCaseDetails().getCaseData().setAppendix12Doc(null);
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void testMidEventHandlerOnSscs2_WhenNoOtherPartyIsEnteredThenThrowError() {
        callback.getCaseDetails().getCaseData().setBenefitCode("022");
        callback.getCaseDetails().getCaseData().setIssueCode("CC");
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("Yes");
        callback.getCaseDetails().getCaseData().setOtherParties(List.of());
        callback.getCaseDetails().getCaseData()
                .setAppeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build());
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals("Please provide other party details", response.getErrors().toArray()[0]);
    }

    @Test
    public void shouldReturnErrorIfIbcaBenefitCodeSelectedByNonIbcaCase() {
        callback.getCaseDetails().getCaseData().setIssueCode("CE");
        callback.getCaseDetails().getCaseData().setBenefitCode(IBCA_BENEFIT_CODE);
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Please choose a valid benefit code"));
    }

    @Test
    public void shouldReturnNoErrorIfBenefitIssueCodeIsValid() {
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(empty()));
    }

    @Test
    public void shouldReturnErrorIfBenefitIssueCodeIsNotInUse() {
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(true);
        sscsCaseData.setBenefitCode("032");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("The benefit code selected is not in use"));
    }

    @Test
    public void shouldReturnErrorIfBenefitIssueCodeIsInvalid() {
        when(panelCompositionService.isBenefitIssueCodeValid(any(), any())).thenReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertTrue(response.getErrors().contains("Incorrect benefit/issue code combination"));
    }

    @Test
    void shouldReturnWarningWhenFtaUploadsResponseAndSelectsWantsFurtherInfo() {
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getWarnings().size());
        assertTrue(response.getWarnings().contains("Are you sure you want HMCTS to review the case?"));
    }

    @Test
    void shouldNotReturnWarningWhenFtaUploadsResponseAndDoesNotSelectsWantsFurtherInfo() {
        callback.getCaseDetails().getCaseData().setDwpFurtherInfo("No");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(MID_EVENT, callback, USER_AUTHORISATION);

        assertTrue(response.getWarnings().isEmpty());
    }
}
