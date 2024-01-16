package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REJECT_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import com.google.common.collect.ImmutableSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.dwp.Mapping;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

@RunWith(JUnitParamsRunner.class)
public class DirectionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";
    private static final String DUMMY_REGIONAL_CENTER = "dummyRegionalCenter";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FooterService footerService;

    private DirectionIssuedAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private ServiceRequestExecutor serviceRequestExecutor;

    @Mock
    private DwpAddressLookupService dwpAddressLookupService;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    private SscsWelshDocument expectedWelshDocument;

    @Mock
    private PreSubmitCallbackResponse<SscsCaseData> response;

    @Before
    public void setUp() {
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, serviceRequestExecutor, "https://sscs-bulk-scan.net",
                "/validate", dwpAddressLookupService, 35, 42, false);

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .signedBy("User")
                .signedRole("Judge")
                .build())
            .directionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()))
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now().minusDays(1))
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("directionIssued.pdf")
                    .build())
                .build())
            .sscsDocument(docs)
            .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                            .name(Name.builder().build())
                            .identity(Identity.builder().build())
                            .build())
                    .build()).build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(sscsCaseData.getDocumentStaging().getPreviewDocument().getDocumentFilename())
                        .documentLink(sscsCaseData.getDocumentStaging().getPreviewDocument())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .build()).build();


        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(serviceRequestExecutor.post(eq(callback), eq("https://sscs-bulk-scan.net/validate"))).thenReturn(response);
        when(response.getErrors()).thenReturn(emptySet());

        when(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(anyString(), anyString())).thenReturn(DUMMY_REGIONAL_CENTER);
        when(dwpAddressLookupService.getDefaultDwpMappingByBenefitType(anyString())).thenReturn(Optional.of(OfficeMapping.builder().isDefault(true).mapping(Mapping.builder().ccd("DWP PIP (1)").build()).build()));
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    public void givenAValidHandleAndEventType_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDirectionDocumentAndAddFooter() {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getDocumentStaging().getPreviewDocument());
        assertNull(response.getData().getDocumentGeneration().getSignedBy());
        assertNull(response.getData().getDocumentGeneration().getSignedRole());
        assertNull(response.getData().getDocumentGeneration().getGenerateNotice());
        assertNull(response.getData().getDocumentStaging().getDateAdded());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED.toString(), response.getData().getDirectionTypeDl().getValue().getCode());
    }

    @Test
    public void givenDirectionNoticeAlreadyExistsAndThenManuallyUploadANewNotice_thenIssueTheNewDocumentWithFooter() {
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, serviceRequestExecutor, "https://sscs-bulk-scan.net", "/validate", dwpAddressLookupService, 35, 42, true);
        sscsCaseData.setPrePostHearing(PrePostHearing.PRE);
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .documentFileName("file.pdf")
                .documentLink(DocumentLink.builder().documentFilename("file.pdf").documentUrl(DOCUMENT_URL).build()).build())
                .build();

        SscsInterlocDirectionDocument theDocument = SscsInterlocDirectionDocument.builder()
                .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                .documentFileName("file.pdf")
                .documentLink(DocumentLink.builder().documentFilename("file.pdf").documentUrl(DOCUMENT_URL2).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(theDocument);

        sscsDocuments.add(document1);
        sscsCaseData.setSscsDocument(sscsDocuments);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(theDocument.getDocumentLink()), any(), eq(DocumentType.DIRECTION_NOTICE), any(), eq(theDocument.getDocumentDateAdded()), eq(null), eq(null));
    }

    public void willSetTheWithDwpStateToDirectionActionRequired() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpState(), is(DIRECTION_ACTION_REQUIRED));
    }

    @Test
    public void givenDirectionTypeIsNull_displayAnError() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Direction Type cannot be empty", response.getErrors().toArray()[0]);
    }

    @SuppressWarnings({"Indentation", "unused"})
    private Object[] getDirectionNoticeConfidentialMembers() {
        return new Object[]{
            new Object[]{ConfidentialityType.GENERAL.getCode(), false, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), true, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), false, null},
            new Object[]{ConfidentialityType.GENERAL.getCode(), false, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), true, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), false, null},
        };
    }

    @Test
    @Parameters(method = "getDirectionNoticeConfidentialMembers")
    public void givenDirectionNoticeCheckFtaStateBasedOnConfidentiality(String confidentialityType, boolean isFtaChosen, DwpState newFtaState) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setConfidentialityType(confidentialityType);
        caseData.setSendDirectionNoticeToFTA(isFtaChosen ? YES : NO);
        caseData.setDirectionDueDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getData().getDwpState(), is(newFtaState));
    }

    @Test
    public void givenDirectionTypeOfProvideInformation_setInterlocStateToAwaitingInformationAndDirectionTypeIsNull() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.PROVIDE_INFORMATION.toString()));
        sscsCaseData.setDirectionDueDate("11/07/2025");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_INFORMATION, response.getData().getInterlocReviewState());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION, response.getData(

        ).getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED.toString(), response.getData().getDirectionTypeDl().getValue().getCode());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_willReturnValidationErrorsFromExternalService() {
        String errorMessage = "There was an error in the external service";
        when(response.getErrors()).thenReturn(ImmutableSet.of(errorMessage));
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());
        assertEquals(errorMessage, response.getErrors().iterator().next());
    }

    @Test
    public void givenDirectionTypeOfAppealToProceedAndCaseIsPostValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        when(caseDetails.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDateSentToDwp());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED));
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfAppealToProceed_shouldSetDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().dwpIssuingOffice("DWP PIP (1)").build());
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNotNull(response.getData().getDateSentToDwp());
        assertThat(response.getData().getDwpDueDate(), is(LocalDate.now().plusDays(expectedResponseDays).toString()));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED));
        assertEquals(DUMMY_REGIONAL_CENTER, response.getData().getDwpRegionalCentre());
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfAppealToProceedWhenDwpIssuingOfficeIsNull_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().build());
        assertDefaultRegionalCentre(expectedResponseDays);
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfAppealToProceedWhenDwpIssuingOfficeIsEmpty_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().dwpIssuingOffice("").build());
        assertDefaultRegionalCentre(expectedResponseDays);
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfAppealToProceedWhenNoMrnDetails_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(null);
        assertDefaultRegionalCentre(expectedResponseDays);
    }

    private void assertDefaultRegionalCentre(int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNotNull(response.getData().getDateSentToDwp());
        assertThat(response.getData().getDwpDueDate(), is(LocalDate.now().plusDays(expectedResponseDays).toString()));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED));
        assertEquals(DUMMY_REGIONAL_CENTER, response.getData().getDwpRegionalCentre());
    }

    @Test
    public void givenDirectionTypeOfGrantExtension_setDwpStateAndDirectionTypeIsNotSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_EXTENSION.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED));
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));

        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_LISTING.toString()));

        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, AWAITING_ADMIN_ACTION, DIRECTION_ACTION_REQUIRED, State.RESPONSE_RECEIVED, expectedResponseDays);
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, null, DIRECTION_ACTION_REQUIRED, State.WITH_DWP, expectedResponseDays);
    }

    @Test
    public void givenDirectionTypeOfGrantReinstatementAndNotInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.APPEAL_CREATED));
        assertTrue(response.getData().getReinstatementOutcome().equals(RequestOutcome.GRANTED));
        assertEquals(DwpState.REINSTATEMENT_GRANTED, response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    public void givenDirectionTypeOfGrantReinstatementAndInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.INTERLOCUTORY_REVIEW_STATE));
        assertTrue(response.getData().getReinstatementOutcome().equals(RequestOutcome.GRANTED));
        assertTrue(response.getData().getInterlocReviewState().equals(AWAITING_ADMIN_ACTION));
        assertEquals(DwpState.REINSTATEMENT_GRANTED, response.getData().getDwpState());
    }

    @Test
    public void givenDirectionTypeOfRefuseReinstatementkeepState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.DORMANT_APPEAL_STATE));
        assertTrue(response.getData().getReinstatementOutcome().equals(RequestOutcome.REFUSED));
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(DwpState.REINSTATEMENT_REFUSED, response.getData().getDwpState());
    }

    @Test
    public void givenDirectionTypeOfGrantReinstatementForWelshCaseDont_setStates() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.DORMANT_APPEAL_STATE));
        assertTrue(response.getData().getReinstatementOutcome().equals(RequestOutcome.IN_PROGRESS));
        assertEquals(DwpState.LAPSED, response.getData().getDwpState());
        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
    }

    @Test
    public void givenDirectionTypeOfRefuseReinstatementForWelshCaseDont_setStates() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.DORMANT_APPEAL_STATE));
        assertTrue(response.getData().getReinstatementOutcome().equals(RequestOutcome.IN_PROGRESS));
        assertEquals(DwpState.LAPSED, response.getData().getDwpState());
        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
    }

    @Test
    public void givenDirectionTypeOfGrantUrgentHearingAndInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.INTERLOCUTORY_REVIEW_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingOutcome(RequestOutcome.IN_PROGRESS.getValue());
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_URGENT_HEARING.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.INTERLOCUTORY_REVIEW_STATE));
        assertTrue(response.getData().getUrgentHearingOutcome().equals(RequestOutcome.GRANTED.getValue()));
        assertTrue(response.getData().getInterlocReviewState().equals(AWAITING_ADMIN_ACTION));
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    public void givenDirectionTypeOfRefuseUrgentHearingkeepState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingOutcome(RequestOutcome.IN_PROGRESS.getValue());
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setUrgentCase("Yes");

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_URGENT_HEARING.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.DORMANT_APPEAL_STATE));
        assertTrue(response.getData().getUrgentHearingOutcome().equals(RequestOutcome.REFUSED.getValue()));
        assertTrue(response.getData().getInterlocReviewState().equals(NONE));
        assertTrue("No".equalsIgnoreCase(response.getData().getUrgentCase()));
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    public void givenDirectionTypeOfRefuseHearingRecordingRequest_setInterlocReviewStateAndInterlocReferralReason() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_HEARING_RECORDING_REQUEST.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertTrue(response.getData().getState().equals(State.DORMANT_APPEAL_STATE));
        assertTrue(response.getData().getInterlocReviewState().equals(AWAITING_ADMIN_ACTION));
        assertEquals(REJECT_HEARING_RECORDING_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    @Parameters({"file.png", "file.jpg", "file.doc"})
    public void givenManuallyUploadedFileIsNotAPdf_thenAddAnErrorToResponse(String filename) {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        SscsInterlocDirectionDocument theDocument = SscsInterlocDirectionDocument.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentFilename(filename).documentUrl(DOCUMENT_URL).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(theDocument);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload PDF documents only", error);
        }
    }

    @Test
    public void givenNoPdfIsUploaded_thenAddAnErrorToResponse() {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        sscsCaseData.setSscsInterlocDirectionDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload a PDF document", error);
        }
    }

    @Test
    public void shouldErrorWhenDirectionTypeIsProvideInformationAndNoDueDate() {
        sscsCaseData.setDirectionDueDate(null);
        sscsCaseData.getDirectionTypeDl().setValue(new DynamicListItem(DirectionType.PROVIDE_INFORMATION.toString(), "appeal To Proceed"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        assertEquals("Please populate the direction due date", response.getErrors().toArray()[0]);
    }

    @Test
    public void givenDecisionIssuedEventAndCaseIsWelsh_SetFieldsAndCallServicesCorrectly() {
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
        assertNull(response.getData().getDwpState());
        assertNull(response.getData().getTimeExtensionRequested());
        assertNotNull(response.getData().getDirectionTypeDl());
        assertNotNull(response.getData().getExtensionNextEventDl());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()),
                any(), eq(DocumentType.DIRECTION_NOTICE), any(), any(), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));

        verifyNoInteractions(serviceRequestExecutor);
    }

    @Test
    public void givenDecisionIssuedWelshEvent_SetFieldsAndCallServicesCorrectly() {

        expectedWelshDocument = SscsWelshDocument.builder()
                .value(SscsWelshDocumentDetails.builder()
                        .documentFileName("welshDirectionDocFilename")
                        .documentLink(DocumentLink.builder().documentUrl("welshUrl").documentBinaryUrl("welshBinaryUrl").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .build()).build();
        sscsCaseData.setSscsWelshDocuments(new ArrayList<>());
        sscsCaseData.getSscsWelshDocuments().add(expectedWelshDocument);


        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED_WELSH);
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(response.getData().getInterlocReviewState());
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
        assertEquals("No", response.getData().getTimeExtensionRequested());

        verifyNoInteractions(footerService);
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenWelshDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_LISTING.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED_WELSH);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, AWAITING_ADMIN_ACTION, DIRECTION_ACTION_REQUIRED, State.RESPONSE_RECEIVED, expectedResponseDays);
    }

    @Test
    @Parameters({"pip, 35", "childSupport, 42"})
    public void givenWelshDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED_WELSH);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, null, DIRECTION_ACTION_REQUIRED, State.WITH_DWP, expectedResponseDays);
    }

    @Test
    public void givenIssueDirectionNoticeForAppealToProceedForPreValidCase_thenSetNonDigitalToDigitalCase() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        sscsCaseData.setCreatedInGapsFrom(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
    }

    @Test
    public void givenIssueDirectionNoticeForAppealToProceedForPreValidCase_thenSetNoDigitalToDigitalCase() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        sscsCaseData.setCreatedInGapsFrom(VALID_APPEAL.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getCreatedInGapsFrom(), is(READY_TO_LIST.getId()));
    }

    @Test
    public void shouldNotClearInterlocReferralReasonIfPostHearingsNotEnabled() {
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION, response.getData().getInterlocReferralReason());
    }

    @Test
    public void shouldClearInterlocReferralReason() {
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, serviceRequestExecutor, "https://sscs-bulk-scan.net",
                "/validate", dwpAddressLookupService, 35, 42, true);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReferralReason());
    }

    private void assertValues(PreSubmitCallbackResponse<SscsCaseData> response, InterlocReviewState interlocReviewState,
                              DwpState dwpState, State state, int expectedResponseDays) {
        assertEquals(interlocReviewState, response.getData().getInterlocReviewState());
        assertThat(response.getData().getDwpState(), is(dwpState));
        assertThat(response.getData().getState(), is(state));
        assertThat(response.getData().getHmctsDwpState(), is("sentToDwp"));
        assertThat(response.getData().getDateSentToDwp(), is(LocalDate.now().toString()));
        assertThat(response.getData().getDwpDueDate(), is(LocalDate.now().plusDays(expectedResponseDays).toString()));
    }
}
