package uk.gov.hmcts.reform.sscs.ccd.presubmit.directionissued;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REJECT_HEARING_RECORDING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.NONE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.VALID_APPEAL;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.JUDGE;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.SUPER_USER;
import static uk.gov.hmcts.reform.sscs.idam.UserRole.TCW;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DirectionType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExtendedSscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExtensionNextEvent;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HmcHearingType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.PrePostHearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDirectionDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.UserDetails;
import uk.gov.hmcts.reform.sscs.model.dwp.Mapping;
import uk.gov.hmcts.reform.sscs.model.dwp.OfficeMapping;
import uk.gov.hmcts.reform.sscs.reference.data.model.ConfidentialityType;
import uk.gov.hmcts.reform.sscs.service.DwpAddressLookupService;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.ServiceRequestExecutor;

class DirectionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";
    private static final String DUMMY_REGIONAL_CENTER = "dummyRegionalCenter";
    private static final String OTHER_PARTY = "otherParty";

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

    @Mock
    private IdamService idamService;

    @Mock
    private UserDetails userDetails;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    private SscsWelshDocument expectedWelshDocument;

    @Mock
    private PreSubmitCallbackResponse<SscsCaseData> mockedResponse;

    @BeforeEach
    public void setUp() {
        openMocks(this);
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false, true);
        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);
        lenient().when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(userDetails);

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
        when(mockedResponse.getErrors()).thenReturn(emptySet());

        when(dwpAddressLookupService.getDwpRegionalCenterByBenefitTypeAndOffice(anyString(), anyString())).thenReturn(DUMMY_REGIONAL_CENTER);
        when(dwpAddressLookupService.getDefaultDwpMappingByBenefitType(anyString())).thenReturn(Optional.of(OfficeMapping.builder().isDefault(true).mapping(Mapping.builder().ccd("DWP PIP (1)").build()).build()));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"DIRECTION_ISSUED", "DIRECTION_ISSUED_WELSH"})
    void givenAValidHandleAndEventType_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    void willCopyThePreviewFileToTheInterlocDirectionDocumentAndAddFooter() {
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
    void givenDirectionNoticeAlreadyExistsAndThenManuallyUploadANewNotice_thenIssueTheNewDocumentWithFooter() {
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService,35, 42, true, true);
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

    @Test
    void willSetTheWithDwpStateToDirectionActionRequired() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeIsNull_displayAnError() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        assertEquals("Direction Type cannot be empty", response.getErrors().toArray()[0]);
    }

    @SuppressWarnings({"Indentation", "unused"})
    private static Object[] getDirectionNoticeConfidentialMembers() {
        return new Object[]{
            new Object[]{ConfidentialityType.GENERAL.getCode(), false, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), true, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), false, null},
            new Object[]{ConfidentialityType.GENERAL.getCode(), false, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), true, DIRECTION_ACTION_REQUIRED},
            new Object[]{ConfidentialityType.CONFIDENTIAL.getCode(), false, null},
        };
    }

    @ParameterizedTest
    @MethodSource("getDirectionNoticeConfidentialMembers")
    void givenDirectionNoticeCheckFtaStateBasedOnConfidentiality(String confidentialityType, boolean isFtaChosen, DwpState newFtaState) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setConfidentialityType(confidentialityType);
        caseData.setSendDirectionNoticeToFTA(isFtaChosen ? YES : NO);
        caseData.setDirectionDueDate(LocalDate.now().toString());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(newFtaState, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfProvideInformation_setInterlocStateToAwaitingInformationAndDirectionTypeIsNull() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.PROVIDE_INFORMATION.toString()));
        sscsCaseData.setDirectionDueDate("11/07/2025");
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_INFORMATION, response.getData().getInterlocReviewState());
    }

    @Test
    void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION, response.getData(

        ).getInterlocReviewState());
        assertEquals(DirectionType.APPEAL_TO_PROCEED.toString(), response.getData().getDirectionTypeDl().getValue().getCode());
    }

    @Test
    void givenDirectionTypeOfAppealToProceedAndCaseIsPreValidInterloc_willNotReturnValidationErrorsFromExternalService() {
        String errorMessage = "There was an error in the external service";
        when(mockedResponse.getErrors()).thenReturn(Set.of(errorMessage));
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());
    }

    @Test
    void givenDirectionTypeOfAppealToProceedAndCaseIsPostValidInterloc_setInterlocStateToAwaitingAdminActionAndDirectionTypeIsSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        when(caseDetails.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertNull(response.getData().getDateSentToDwp());
        assertEquals(DwpState.DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfAppealToProceed_shouldSetDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().dwpIssuingOffice("DWP PIP (1)").build());
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertNotNull(response.getData().getDateSentToDwp());
        assertEquals(response.getData().getDwpDueDate(), LocalDate.now().plusDays(expectedResponseDays).toString());
        assertEquals(DwpState.DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
        assertEquals(DUMMY_REGIONAL_CENTER, response.getData().getDwpRegionalCentre());
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfAppealToProceedWhenDwpIssuingOfficeIsNull_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().build());
        assertDefaultRegionalCentre(expectedResponseDays);
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfAppealToProceedWhenDwpIssuingOfficeIsEmpty_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
        Appeal appeal = callback.getCaseDetails().getCaseData().getAppeal();
        appeal.setBenefitType(BenefitType.builder().code(benefitType).build());
        appeal.setMrnDetails(MrnDetails.builder().dwpIssuingOffice("").build());
        assertDefaultRegionalCentre(expectedResponseDays);
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfAppealToProceedWhenNoMrnDetails_shouldSetDefaultDwpRegionalCentre(String benefitType, int expectedResponseDays) {
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
        assertEquals(response.getData().getDwpDueDate(), LocalDate.now().plusDays(expectedResponseDays).toString());
        assertEquals(DwpState.DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
        assertEquals(DUMMY_REGIONAL_CENTER, response.getData().getDwpRegionalCentre());
    }

    @Test
    void givenDirectionTypeOfGrantExtension_setDwpStateAndDirectionTypeIsNotSet() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_EXTENSION.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertEquals(DwpState.DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));

        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_LISTING.toString()));

        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, AWAITING_ADMIN_ACTION, DIRECTION_ACTION_REQUIRED, State.RESPONSE_RECEIVED, expectedResponseDays);
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, null, DIRECTION_ACTION_REQUIRED, State.WITH_DWP, expectedResponseDays);
    }

    @Test
    void givenDirectionTypeOfGrantReinstatementAndNotInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.APPEAL_CREATED, response.getData().getState());
        assertEquals(RequestOutcome.GRANTED, response.getData().getReinstatementOutcome());
        assertEquals(DwpState.REINSTATEMENT_GRANTED, response.getData().getDwpState());
        assertNull(response.getData().getInterlocReviewState());
    }

    @Test
    void givenDirectionTypeOfGrantReinstatementAndInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.INTERLOCUTORY_REVIEW_STATE);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, response.getData().getState());
        assertEquals(RequestOutcome.GRANTED, response.getData().getReinstatementOutcome());
        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(DwpState.REINSTATEMENT_GRANTED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfRefuseReinstatementkeepState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, response.getData().getState());
        assertEquals(RequestOutcome.REFUSED, response.getData().getReinstatementOutcome());
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(DwpState.REINSTATEMENT_REFUSED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfGrantReinstatementForWelshCaseDont_setStates() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, response.getData().getState());
        assertEquals(RequestOutcome.IN_PROGRESS, response.getData().getReinstatementOutcome());
        assertEquals(DwpState.LAPSED, response.getData().getDwpState());
        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
    }

    @Test
    void givenDirectionTypeOfRefuseReinstatementForWelshCaseDont_setStates() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setLanguagePreferenceWelsh("yes");


        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_REINSTATEMENT.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, response.getData().getState());
        assertEquals(RequestOutcome.IN_PROGRESS, response.getData().getReinstatementOutcome());
        assertEquals(DwpState.LAPSED, response.getData().getDwpState());
        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
    }

    @Test
    void givenDirectionTypeOfGrantUrgentHearingAndInterlocReview_setState() {

        callback.getCaseDetails().getCaseData().setState(State.INTERLOCUTORY_REVIEW_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingOutcome(RequestOutcome.IN_PROGRESS.getValue());
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.GRANT_URGENT_HEARING.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, response.getData().getState());
        assertEquals(RequestOutcome.GRANTED.getValue(), response.getData().getUrgentHearingOutcome());
        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfRefuseUrgentHearingkeepState() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setPreviousState(State.APPEAL_CREATED);
        callback.getCaseDetails().getCaseData().setInterlocReviewState(null);
        callback.getCaseDetails().getCaseData().setUrgentHearingOutcome(RequestOutcome.IN_PROGRESS.getValue());
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);
        callback.getCaseDetails().getCaseData().setUrgentCase("Yes");

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_URGENT_HEARING.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, response.getData().getState());
        assertEquals(RequestOutcome.REFUSED.getValue(), response.getData().getUrgentHearingOutcome());
        assertEquals(NONE, response.getData().getInterlocReviewState());
        assertTrue("No".equalsIgnoreCase(response.getData().getUrgentCase()));
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfRefuseHearingRecordingRequest_setInterlocReviewStateAndInterlocReferralReason() {

        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_HEARING_RECORDING_REQUEST.toString()));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, response.getData().getState());
        assertEquals(AWAITING_ADMIN_ACTION, response.getData().getInterlocReviewState());
        assertEquals(REJECT_HEARING_RECORDING_REQUEST, response.getData().getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, response.getData().getDwpState());
    }

    @Test
    void givenDirectionTypeOfIssueAndSendToAdmin_setInterlocReviewStateOnly() {
        callback.getCaseDetails().getCaseData().setState(State.DORMANT_APPEAL_STATE);
        callback.getCaseDetails().getCaseData().setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        callback.getCaseDetails().getCaseData().setDwpState(DwpState.LAPSED);

        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.ISSUE_AND_SEND_TO_ADMIN.toString()));
        PreSubmitCallbackResponse<SscsCaseData> res = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(State.DORMANT_APPEAL_STATE, res.getData().getState());
        assertEquals(AWAITING_ADMIN_ACTION, res.getData().getInterlocReviewState());
        assertNull(res.getData().getInterlocReferralReason());
        assertEquals(DIRECTION_ACTION_REQUIRED, res.getData().getDwpState());
    }

    @ParameterizedTest
    @ValueSource(strings = {"file.png", "file.jpg", "file.doc"})
    void givenManuallyUploadedFileIsNotAPdf_thenAddAnErrorToResponse(String filename) {
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
    void givenNoPdfIsUploaded_thenAddAnErrorToResponse() {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        sscsCaseData.setSscsInterlocDirectionDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload a PDF document", error);
        }
    }

    @Test
    void shouldErrorWhenDirectionTypeIsProvideInformationAndNoDueDate() {
        sscsCaseData.setDirectionDueDate(null);
        sscsCaseData.getDirectionTypeDl().setValue(new DynamicListItem(DirectionType.PROVIDE_INFORMATION.toString(), "appeal To Proceed"));
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        assertEquals("Please populate the direction due date", response.getErrors().toArray()[0]);
    }

    @Test
    void givenDecisionIssuedEventAndCaseIsWelsh_SetFieldsAndCallServicesCorrectly() {
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
    void givenDecisionIssuedWelshEvent_SetFieldsAndCallServicesCorrectly() {

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

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenWelshDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToListing_setResponseReceivedStateAndInterlocStateToAwaitingAdminAction(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_LISTING.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED_WELSH);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, AWAITING_ADMIN_ACTION, DIRECTION_ACTION_REQUIRED, State.RESPONSE_RECEIVED, expectedResponseDays);
    }

    @ParameterizedTest
    @CsvSource({"pip, 35", "childSupport, 42"})
    void givenWelshDirectionTypeOfRefuseExtensionAndExtensionNextEventIsSendToValidAppeal_setWithDwpStateAndDoNotSetInterlocState(String benefitType, int expectedResponseDays) {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.REFUSE_EXTENSION.toString()));
        callback.getCaseDetails().getCaseData().setExtensionNextEventDl(new DynamicList(ExtensionNextEvent.SEND_TO_VALID_APPEAL.toString()));
        callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitType).build());

        when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED_WELSH);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertValues(response, null, DIRECTION_ACTION_REQUIRED, State.WITH_DWP, expectedResponseDays);
    }

    @Test
    void givenIssueDirectionNoticeForAppealToProceedForPreValidCase_thenSetNonDigitalToDigitalCase() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
        sscsCaseData.setCreatedInGapsFrom(null);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getCreatedInGapsFrom(), READY_TO_LIST.getId());
    }

    @Test
    void givenIssueDirectionNoticeForAppealToProceedForPreValidCase_thenSetNoDigitalToDigitalCase() {
        callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(DirectionType.APPEAL_TO_PROCEED.toString()));
        when(caseDetails.getState()).thenReturn(State.INCOMPLETE_APPLICATION);
        sscsCaseData.setCreatedInGapsFrom(VALID_APPEAL.getId());

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(response.getData().getCreatedInGapsFrom(), READY_TO_LIST.getId());
    }

    @Test
    void shouldNotClearInterlocReferralReasonIfPostHearingsNotEnabled() {
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION, response.getData().getInterlocReferralReason());
    }

    @Test
    void shouldClearInterlocReferralReason() {
        handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, true, true);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReferralReason());
    }

    private void assertValues(PreSubmitCallbackResponse<SscsCaseData> response, InterlocReviewState interlocReviewState,
                              DwpState dwpState, State state, int expectedResponseDays) {
        assertEquals(interlocReviewState, response.getData().getInterlocReviewState());
        assertEquals(dwpState, response.getData().getDwpState());
        assertEquals(state, response.getData().getState());
        assertEquals("sentToDwp", response.getData().getHmctsDwpState());
        assertEquals(LocalDate.now().toString(), response.getData().getDateSentToDwp());
        assertEquals(LocalDate.now().plusDays(expectedResponseDays).toString(), response.getData().getDwpDueDate());
    }

    @Test
    void givenNoUploadedAndGeneratedDoc_thenReturnError() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(NO);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().contains("You need to upload a PDF document"));
    }

    @Test
    void givenGenerateNoticeIsYes_thenReturnCaseDataPreviewDoc() {
        DocumentLink url = sscsCaseData.getDocumentStaging().getPreviewDocument();

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(url), any(), any(), any(), any(), any(), any());
    }

    @Test
    void givenGenerateNoticeIsSetToNoAndInterlocDocIsNotNull_thenReturnRelevantDocLink() {
        sscsCaseData.getDocumentGeneration().setGenerateNotice(NO);
        assertFalse(sscsCaseData.getDocumentGeneration().getGenerateNotice().toBoolean());

        SscsInterlocDirectionDocument interlocDoc = SscsInterlocDirectionDocument.builder()
            .documentType("Doc type")
            .documentFileName("Doc filename")
            .documentLink(DocumentLink.builder()
                .documentFilename("testingDoc")
                .documentBinaryUrl(DOCUMENT_URL)
                .documentUrl(DOCUMENT_URL)
                .build()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(interlocDoc);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(interlocDoc.getDocumentLink(), response.getData().getSscsInterlocDirectionDocument().getDocumentLink());
    }

    @Test
    void givenGenerateNoticeIsSetToYesAndInterlocDocIsNotNull_thenReturnNull() {
        assertTrue(sscsCaseData.getDocumentGeneration().getGenerateNotice().toBoolean());

        SscsInterlocDirectionDocument interlocDoc = SscsInterlocDirectionDocument.builder()
            .documentType("Doc type")
            .documentFileName("Doc filename")
            .documentLink(DocumentLink.builder()
                .documentFilename("testingDoc")
                .documentBinaryUrl(DOCUMENT_URL)
                .documentUrl(DOCUMENT_URL)
                .build()).build();

        sscsCaseData.setSscsInterlocDirectionDocument(interlocDoc);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getSscsInterlocDirectionDocument());
    }

    @Test
    void willWipeHmcHearingTypeIfSelectNextHmcHearingTypeIsNull() {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(null);
        sscsCaseData.setHmcHearingType(HmcHearingType.SUBSTANTIVE);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
    }

    @Test
    void willWipeHmcHearingTypeIfSelectNextHmcHearingTypeIsNo() {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(NO);
        sscsCaseData.setHmcHearingType(HmcHearingType.SUBSTANTIVE);
        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder().hmcHearingType(HmcHearingType.SUBSTANTIVE).build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(NO, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertNull(response.getData().getAppeal().getHearingOptions().getHmcHearingType());
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    void willSetHmcHearingTypeInHearingOptionsNullIfselectNextHmcHearingTypeIsYes(HmcHearingType hmcHearingType) {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(YES);
        sscsCaseData.setHmcHearingType(hmcHearingType);
        sscsCaseData.getAppeal().setHearingOptions(null);
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertEquals(hmcHearingType, response.getData().getHmcHearingType());
        assertEquals(hmcHearingType, response.getData().getAppeal().getHearingOptions().getHmcHearingType());
        assertEquals(HearingOptions.builder().hmcHearingType(hmcHearingType).build(), response.getData().getAppeal().getHearingOptions());
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"SUBSTANTIVE", "DIRECTION_HEARINGS"})
    void willSetTypeOfHearingInHearingOptionsNotNullIfSelectNextTypeOfHearingIsYes(HmcHearingType hmcHearingType) {
        when(caseDetails.getState()).thenReturn(State.READY_TO_LIST);
        sscsCaseData.getExtendedSscsCaseData().setSelectNextHmcHearingType(YES);
        sscsCaseData.setHmcHearingType(hmcHearingType);
        sscsCaseData.getAppeal().setHearingOptions(HearingOptions.builder().agreeLessNotice("string").build());
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(YES, response.getData().getExtendedSscsCaseData().getSelectNextHmcHearingType());
        assertEquals(hmcHearingType, response.getData().getHmcHearingType());
        assertEquals(hmcHearingType, response.getData().getAppeal().getHearingOptions().getHmcHearingType());
        HearingOptions expectedHearingOptions = HearingOptions.builder().agreeLessNotice("string").hmcHearingType(hmcHearingType).build();
        assertEquals(expectedHearingOptions, response.getData().getAppeal().getHearingOptions());
    }

    @Nested
    class CmOtherPartyConfidentialityFeatureFlagEnabled {
        private final boolean cmOtherPartyConfidentialityFeatureFlag = true;
        private final LocalDateTime testStartDateTime = LocalDateTime.now();

        @ParameterizedTest
        @CsvSource({
            "confidentialityGrantedSendToAdmin,YES,childSupport",
            "confidentialityRefusedSendToAdmin,NO,childSupport",
            "confidentialityGrantedSendToAdmin,YES,UC",
            "confidentialityRefusedSendToAdmin,NO,UC"
        })
        void givenIssueDirectionNotice_andTheSelectedConfidentialityPartyIsAppellant_thenUpdateAppellantConfidentialityOnly(
            final String directionType, final YesNo expectedConfidentialityRequired, final String benefitTypeCode) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitTypeCode).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("appellant", "Appellant (or Appointee)"), null);

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).hasValue(expectedConfidentialityRequired);
            assertThat(response.getData().getAppellant().orElseThrow(AssertionError::new).getConfidentialityRequiredChangedDate()).isAfterOrEqualTo(testStartDateTime);
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin,YES", "confidentialityRefusedSendToAdmin,NO"})
        void givenIssueDirectionNotice_andAppellantAlreadyHasSameConfidentiality_thenDoNotUpdateChangedDate(
            final String directionType, final YesNo existingConfidentiality) {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
            callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(existingConfidentiality);

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("appellant", "Appellant (or Appointee)"), null);
            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).hasValue(existingConfidentiality);
            assertThat(response.getData().getAppellant().orElseThrow(AssertionError::new).getConfidentialityRequiredChangedDate()).isNull();
        }

        @ParameterizedTest
        @CsvSource({
            "confidentialityGrantedSendToAdmin,YES,childSupport",
            "confidentialityRefusedSendToAdmin,NO,childSupport",
            "confidentialityGrantedSendToAdmin,YES,UC",
            "confidentialityRefusedSendToAdmin,NO,UC"
        })
        void givenIssueDirectionNotice_andTheSelectedConfidentialityPartyIsOtherParty_thenUpdateTheSelectedOtherPartyConfidentialityOnly(
            final String directionType, final YesNo expectedConfidentialityRequired, final String benefitTypeCode) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            var selectedPartyId = "666-666-666";

            callback.getCaseDetails()
                .getCaseData()
                .setOtherParties(List.of(buildOtherParty("444-444-444", "Tirke", "Do"),
                    buildOtherParty("555-555-555", "Rubar", "Do"),
                    buildOtherParty(selectedPartyId, "Ozan", "Mo")));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(benefitTypeCode).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem(OTHER_PARTY + selectedPartyId, "xx"), null);

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().roles(List.of(SUPER_USER.getValue())).build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            var otherPartyUpdated = response.getData().getOtherParties().stream().filter(o -> o.getValue().getId().equals(selectedPartyId)).toList();

            assertThat(otherPartyUpdated.size()).isEqualTo(1);
            assertThat(otherPartyUpdated.getFirst().getValue().getConfidentialityRequired()).isEqualTo(expectedConfidentialityRequired);
            assertThat(otherPartyUpdated.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfterOrEqualTo(testStartDateTime);

            response.getData().getOtherParties().stream()
                .filter(o -> !o.getValue().getId().equals(selectedPartyId))
                .forEach(otherParty -> {
                    assertThat(otherParty.getValue().getConfidentialityRequired()).isNull();
                    assertThat(otherParty.getValue().getConfidentialityRequiredChangedDate()).isNull();
                });

            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin,YES", "confidentialityRefusedSendToAdmin,NO"})
        void givenIssueDirectionNotice_andOtherPartyAlreadyHasSameConfidentiality_thenDoNotUpdateChangedDate(
            final String directionType, final YesNo existingConfidentiality) {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            var selectedPartyId = "666-666-666";
            callback.getCaseDetails().getCaseData().setOtherParties(List.of(
                buildOtherParty("555-555-555", "Rubar", "Do"),
                buildOtherParty(selectedPartyId, "Ozan", "Mo", existingConfidentiality)));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem(OTHER_PARTY + selectedPartyId, "xx"), null);
            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            var selected = response.getData().getOtherParties().stream()
                .filter(o -> o.getValue().getId().equals(selectedPartyId)).toList();
            assertThat(selected.size()).isEqualTo(1);
            assertThat(selected.getFirst().getValue().getConfidentialityRequired()).isEqualTo(existingConfidentiality);
            assertThat(selected.getFirst().getValue().getConfidentialityRequiredChangedDate()).isNull();
        }

        @Test
        void givenIssueDirectionNotice_andNoOtherPartyMatchesSelectedId_thenDoNotUpdateAnyOtherParty() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setOtherParties(List.of(
                buildOtherParty("111-111-111", "Alice", "Smith"),
                buildOtherParty("222-222-222", "Bob", "Jones")));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("other_party999-999-999", "Unknown"), null);
            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            response.getData().getOtherParties().forEach(o -> {
                assertThat(o.getValue().getConfidentialityRequired()).isNull();
                assertThat(o.getValue().getConfidentialityRequiredChangedDate()).isNull();
            });
        }

        @Test
        void givenIssueDirectionNotice_andSelectedPartyCodeIsExactlyOtherPartyPrefix_thenDoNotUpdateAnyOtherParty() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setOtherParties(List.of(
                buildOtherParty("111-111-111", "Alice", "Smith"),
                buildOtherParty("", "Blank", "Id")));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem(OTHER_PARTY, "No ID"), null);
            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            response.getData().getOtherParties().forEach(o -> {
                assertThat(o.getValue().getConfidentialityRequired()).isNull();
                assertThat(o.getValue().getConfidentialityRequiredChangedDate()).isNull();
            });
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andThereIsNoSelectedConfidentialityParty_thenDoNotUpdateConfidentiality(final String directionType) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails()
                .getCaseData()
                .setOtherParties(List.of(buildOtherParty("555-555-555", "Rubar", "Do"),
                    buildOtherParty("666-666-666", "Ozan", "Mo")));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(null)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            response.getData().getOtherParties().forEach(otherParty -> {
                assertThat(otherParty.getValue().getConfidentialityRequired()).isNull();
                assertThat(otherParty.getValue().getConfidentialityRequiredChangedDate()).isNull();
            });
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andOneOfOtherPartyIsSelectedAsConfidentialityPartyButOtherPartiesFieldIsEmpty_thenDoNotUpdateConfidentiality(final String directionType) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setOtherParties(List.of());
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("other_party666-666-666", "Other Party Ozan"), null);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getOtherParties().size()).isEqualTo(0);
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andOneOfOtherPartyIsSelectedAsConfidentialityPartyButOtherPartiesFieldValueIsNull_thenDoNotUpdateConfidentiality(final String directionType) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setOtherParties(null);
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("other_party666-666-666", "Other Party Ozan"), null);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getOtherParties()).isNull();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andTheSelectedConfidentialityPartyIdHasWrongPrefix_thenDoNotUpdateConfidentiality(final String directionType) {
            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails()
                .getCaseData()
                .setOtherParties(List.of(buildOtherParty("555-555-555", "Rubar", "Do"), buildOtherParty("666-666-666", "Ozan", "Mo")));

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("wrongPrefix666-666-666", "Other Party Ozan"), null);

            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            response.getData().getOtherParties().forEach(otherParty -> {
                assertThat(otherParty.getValue().getConfidentialityRequired()).isNull();
                assertThat(otherParty.getValue().getConfidentialityRequiredChangedDate()).isNull();
            });
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andBenefitTypeIsNotInConfidentialityTab_thenDoNotApplyConfidentialityDecision(final String directionType) {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("pip").build());
            callback.getCaseDetails().getCaseData().getAppeal().getAppellant().setConfidentialityRequired(null);

            var selectedConfidentialityParty = new DynamicList(new DynamicListItem("appellant", "Appellant (or Appointee)"), null);
            callback.getCaseDetails().getCaseData().setExtendedSscsCaseData(ExtendedSscsCaseData.builder()
                .selectedConfidentialityParty(selectedConfidentialityParty)
                .build());

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEqualTo(Set.of());
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getInterlocReviewState()).isNull();
        }

        @Test
        void givenIssueDirectionNotice_andUserIsNotSuperUser_thenReturnValidationError() {

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEqualTo(Set.of("User not authorised to issue confidentiality decision directions."));
        }

        @Test
        void givenIssueDirectionNotice_andIdamServiceReturnsNullUserDetails_thenReturnValidationError() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(null);

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEqualTo(Set.of("User not authorised to issue confidentiality decision directions."));
        }

        @Test
        void givenIssueDirectionNotice_andUserHasNonMatchingRoles_thenReturnValidationError() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().roles(List.of("caseworker-sscs")).build());

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).isEqualTo(Set.of("User not authorised to issue confidentiality decision directions."));
        }

        @Test
        void givenIssueDirectionNotice_andUserHasTcwRole_thenNoAuthorizationError() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().roles(List.of(TCW.getValue())).build());

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).doesNotContain("User not authorised to issue confidentiality decision directions.");
        }

        @Test
        void givenIssueDirectionNotice_andUserHasJudgeRole_thenNoAuthorizationError() {
            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList("confidentialityGrantedSendToAdmin"));
            callback.getCaseDetails().getCaseData().getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

            when(idamService.getUserDetails(USER_AUTHORISATION)).thenReturn(UserDetails.builder().roles(List.of(JUDGE.getValue())).build());

            final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getErrors()).doesNotContain("User not authorised to issue confidentiality decision directions.");
        }
    }

    @Nested
    class CmOtherPartyConfidentialityFeatureFlagNotEnabled {

        @ParameterizedTest
        @ValueSource(strings = {"confidentialityGrantedSendToAdmin", "confidentialityRefusedSendToAdmin"})
        void givenIssueDirectionNotice_andCmOtherPartyConfidentialityFeatureFlagNotEnabled_thenDoNotUpdateAppellantConfidentiality(String directionType) {
            boolean cmOtherPartyConfidentialityFeatureFlag = false;

            when(callback.getEvent()).thenReturn(EventType.DIRECTION_ISSUED);

            handler = new DirectionIssuedAboutToSubmitHandler(footerService, dwpAddressLookupService, idamService, 35, 42, false,
                cmOtherPartyConfidentialityFeatureFlag);

            callback.getCaseDetails().getCaseData().setDirectionTypeDl(new DynamicList(directionType));

            callback.getCaseDetails()
                .getCaseData()
                .setOtherParties(List.of(buildOtherParty("555-555-555", "Rubar", "Do"),
                    buildOtherParty("666-666-666", "Ozan", "Mo")));

            var response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertThat(response.getData().getOtherParties().size()).isEqualTo(2);
            response.getData().getOtherParties().forEach(otherParty -> {
                assertThat(otherParty.getValue().getConfidentialityRequired()).isNull();
                assertThat(otherParty.getValue().getConfidentialityRequiredChangedDate()).isNull();
            });
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequired).isEmpty();
            assertThat(response.getData().getAppellant()).isPresent().map(Appellant::getConfidentialityRequiredChangedDate).isEmpty();
        }
    }

    private CcdValue<OtherParty> buildOtherParty(final String id, final String firstName, final String lastName) {
        return buildOtherParty(id, firstName, lastName, null);
    }

    private CcdValue<OtherParty> buildOtherParty(final String id, final String firstName, final String lastName, final YesNo confidentialityRequired) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder()
            .id(id)
            .name(Name.builder().firstName(firstName).lastName(lastName).build())
            .confidentialityRequired(confidentialityRequired)
            .build()).build();
    }
}