package uk.gov.hmcts.reform.sscs.ccd.presubmit.hmctsresponsereviewed;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.APPEAL_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.HMCTS_RESPONSE_REVIEWED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.SelectWhoReviewsCase.REVIEW_BY_TCW;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.hmctsresponsereviewed.HmctsResponseReviewedAboutToSubmitHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.PanelMemberComposition;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.service.PanelCompositionService;
import uk.gov.hmcts.reform.sscs.service.AddNoteService;
import uk.gov.hmcts.reform.sscs.service.DwpDocumentService;

@ExtendWith(MockitoExtension.class)
public class HmctsResponseReviewedAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private PanelCompositionService panelCompService;
    @Mock
    DwpDocumentService dwpDocumentService;
    @Mock
    AddNoteService addNoteService;

    private Callback<SscsCaseData> callback;
    private CaseDetails<SscsCaseData> caseDetails;
    private CaseDetails<SscsCaseData> caseDetailsBefore;
    private SscsCaseData sscsCaseData;
    private SscsCaseData sscsCaseDataBefore;

    private HmctsResponseReviewedAboutToSubmitHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .selectWhoReviewsCase(new DynamicList(
                    new DynamicListItem("reviewByTcw", "Review by TCW"), null))
                .benefitCode("002")
                .issueCode("CC")
                .build();
        sscsCaseDataBefore = SscsCaseData.builder().build();
        caseDetails =
                new CaseDetails<>(1234L, "SSCS", RESPONSE_RECEIVED, sscsCaseData, now(), "Benefit");
        caseDetailsBefore =
                new CaseDetails<>(1234L, "SSCS", RESPONSE_RECEIVED, sscsCaseDataBefore, now(), "Benefit");
        callback = new Callback<>(caseDetails, Optional.of(caseDetailsBefore), HMCTS_RESPONSE_REVIEWED, false);

        handler = new HmctsResponseReviewedAboutToSubmitHandler(dwpDocumentService, panelCompService, addNoteService);
    }

    @Test
    public void givenANonHmctsResponseReviewedEvent_thenReturnFalse() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetailsBefore), APPEAL_RECEIVED, false);

        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @CsvSource({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenHmctsResponseReviewedEventWithNoDwpResponseDate_thenSetCaseCodeAndDefaultDwpResponseDateToToday() {
        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedEventWithDwpResponseDate_thenSetCaseCodeAndUseProvidedDwpResponseDate() {
        callback.getCaseDetails().getCaseData()
                .setDwpResponseDate(LocalDate.now().minusDays(1).toString());

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("002CC", response.getData().getCaseCode());
        assertEquals(LocalDate.now().minusDays(1).toString(), response.getData().getDwpResponseDate());
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyBenefitCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setBenefitCode(null);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Benefit code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithEmptyIssueCode_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode(null);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Issue code cannot be empty", error);
        }
    }

    @Test
    public void givenAHmctsResponseReviewedWithIssueCodeSetToDD_displayAnError() {
        callback.getCaseDetails().getCaseData().setIssueCode("DD");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
        assertEquals("UM", response.getData().getIssueCode());
        assertEquals("001", response.getData().getBenefitCode());
        assertEquals("001UM", response.getData().getCaseCode());
    }

    @Test
    public void givenUcbSelectedAndNoUcbDocument_displayAnError() {
        sscsCaseData.setDwpUcb(YES.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(null);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().iterator().next(), is("Please upload a UCB document"));
    }

    @Test
    public void givenUcbSelectedIsNo_thenTheFieldsAreCleared() {
        sscsCaseData.setDwpUcb(NO.getValue());
        sscsCaseData.setDwpUcbEvidenceDocument(
            DocumentLink.builder().documentUrl("121").documentFilename("1.pdf").build());

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getDwpUcb(), is(YES.getValue()));
        assertThat(sscsCaseData.getDwpUcbEvidenceDocument(), is(nullValue()));
        assertThat(sscsCaseData.getDwpDocuments().size(), is(1));
    }

    @Test
    public void givenIbcaCase_thenTheIbcaOnlyFieldsAreCleared() {
        sscsCaseData.setBenefitCode("093");
        sscsCaseData.setBenefitCodeIbcaOnly("093");
        sscsCaseData.setIssueCodeIbcaOnly("SP");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(sscsCaseData.getBenefitCode(), is("093"));
        assertThat(sscsCaseData.getIssueCode(), is("SP"));
        assertNull(sscsCaseData.getBenefitCodeIbcaOnly());
        assertNull(sscsCaseData.getIssueCodeIbcaOnly());
    }

    @Test
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetailsBefore), APPEAL_RECEIVED, false);

        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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
    public void givenChildSupportCaseAndCaseCodeIsChangedToNonChildSupportCodeAndCaseHasOtherParty_thenShowError() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("022");

        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

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

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(1));
        assertThat(response.getWarnings().iterator().next(),
            is("The benefit code will be changed to a non-child support benefit code"));
    }

    @ParameterizedTest
    @CsvSource({"022", "023", "024", "025", "026", "028"})
    public void givenChildSupportCaseAndCaseCodeIsSetToChildSupportCode_thenNoWarningOrErrorIsShown(
        String childSupportBenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode(childSupportBenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndCaseCodeIsAlreadyANonChildSupportCase_thenShowErrorOrWarning() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode("001");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @Test
    public void givenChildSupportCaseAndReferralReasonPhe_thenErrorIsShown() {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setBenefitCode("022");
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.PHE_REQUEST);
        sscsCaseDataBefore.setBenefitCode("022");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Assertions.assertThat(response.getErrors())
            .hasSize(1)
            .containsOnly("PHE request' is not a valid selection for child support cases");
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsSetToSscs5Code_thenNoErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childBenefit").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("022");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(0));
        assertThat(response.getWarnings().size(), is(0));
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenSscs5CaseAndCaseCodeIsChangedToNonSscs5_thenShowError(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("thirtyHoursFreeChildcare").build());
        sscsCaseData.setBenefitCode("001");
        sscsCaseDataBefore.setBenefitCode(sscs5BenefitCode);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().get());
    }

    @ParameterizedTest
    @CsvSource({"015", "016", "030", "034", "050", "053", "054", "055", "057", "058"})
    public void givenNonSscs5CaseAndCaseCodeIsSetToSscs5Code_thenErrorIsShown(String sscs5BenefitCode) {
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());
        sscsCaseData.setBenefitCode(sscs5BenefitCode);
        sscsCaseDataBefore.setBenefitCode("051");

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getWarnings().size(), is(0));
        assertEquals("Benefit code cannot be changed to the selected code",
            response.getErrors().stream().findFirst().get());
    }

    @Test
    public void shouldResetPanelComposition() {
        handler = new HmctsResponseReviewedAboutToSubmitHandler(dwpDocumentService, panelCompService, addNoteService);
        var panelComposition = new PanelMemberComposition(List.of("84"));
        when(panelCompService.resetPanelCompositionIfStale(sscsCaseData, Optional.of(caseDetailsBefore)))
                .thenReturn(panelComposition);

        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(panelComposition, response.getData().getPanelMemberComposition());
    }

    @ParameterizedTest
    @CsvSource({
        "TIME_EXTENSION",
        "PHE_REQUEST",
        "OVER_300_PAGES",
        "OVER_13_MONTHS",
        "OTHER",
        "NO_RESPONSE_TO_DIRECTION"
    })
    public void ifEventIsResponseReviewed_AddInterlocReferralReasonToNote(InterlocReferralReason value) {
        sscsCaseData.setTempNoteDetail("Here is my note");
        sscsCaseData.setInterlocReferralReason(value);
        sscsCaseData.setSelectWhoReviewsCase(getWhoReviewsCaseDynamicList());
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), HMCTS_RESPONSE_REVIEWED, false);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(addNoteService,times(1)).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData),
                eq(String.format("Referred to interloc for review by judge - %s - Here is my note",
                        value.getDescription())));
    }

    @Test
    public void testInterlocReferralReasonIsNoneAndResponseReviewedEvent_thenNoNoteIsAdded() {
        sscsCaseData.setTempNoteDetail(null);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.NONE);
        sscsCaseData.setSelectWhoReviewsCase(getWhoReviewsCaseDynamicList());
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), HMCTS_RESPONSE_REVIEWED, false);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(addNoteService,times(1))
                .addNote(eq(USER_AUTHORISATION), eq(sscsCaseData), eq(null));
    }

    @Test
    public void testTempNoteFilledIsNullAndResponseReviewedEvent_thenNoteIsAdded() {
        sscsCaseData.setTempNoteDetail(null);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.OVER_300_PAGES);
        sscsCaseData.setSelectWhoReviewsCase(getWhoReviewsCaseDynamicList());
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), HMCTS_RESPONSE_REVIEWED, false);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(addNoteService,times(1)).addNote(eq(USER_AUTHORISATION), eq(sscsCaseData),
                eq("Referred to interloc for review by judge - Over 300 pages"));
    }

    @Test
    public void givenListAssistCase_thenSetIgnoreWarningsToYes() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.LIST_ASSIST);
        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getIgnoreCallbackWarnings());
    }

    @Test
    public void givenGapsCase_thenSetIgnoreWarningsIsNotSet() {
        sscsCaseData.getSchedulingAndListingFields().setHearingRoute(HearingRoute.GAPS);
        var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getIgnoreCallbackWarnings());
    }

    @NotNull
    private static DynamicList getWhoReviewsCaseDynamicList() {
        List<DynamicListItem> listOptions = new ArrayList<>();
        listOptions.add(new DynamicListItem(REVIEW_BY_TCW.getId(), REVIEW_BY_TCW.getLabel()));
        listOptions.add(new DynamicListItem(REVIEW_BY_JUDGE.getId(), REVIEW_BY_JUDGE.getLabel()));
        return new DynamicList(new DynamicListItem("reviewByJudge",null), listOptions);
    }
}
