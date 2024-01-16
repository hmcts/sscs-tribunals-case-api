package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
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

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    private SscsCaseData sscsCaseDataBefore;

    @Before
    public void setUp() {
        openMocks(this);
        DwpDocumentService dwpDocumentService = new DwpDocumentService();
        handler = new HmctsResponseReviewedAboutToSubmitHandler(dwpDocumentService);

        when(callback.getEvent()).thenReturn(EventType.HMCTS_RESPONSE_REVIEWED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId").appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(
                    new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .benefitCode("002")
                .issueCode("CC")
                .build();

        sscsCaseDataBefore = SscsCaseData.builder().build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getCaseData()).thenReturn(sscsCaseDataBefore);
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
    public void givenHmctsResponseReviewedEventWithNoDwpResponseDate_thenSetCaseCodeAndDefaultDwpResponseDateToToday() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithDwpResponseDate_thenSetCaseCodeAndUseProvidedDwpResponseDate() {
        callback.getCaseDetails().getCaseData().setDwpResponseDate(LocalDate.now().minusDays(1).toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().minusDays(1).toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(
            DocumentLink.builder().documentUrl("121").documentFilename("1.pdf").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(nullValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments(), is(nullValue()));
    }

    @Test
    public void givenUcbSelectedAndUploadedUcbDoc_thenNoErrors() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(
            DocumentLink.builder().documentUrl("11").documentFilename("file.pdf").build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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
    public void givenDwpDocumentIsUpdated_thenDwpCollectionIsUpdated() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("response.pdf")
                    .documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();

        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38.pdf").documentBinaryUrl("/binaryurl")
                    .documentUrl("/url").build()).documentType(DwpDocumentType.AT_38.getValue()).build()).build();

        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("evidence.pdf")
                    .documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();

        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(dwpResponseDocument);
        dwpDocuments.add(dwpAt38Document);
        dwpDocuments.add(dwpEvidenceDocument);

        callback.getCaseDetails().getCaseData().setDwpDocuments(dwpDocuments);

        callback.getCaseDetails().getCaseData().setDwpResponseDocument(new DwpResponseDocument(
            dwpResponseDocument.getValue().getDocumentLink(), dwpResponseDocument.getValue().getDocumentFileName()));

        callback.getCaseDetails().getCaseData().setDwpAT38Document(new DwpResponseDocument(
                DocumentLink.builder().documentUrl("/newurl").documentBinaryUrl("/newbinaryurl")
                    .documentFilename("newfilename.pdf").build(), "newfilename"));

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(
            new DwpResponseDocument(dwpEvidenceDocument.getValue().getDocumentLink(),
                dwpEvidenceDocument.getValue().getDocumentFileName()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String todayDate = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        assertEquals("/newurl",
            response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl());

        assertEquals("/newbinaryurl",
            response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentBinaryUrl());

        assertEquals("AT38 received on " + todayDate + ".pdf",
            response.getData().getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentFilename());

        assertEquals("AT38 received on " + todayDate,
            response.getData().getDwpDocuments().get(1).getValue().getDocumentFileName());

        assertNull(response.getData().getDwpResponseDocument());
        assertNull(response.getData().getDwpAT38Document());
        assertNull(response.getData().getDwpEvidenceBundleDocument());
    }

    @Test
    public void givenNoDwpDocument_thenDwpUploadedCollectionIsUpdated() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("response.pdf")
                    .documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();

        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38.pdf")
                    .documentBinaryUrl("/binaryurl").documentUrl("/url").build())
            .documentType(DwpDocumentType.AT_38.getValue()).build()).build();

        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("evidence.pdf")
                    .documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();

        callback.getCaseDetails().getCaseData().setDwpResponseDocument(
            new DwpResponseDocument(dwpResponseDocument.getValue().getDocumentLink(),
                dwpResponseDocument.getValue().getDocumentFileName()));

        callback.getCaseDetails().getCaseData().setDwpAT38Document(
            new DwpResponseDocument(dwpAt38Document.getValue().getDocumentLink(),
                dwpAt38Document.getValue().getDocumentFileName()));

        callback.getCaseDetails().getCaseData().setDwpEvidenceBundleDocument(
            new DwpResponseDocument(dwpEvidenceDocument.getValue().getDocumentLink(),
                dwpEvidenceDocument.getValue().getDocumentFileName()));

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        String todayDate = java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                    hasProperty("documentLink", allOf(
                            hasProperty("documentUrl", is("/evidenceurl")),
                            hasProperty("documentBinaryUrl", is("/evidencebinaryurl")),
                            hasProperty("documentFilename",
                                is("FTA evidence received on " + todayDate + ".pdf"))
                    ))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                    hasProperty("documentLink", allOf(
                        hasProperty("documentUrl", is("/responseurl")),
                        hasProperty("documentBinaryUrl", is("/responsebinaryurl")),
                        hasProperty("documentFilename",
                            is("FTA response received on " + todayDate + ".pdf"))
                    ))
                ))
        ));

        assertThat(response.getData().getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                    hasProperty("documentLink", allOf(
                        hasProperty("documentUrl", is("/url")),
                        hasProperty("documentBinaryUrl", is("/binaryurl")),
                        hasProperty("documentFilename", is("AT38 received on " + todayDate + ".pdf"))
                    )),
                    hasProperty("documentFileName", is("AT38 received on " + todayDate))
                ))
        ));

        assertNull(response.getData().getDwpResponseDocument());
        assertNull(response.getData().getDwpAT38Document());
        assertNull(response.getData().getDwpEvidenceBundleDocument());
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasOtherParty_thenShowError() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertThat(response.getErrors().iterator().next(),
            is("Benefit code cannot be changed on cases with registered 'Other Party'"));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasNoOtherParty_thenShowWarning() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(),
            is("The benefit code will be changed to a non-child support benefit code"));
    }

    @Test
    @Parameters({"022", "023", "024", "025", "026", "028"})
    public void givenChildSupportCaseAndCaseCodeIsSetToChildSupportCode_thenNoWarningOrErrorIsShown(
        String childSupportBenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(childSupportBenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsAlreadyANonChildSupportCase_thenShowErrorOrWarning() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("001");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndReferralReasonPhe_thenErrorIsShown() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.PHE_REQUEST);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("PHE request' is not a valid selection for child support cases");
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsSetToSscs5Code_thenNoErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childBenefit").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsChangedToNonSscs5_thenShowError(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("thirtyHoursFreeChildcare").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode(sscs5BenefitCode);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().get());
    }

    @Test
    @Parameters({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenNonSscs5CaseAndCaseCodeIsSetToSscs5Code_thenErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("051");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().get());
    }
}
