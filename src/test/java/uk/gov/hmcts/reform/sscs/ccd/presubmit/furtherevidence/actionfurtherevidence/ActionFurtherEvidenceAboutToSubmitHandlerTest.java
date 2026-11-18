package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.APPELLANT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DWP_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.JOINT_PARTY_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POST_HEARING_OTHER;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REPRESENTATIVE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.FURTHER_EVIDENCE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.DWP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.getLocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.DatedRequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentTabChoice;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.InternalCaseDocumentData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostHearingRequestType;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.domain.WorkAllocationFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActionFurtherEvidenceAboutToSubmitHandlerTest {

    private static final DocumentLink DOC_LINK = DocumentLink.builder().documentUrl("test.com").build();
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO = "No";
    private static final String YES = "Yes";

    private ActionFurtherEvidenceAboutToSubmitHandler actionFurtherEvidenceAboutToSubmitHandler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private FooterService footerService;

    @Mock
    private UserDetailsService userDetailsService;

    private SscsCaseData sscsCaseData;

    private List<ScannedDocument> scannedDocumentList = new ArrayList<>();
    private final BundleAdditionFilenameBuilder bundleAdditionFilenameBuilder = new BundleAdditionFilenameBuilder();

    @BeforeEach
    void setUp() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        when(callback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
        when(callback.isIgnoreWarnings()).thenReturn(true);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("bla.pdf")
                                  .subtype("sscs1")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .scannedDate("2019-06-13T00:00:00.000")
                                  .controlNumber("123")
                                  .build()).build();

        ScannedDocument scannedDocument2 = ScannedDocument.builder()
                                                          .value(ScannedDocumentDetails.builder()
                                                                                       .fileName("bla2.pdf")
                                                                                       .subtype("sscs2")
                                                                                       .url(DocumentLink.builder().documentUrl("www.test2.com").build())
                                                                                       .scannedDate("2019-06-12T00:00:00.000")
                                                                                       .controlNumber("124")
                                                                                       .build())
                                                          .build();

        scannedDocumentList.add(scannedDocument);
        scannedDocumentList.add(scannedDocument2);
        DynamicList furtherEvidenceActionList = buildFurtherEvidenceActionItemListForGivenOption(ISSUE_FURTHER_EVIDENCE.getCode(),
            ISSUE_FURTHER_EVIDENCE.getLabel());

        DynamicListItem value = new DynamicListItem("appellant", "Appellant (or Appointee)");
        DynamicList originalSender = new DynamicList(value, singletonList(value));

        sscsCaseData = SscsCaseData.builder()
                                   .ccdCaseId("1234")
                                   .scannedDocuments(scannedDocumentList)
                                   .furtherEvidenceAction(furtherEvidenceActionList)
                                   .originalSender(originalSender)
                                   .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
                                   .build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertThat(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThat(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    void givenAPostponementRequestWithoutDetails_thenAddAnError() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
            new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                                                                                      .fileName("Testing.jpg").url(DocumentLink.builder()
                                                                                                                               .documentUrl("test1.com").build()).build()).build();


        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors().iterator().next()).isEqualTo(ActionFurtherEvidenceAboutToSubmitHandler.POSTPONEMENT_DETAILS_IS_MANDATORY);
    }

    @Test
    void givenAValidPostponementRequest_thenSscsDocumentTypeIsPostponementRequestAndOriginalSenderIsSetAndNoteIsCreatedAndFlagUnprocessedIsYes() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
            new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                                                                                      .fileName("Testing.jpg").url(DocumentLink.builder()
                                                                                                                               .documentUrl("test.com").build()).build()).build();


        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().postponementRequestDetails("Request Detail Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(DocumentType.POSTPONEMENT_REQUEST.getValue());
        assertThat(sscsDocumentDetail.getOriginalPartySender()).isEqualTo(sscsCaseData.getOriginalSender().getValue().getCode());
        assertThat(sscsCaseData.getPostponementRequest().getUnprocessedPostponementRequest()).isEqualTo(YesNo.YES);
        assertThat(sscsCaseData.getAppealNotePad().getNotesCollection())
            .anyMatch(note -> note.getValue().getNoteDetail().equals("Request Detail Test"));
    }

    @ParameterizedTest
    @DisplayName("Given a valid post hearing application request, "
        + "and review by judge is selected, "
        + "and original sender is DWP, "
        + "then DWP State is updated and types are set")
    @CsvSource({
        "setAsideApplication, SET_ASIDE_REQUESTED, SET_ASIDE",
        "correctionApplication, CORRECTION_REQUESTED, CORRECTION",
        "statementOfReasonsApplication, STATEMENT_OF_REASONS_REQUESTED, STATEMENT_OF_REASONS",
        "libertyToApplyApplication, LIBERTY_TO_APPLY_REQUESTED, LIBERTY_TO_APPLY",
        "permissionToAppealApplication, PERMISSION_TO_APPEAL_REQUESTED, PERMISSION_TO_APPEAL"
    })
    void givenAValidPostHearingApplicationRequest_andReviewByJudgeIsSelected_andOriginalSenderIsDwp_thenDwpStateIsUpdatedAndTypesAreSet(
        String documentType,
        DwpState dwpState,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isEqualTo(dwpState);
        assertThat(response.getData().getPostHearing().getRequestType()).isEqualTo(requestType);
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(documentType);
    }

    @ParameterizedTest
    @DisplayName("Given a valid post hearing application request, "
        + "and review by judge is selected, "
        + "and original sender is NOT DWP, "
        + "then DWP State is NOT updated and types are set")
    @CsvSource({
        "setAsideApplication, SET_ASIDE",
        "correctionApplication, CORRECTION",
        "statementOfReasonsApplication, STATEMENT_OF_REASONS",
        "libertyToApplyApplication, LIBERTY_TO_APPLY",
        "permissionToAppealApplication, PERMISSION_TO_APPEAL"
    })
    void givenAValidPostHearingApplicationRequest_andReviewByJudgeIsSelected_andOriginalSenderIsNotDwp_thenDwpStateIsNotUpdatedAndTypesAreSet(
        String documentType,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isNull();
        assertThat(response.getData().getPostHearing().getRequestType()).isEqualTo(requestType);
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(documentType);
    }

    @ParameterizedTest
    @DisplayName("Given a valid post hearing application request from an FTA user with 'Admin Action X' selected, "
        + "then DWP state and Interloc Review state are updated, "
        + "and post hearing request type and document type are identified as expected.")
    @CsvSource({
        "ADMIN_ACTION_CORRECTION, correctionApplication, CORRECTION_REQUESTED, CORRECTION",
    })
    void givenAValidPostHearingApplicationRequest_andAdminActionIsSelected_thenDwpStateIsUpdatedAndTypesAreSet(
        FurtherEvidenceActionDynamicListItems adminAction,
        String documentType,
        DwpState dwpState,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            adminAction.getCode(),
            adminAction.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isEqualTo(dwpState);
        assertThat(response.getData().getPostHearing().getRequestType()).isEqualTo(requestType);
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(documentType);
    }

    @ParameterizedTest
    @DisplayName("Given a post hearing document is uploaded from an FTA user "
        + "and post hearings flag is disabled, "
        + "then DWP state and post hearing are not updated.")
    @CsvSource({
        "setAsideApplication",
        "correctionApplication",
        "statementOfReasonsApplication",
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    void givenAPostHearingDocumentUpload_andPostHearingsFlagIsDisabled_thenDoesNotUpdatePostHearingOrDwpState(
        String documentType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isNull();
        assertThat(response.getData().getPostHearing().getRequestType()).isNull();
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(documentType);
    }


    @ParameterizedTest
    @DisplayName("Given a post hearing B document is uploaded from an FTA user "
        + "and post hearings B flag is disabled, "
        + "then DWP state and post hearing are not updated.")
    @CsvSource({
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    void givenAPostHearingBDocumentUpload_andPostHearingsBFlagIsDisabled_thenDoesNotUpdatePostHearingOrDwpState(
        String documentType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, false, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isNull();
        assertThat(response.getData().getPostHearing().getRequestType()).isNull();
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(documentType);
    }

    @ParameterizedTest
    @CsvSource({
        "setAsideApplication",
        "statementOfReasonsApplication",
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    void givenAValidPostHearingApplicationWithInvalidFurtherEvidenceAction_thenThrowInvalidActionError(String documentType) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem issueEvidenceAction = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getCode(),
            FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).contains(String.format("'Further Evidence Action' must be set to '%s'",
                SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel()));
    }

    @ParameterizedTest
    @CsvSource({
        "correctionApplication,Admin action correction",
    })
    void givenAValidPostHearingApplicationWithInvalidFurtherEvidenceAction_thenThrowInvalidActionError(String documentType, String actionLabel) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem issueEvidenceAction = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getCode(),
            FurtherEvidenceActionDynamicListItems.ISSUE_FURTHER_EVIDENCE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(issueEvidenceAction);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors()).contains(String.format("'Further Evidence Action' must be set to '%s' or '%s'",
                SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel(), actionLabel));
    }

    @ParameterizedTest
    @CsvSource({
        "setAsideApplication,APPELLANT",
        "setAsideApplication,REPRESENTATIVE",
        "setAsideApplication,DWP",
        "setAsideApplication,HMCTS",
        "correctionApplication,APPELLANT",
        "correctionApplication,REPRESENTATIVE",
        "correctionApplication,DWP",
        "correctionApplication,HMCTS",
        "statementOfReasonsApplication,APPELLANT",
        "statementOfReasonsApplication,REPRESENTATIVE",
        "statementOfReasonsApplication,DWP",
        "statementOfReasonsApplication,HMCTS",
        "libertyToApplyApplication,APPELLANT",
        "libertyToApplyApplication,REPRESENTATIVE",
        "libertyToApplyApplication,DWP",
        "libertyToApplyApplication,HMCTS",
        "permissionToAppealApplication,APPELLANT",
        "permissionToAppealApplication,REPRESENTATIVE",
        "permissionToAppealApplication,DWP",
        "permissionToAppealApplication,HMCTS"
    })
    void givenAValidPostHearingRequestFromParty_thenOriginalSenderSetOnSscsDocument(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(sender.getCode(), sender.getLabel()));

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.com").build();
        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(docLink)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOriginalSender().getValue().getCode()).isEqualTo(sender.getCode());
        assertThat(response.getData().getOriginalSender().getValue().getLabel()).isEqualTo(sender.getLabel());
    }

    @ParameterizedTest
    @CsvSource({
        "setAsideApplication,APPELLANT",
        "setAsideApplication,REPRESENTATIVE",
        "setAsideApplication,DWP",
        "setAsideApplication,HMCTS",
        "correctionApplication,APPELLANT",
        "correctionApplication,REPRESENTATIVE",
        "correctionApplication,DWP",
        "correctionApplication,HMCTS",
        "statementOfReasonsApplication,APPELLANT",
        "statementOfReasonsApplication,REPRESENTATIVE",
        "statementOfReasonsApplication,DWP",
        "statementOfReasonsApplication,HMCTS",
        "libertyToApplyApplication,APPELLANT",
        "libertyToApplyApplication,REPRESENTATIVE",
        "libertyToApplyApplication,DWP",
        "libertyToApplyApplication,HMCTS",
        "permissionToAppealApplication,APPELLANT",
        "permissionToAppealApplication,REPRESENTATIVE",
        "permissionToAppealApplication,DWP",
        "permissionToAppealApplication,HMCTS"
    })
    void givenValidAPostHearingRequestFromParty_thenAddFooterTextToDocument(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(sender.getCode(), sender.getLabel()));

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.com").build();
        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(docLink)
                                                                         .includeInBundle(YES)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).addFooter(scannedDocument.getValue().getUrl(), sender.getDocumentFooter(), null);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(documentType);
    }

    @ParameterizedTest
    @CsvSource({
        "setAsideApplication,APPELLANT",
        "setAsideApplication,REPRESENTATIVE",
        "correctionApplication,APPELLANT",
        "correctionApplication,REPRESENTATIVE",
        "statementOfReasonsApplication,APPELLANT",
        "statementOfReasonsApplication,REPRESENTATIVE",
        "libertyToApplyApplication,APPELLANT",
        "libertyToApplyApplication,REPRESENTATIVE",
        "permissionToAppealApplication,APPELLANT",
        "permissionToAppealApplication,REPRESENTATIVE"
    })
    void givenValidAPostHearingRequestFromPartyWhenIncludeInBundle_thenAddDocumentToBundle(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        when(footerService.getNextBundleAddition(any())).thenReturn("A");
        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(sender.getCode(), sender.getLabel()));

        DocumentLink docLink = DocumentLink.builder().documentUrl("test.com").build();
        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(documentType)
                                                                         .fileName("Test.pdf")
                                                                         .url(docLink)
                                                                         .includeInBundle(YES)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("generateFurtherEvidenceActionListScenarios")
    void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(DynamicList furtherEvidenceActionList,
        DynamicList originalSender,
        String evidenceHandle,
        DocumentType expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        } catch (IllegalStateException e) {
            assertThat(furtherEvidenceActionList == null || originalSender == null).isTrue();
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, response);
        }
    }

    @Test
    void givenAConfidentialCaseWithScannedDocumentsAndEditedDocument_shouldMoveToSscsDocumentsWithEditedDocument() {
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("bla3.pdf")
                                  .subtype("sscs1")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                                  .scannedDate("2020-06-13T00:00:00.000")
                                  .controlNumber("123")
                                  .build()).build();

        scannedDocumentList.add(scannedDocument);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentFileName()).isEqualTo("Appellant evidence received on 13-06-2020");
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo("appellantEvidence");
        assertThat(sscsDocumentDetail.getDocumentLink().getDocumentUrl()).isEqualTo("www.test.com");
        assertThat(sscsDocumentDetail.getEditedDocumentLink().getDocumentUrl()).isEqualTo("www.edited.com");
        assertThat(sscsDocumentDetail.getDocumentDateAdded()).isEqualTo("2020-06-13");
        assertThat(sscsDocumentDetail.getControlNumber()).isEqualTo("123");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued()).isEqualTo(NO);
        assertThat(response.getData().getScannedDocuments()).isNull();
        assertThat(response.getData().getEvidenceHandled()).isEqualTo(YES);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenACaseWithScannedDocumentOfTypeCoversheet_shouldNotMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .type("coversheet")
                                  .fileName("bla.pdf")
                                  .subtype("sscs1")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .scannedDate("2019-06-12T00:00:00.000")
                                  .controlNumber("123")
                                  .includeInBundle(NO)
                                  .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(buildFurtherEvidenceActionItemListForGivenOption(OTHER_DOCUMENT_MANUAL.getCode(),
            OTHER_DOCUMENT_MANUAL.getLabel()));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).isNullOrEmpty();
        assertThat(response.getData().getEvidenceHandled()).isEqualTo(YES);

        if (ignoreWarnings) {
            assertThat(response.getWarnings()).isEmpty();
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertThat(warning).isEqualTo("Coversheet will be ignored, are you happy to proceed?");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void givenACaseWithAnEmptyScannedDocumentType_shouldMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .type(null)
                                  .fileName("bla.pdf")
                                  .subtype("sscs1")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .scannedDate("2019-06-12T00:00:00.000")
                                  .controlNumber("123")
                                  .includeInBundle(NO)
                                  .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(
            buildFurtherEvidenceActionItemListForGivenOption(OTHER_DOCUMENT_MANUAL.getCode(),
                OTHER_DOCUMENT_MANUAL.getLabel()));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(ignoreWarnings);

        PreSubmitCallbackResponse<SscsCaseData> response =
            actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getEvidenceHandled()).isEqualTo(YES);

        if (ignoreWarnings) {
            assertThat(response.getWarnings()).isEmpty();
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertThat(warning).isEqualTo("Document type is empty, are you happy to proceed?");
        }
    }

    @Test
    void edgeCaseWhereEvidenceIsSentToBulkPrintSinceNoFurtherActionIsSelected() {
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                                  .fileName("bla.pdf")
                                  .subtype("sscs1")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .scannedDate("2019-06-12T00:00:00.000")
                                  .controlNumber("123")
                                  .includeInBundle(NO)
                                  .build()).build();
        scannedDocumentList = new ArrayList<>();
        scannedDocumentList.add(scannedDocument);
        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setFurtherEvidenceAction(
            buildFurtherEvidenceActionItemListForGivenOption(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.setOriginalSender(buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)"));
        sscsCaseData.setEvidenceHandled(NO);

        when(callback.isIgnoreWarnings()).thenReturn(false);

        PreSubmitCallbackResponse<SscsCaseData> response =
            actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getEvidenceIssued()).isEqualTo(NO);
        assertThat(response.getData().getEvidenceHandled()).isEqualTo(YES);
        assertThat(response.getWarnings()).isEmpty();
    }

    private void assertHappyPaths(DocumentType expectedDocumentType,
        PreSubmitCallbackResponse<SscsCaseData> response) {

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentFileName()).isEqualTo((expectedDocumentType.getLabel() != null ? expectedDocumentType.getLabel() : expectedDocumentType.getValue()) + " received on 13-06-2019");
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(expectedDocumentType.getValue());
        assertThat(sscsDocumentDetail.getDocumentLink().getDocumentUrl()).isEqualTo("www.test.com");
        assertThat(sscsDocumentDetail.getDocumentDateAdded()).isEqualTo("2019-06-13");
        assertThat(sscsDocumentDetail.getControlNumber()).isEqualTo("123");
        String expectedEvidenceIssued = ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED.contains(response.getData().getFurtherEvidenceAction().getValue().getCode()) ? YES : NO;
        assertThat(response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued()).isEqualTo(expectedEvidenceIssued);
        final String expectedDateIssued = ISSUE_FURTHER_EVIDENCE.getCode().equals(response.getData().getFurtherEvidenceAction().getValue().getCode())
            ? getLocalDate().format(DateTimeFormatter.ISO_DATE) : null;
        assertThat(response.getData().getSscsDocument())
            .extracting(document -> document.getValue().getDocumentDateIssued())
            .containsOnly(expectedDateIssued);
        assertThat(response.getData().getScannedDocuments()).isNull();
        assertThat(response.getData().getEvidenceHandled()).isEqualTo(YES);
    }

    private static Stream<Arguments> generateFurtherEvidenceActionListScenarios() {
        DynamicList furtherEvidenceActionListOtherDocuments =
            buildFurtherEvidenceActionItemListForGivenOption("otherDocumentManual",
                "Other document type - action manually");

        DynamicList furtherEvidenceActionListInterloc =
            buildFurtherEvidenceActionItemListForGivenOption("informationReceivedForInterlocJudge",
                "Information received for interlocutory review");

        DynamicList furtherEvidenceActionListIssueParties =
            buildFurtherEvidenceActionItemListForGivenOption("issueFurtherEvidence",
                "Issue further evidence to all parties");

        DynamicList appellantOriginalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");
        DynamicList representativeOriginalSender = buildOriginalSenderItemListForGivenOption("representative",
            "Representative");
        DynamicList dwpOriginalSender = buildOriginalSenderItemListForGivenOption("dwp",
            "Fta");
        DynamicList jointPartyOriginalSender = buildOriginalSenderItemListForGivenOption("jointParty",
            "Joint Party");

        return Stream.of(
            //other options scenarios
            Arguments.of(furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, appellantOriginalSender, NO, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, appellantOriginalSender, YES, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, dwpOriginalSender, NO, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, dwpOriginalSender, null, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListOtherDocuments, dwpOriginalSender, YES, DWP_EVIDENCE),
            //issue parties scenarios
            Arguments.of(furtherEvidenceActionListIssueParties, appellantOriginalSender, null, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, appellantOriginalSender, NO, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, appellantOriginalSender, YES, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, dwpOriginalSender, NO, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, dwpOriginalSender, YES, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, dwpOriginalSender, null, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE),
            Arguments.of(furtherEvidenceActionListIssueParties, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE),
            //interloc scenarios
            Arguments.of(furtherEvidenceActionListInterloc, appellantOriginalSender, null, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, appellantOriginalSender, NO, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, appellantOriginalSender, YES, APPELLANT_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, dwpOriginalSender, null, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, dwpOriginalSender, NO, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, dwpOriginalSender, YES, DWP_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE),
            Arguments.of(furtherEvidenceActionListInterloc, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE),
            //edge cases scenarios
            Arguments.of(null, representativeOriginalSender, "", null), //edge case: furtherEvidenceActionOption is null
            Arguments.of(furtherEvidenceActionListIssueParties, null, null, null) //edge case: originalSender is null
        );
    }

    static DynamicList buildOriginalSenderItemListForGivenOption(String code, String label) {
        DynamicListItem value = new DynamicListItem(code, label);
        return new DynamicList(value, singletonList(value));
    }

    private static DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
            singletonList(selectedOption));
    }

    @Test
    void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
                                       .value(SscsDocumentDetails.builder()
                                                                 .documentType("appellantEvidence")
                                                                 .documentFileName("exist.pdf")
                                                                 .build())
                                       .build();
        sscsDocuments.add(doc);

        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Appellant evidence received on 13-06-2019");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo("2019-06-13");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentFileName()).isEqualTo("Appellant evidence received on 12-06-2019");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded()).isEqualTo("2019-06-12");
        assertThat(response.getData().getSscsDocument().get(2).getValue().getDocumentFileName()).isEqualTo("exist.pdf");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getControlNumber()).isNotNull();
        assertThat(response.getData().getScannedDocuments()).isNull();
    }

    @Test
    void givenAWelshCaseWithScannedDocuments_thenSetTranslationStatusToRequired() {
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument doc = SscsDocument.builder()
                                       .value(SscsDocumentDetails.builder()
                                                                 .documentType("appellantEvidence")
                                                                 .documentFileName("exist.pdf")
                                                                 .build())
                                       .build();
        sscsDocuments.add(doc);

        sscsCaseData.setScannedDocuments(scannedDocumentList);
        sscsCaseData.setSscsDocument(sscsDocuments);
        sscsCaseData.setLanguagePreferenceWelsh(YES);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentFileName()).isEqualTo("Appellant evidence received on 12-06-2019");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded()).isEqualTo("2019-06-12");
        assertThat(response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus()).isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentFileName()).isEqualTo("Appellant evidence received on 13-06-2019");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateAdded()).isEqualTo("2019-06-13");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentTranslationStatus()).isEqualTo(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED);

        assertThat(response.getData().getSscsDocument().get(2).getValue().getDocumentFileName()).isEqualTo("exist.pdf");
        assertThat(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus()).isNull();
        assertThat(response.getData().getScannedDocuments()).isNull();
        assertThat(response.getData().getTranslationWorkOutstanding()).isEqualTo(YES);
    }

    @Test
    void givenACaseWithScannedDocumentWithEmptyValues_thenHandleTheDocument() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentLink().getDocumentUrl()).isEqualTo("www.test.com");
    }

    @Test
    void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);

        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("No further evidence to process");
        }
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, State state) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData caseData = SscsCaseData.builder()
                                                .originalSender(dynamicList)
                                                .furtherEvidenceAction(dynamicList)
                                                .state(state)
                                                .scannedDocuments(singletonList(ScannedDocument.builder().build()))
                                                .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
                                                .build();
        CaseDetails<SscsCaseData> details = new CaseDetails<>(123L, "sscs",
            state, caseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(details, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }

    @Test
    void givenIssueFurtherEvidenceWhenStateNotWithDwp_shouldUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callbackData = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(updated.getData().getDwpFurtherEvidenceStates()).isEqualTo(FURTHER_EVIDENCE_RECEIVED);
        assertThat(updated.getData().getDwpState()).isEqualTo(DwpState.FE_RECEIVED);
    }

    @Test
    void givenIssueFurtherEvidenceAndStateIsWithDwp_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callbackData = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(updated.getData().getDwpState()).isNull();
        assertThat(updated.getData().getDwpFurtherEvidenceStates()).isNull();
    }

    @ParameterizedTest
    @MethodSource("generateIssueFurtherEvidenceAddressEmptyScenarios")
    void givenIssueFurtherEvidenceAndEmptyAppellantAddress_shouldReturnAnErrorToUser(Appeal appeal, List<String> parties) {
        Callback<SscsCaseData> callbackData = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        callbackData.getCaseDetails().getCaseData().setAppeal(appeal);
        PreSubmitCallbackResponse<SscsCaseData> result = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        for (String party : parties) {
            String expectedError = "Address details are missing for the " + party + ", please validate or process manually";
            assertThat(result.getErrors()).contains(expectedError);
        }
    }

    private static Stream<Arguments> generateIssueFurtherEvidenceAddressEmptyScenarios() {

        return Stream.of(
            Arguments.of(Appeal.builder().appellant(Appellant.builder().address(Address.builder().build()).build()).build(), List.of("Appellant")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("Line1").build()).build()).build(), List.of("Appellant")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().address(Address.builder().postcode("TS1 2BA").build()).build()).build(), List.of("Appellant")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().address(null).build()).build(), List.of("Appellant")),
            Arguments.of(Appeal.builder().appellant(null).build(), List.of("Appellant")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().isAppointee(YES).build()).build(), List.of("Appointee")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().build()).build()).build(), List.of("Appointee")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(Address.builder().build()).build()).build()).build(), List.of("Appointee")),
            Arguments.of(Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(null).build()).build()).build(), List.of("Appointee")),
            Arguments.of(Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), List.of("Representative")),
            Arguments.of(Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(null).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), List.of("Representative")),
            Arguments.of(Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(null).build()).build(), List.of("Appellant", "Representative"))
        );
    }

    @Test
    void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callbackData = buildCallback(OTHER_DOCUMENT_MANUAL.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(updated.getData().getDwpFurtherEvidenceStates()).isNull();
    }

    @Test
    void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callbackData = buildCallback(null, State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callbackData, USER_AUTHORISATION);

        assertThat(updated.getData().getDwpFurtherEvidenceStates()).isNull();
    }

    @Test
    void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);

        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("No document URL so could not process");
        }
    }

    @Test
    void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DOC_LINK)
                                  .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);

        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("Case is not marked as confidential so cannot upload an edited document");
        }
    }

    @Test
    void givenAConfidentialCaseAndEditedDocumentPopulated_thenDoNotAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DOC_LINK)
                                  .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).isEmpty();

    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" ", "    "})
    void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName(filename).url(DOC_LINK).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors()).hasSize(1);

        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("No document file name so could not process");
        }
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = State.class, names = {"DORMANT_APPEAL_STATE", "VOID_STATE"})
    void shouldReviewByJudgeAndUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        List<ScannedDocument> docs = new ArrayList<>();
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isEqualTo(State.INTERLOCUTORY_REVIEW_STATE);
        assertThat(sscsCaseData.getReinstatementOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getReinstatementRegistered()).isEqualTo(LocalDate.now());
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
    }

    @ParameterizedTest
    @CsvSource({"VALID_APPEAL", "READY_TO_LIST"})
    void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isEqualTo(previousState);
        assertThat(sscsCaseData.getReinstatementOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getReinstatementRegistered()).isEqualTo(LocalDate.now());
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
    }

    @Test
    void skipReinstatementRequestSettingIfWelsh() {

        State previousState = State.VALID_APPEAL;

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);
        sscsCaseData.setLanguagePreferenceWelsh("yes");

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isEqualTo(previousState);
        assertThat(sscsCaseData.getReinstatementOutcome()).isNull();
        assertThat(sscsCaseData.getReinstatementRegistered()).isNull();
        assertThat(sscsCaseData.getInterlocReviewState()).isNull();
        assertThat(sscsCaseData.isTranslationWorkOutstanding()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"VALID_APPEAL", "READY_TO_LIST"})
    void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasUrgentHearingRequestDocument(State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                                  .url(DOC_LINK).includeInBundle(NO).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isEqualTo(previousState);
        assertThat(response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"READY_TO_LIST,1", "RESPONSE_RECEIVED,1", "DORMANT_APPEAL_STATE,1", "HEARING,1", "WITH_DWP,1", "VALID_APPEAL,0", "NOT_LISTABLE,1", "LISTING_ERROR,1", "HANDLING_ERROR,1"})
    void shouldSetBundleAdditionBasedOnPreviousState(State state, int occurrence) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        when(caseDetails.getState()).thenReturn(state);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocumentDetails scannedDocumentDetails = ScannedDocumentDetails.builder()
                                                                              .fileName("filename.pdf")
                                                                              .type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                                                                              .url(DOC_LINK)
                                                                              .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocumentDetails)
                                                         .build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count()).isEqualTo(occurrence);
    }

    @ParameterizedTest
    @CsvSource({"ISSUE_FURTHER_EVIDENCE,No,1", "OTHER_DOCUMENT_MANUAL,Yes,1", "OTHER_DOCUMENT_MANUAL,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,1", "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,Yes,1", "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,No,0",
        "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,Yes,1", "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,No,0",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,Yes,1", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,No,0"})
    void shouldSetBundleAdditionBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocument scannedDocument = ScannedDocument
            .builder()
            .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                                      .url(DOC_LINK).includeInBundle(includeInBundle).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count()).isEqualTo(occurs);
        assertThat(response.getWarnings()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"ADMIN_ACTION_CORRECTION","SEND_TO_INTERLOC_REVIEW_BY_JUDGE"})
    void shouldSetBundleAdditionForCorrectionApplication(FurtherEvidenceActionDynamicListItems actionType) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(caseDetails.getState()).thenReturn(DORMANT_APPEAL_STATE);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocumentDetails scannedDocumentDetails = ScannedDocumentDetails.builder()
                                                                              .fileName("filename.pdf")
                                                                              .type(ScannedDocumentType.CORRECTION_APPLICATION.getValue())
                                                                              .url(DOC_LINK)
                                                                              .includeInBundle("Yes")
                                                                              .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocumentDetails)
                                                         .build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count()).isEqualTo(1);
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoReinstatementRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isNull();
        assertThat(sscsCaseData.getReinstatementOutcome()).isNull();
        assertThat(sscsCaseData.getReinstatementRegistered()).isNull();
        assertThat(sscsCaseData.getInterlocReviewState()).isNull();
    }

    @Test
    void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoUrgentHearingRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getPreviousState()).isNull();
        assertThat(sscsCaseData.getInterlocReviewState()).isNull();
        assertThat(response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count()).isZero();
    }

    @Test
    void givenWhenNotActionManuallyAndHasReinstatementRequestDocumentThenSetReinstateCaseFields() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getReinstatementRegistered()).isEqualTo(LocalDate.now());
        assertThat(sscsCaseData.getReinstatementOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getInterlocReviewState()).isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE);
        assertThat(sscsCaseData.getPreviousState()).isEqualTo(State.INTERLOCUTORY_REVIEW_STATE);
    }

    @ParameterizedTest
    @CsvSource({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFields(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();

        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty()).isNull();
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate()).isEqualTo(LocalDate.now());
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, Yes",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, No",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, Yes",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, No",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, null"
    })
    void givenConfidentialRequestWhenJointPartyExistsAndAlreadyHasConfidentialityFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFieldsAndDisplayWarning(
        FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem,
        String isProgressingViaGaps
    ) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        sscsCaseData.setIsProgressingViaGaps(isProgressingViaGaps);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedRequestOutcome());

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();

        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty()).isEqualTo(createDatedRequestOutcome());
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void givenAnAppellantHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        when(callback.isIgnoreWarnings()).thenReturn(false);
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK).includeInBundle(NO).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getWarnings()).hasSize(1);
        assertThat(response.getWarnings().iterator().next()).isEqualTo("This case has a confidentiality flag, ensure any evidence from the appellant (or appointee) has confidential information redacted");
    }

    @Test
    void givenAJointPartyHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        when(callback.isIgnoreWarnings()).thenReturn(false);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK).includeInBundle(NO).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getWarnings()).hasSize(1);
        assertThat(response.getWarnings().iterator().next()).isEqualTo("This case has a confidentiality flag, ensure any evidence from the joint party has confidential information redacted");
    }

    @Test
    void givenAConfidentialChildSupportCase_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        when(callback.isIgnoreWarnings()).thenReturn(false);
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();

        assertThat(response.getWarnings()).hasSize(2);
        assertThat(response.getWarnings().iterator().next()).isEqualTo("This case has a confidentiality flag, ensure any evidence from the appellant (or appointee) has confidential information redacted");
    }

    @Test
    void givenAnAppellantHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void givenAJointPartyHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDoNotDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
    }

    @Test
    void givenANonConfidentialChildSupportCase_thenDoNotDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code("childSupport").build());
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .url(DOC_LINK)
                                  .originalSenderOtherPartyId("1")
                                  .originalSenderOtherPartyName("Other Party")
                                  .build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyId()).isEqualTo("1");
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyName()).isEqualTo("Other Party");
    }

    @ParameterizedTest
    @CsvSource({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderAppellant_thenShowError(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().iterator().next()).isEqualTo("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case");
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant()).isNull();
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty()).isNull();
    }

    @Test
    void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty().getRequestOutcome()).isEqualTo(RequestOutcome.IN_PROGRESS);
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant()).isNull();
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty().getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().iterator().next()).isEqualTo("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case");
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeJointParty()).isNull();
        assertThat(sscsCaseData.getConfidentialityRequestOutcomeAppellant()).isNull();
    }


    @Test
    void givenConfidentialRequestFromRep_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(REPRESENTATIVE.getCode(), REPRESENTATIVE.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(1);
        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("Original sender must be appellant or joint party for a confidential document");
        }
    }

    @Test
    void givenChildSupportWithConfidentialRequestFromOtherPartyDoNotMatchWithDocumentOriginalSender_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(otherParty).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getId(), OTHER_PARTY.getLabel() + " - " + otherParty.getName()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .originalSenderOtherPartyId("otherPartyUnknown")
                                  .originalSenderOtherPartyName("unknown name")
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(2);
        assertThat(response.getErrors()).contains("The PDF evidence does not match the Original Sender selected");
    }

    @Test
    void givenChildSupportWithOtherRequestFromOtherPartyWithDocumentOriginalSenderIsNull_thenDonNotShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(otherParty).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getId(), OTHER_PARTY.getLabel() + " - " + otherParty.getName()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .originalSenderOtherPartyId(null)
                                  .originalSenderOtherPartyName(null)
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenChildSupportWithConfidentialRequestFromOtherPartyDoMatchWithDocumentOriginalSender_thenDontShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue<OtherParty> ccdValue = CcdValue.<OtherParty>builder().value(otherParty).build();
        otherPartyList.add(ccdValue);
        sscsCaseData.setOtherParties(otherPartyList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getId(), OTHER_PARTY.getLabel() + " - " + otherParty.getName()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .originalSenderOtherPartyId("10")
                                  .originalSenderOtherPartyName("John Smith")
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenChildSupportWithOtherRequestWhenOtherPartyIsEmpty_thenDontShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        sscsCaseData.setOtherParties(otherPartyList);
        sscsCaseData.getAppeal().setBenefitType(BenefitType.builder().code(Benefit.CHILD_SUPPORT.getShortName()).build());

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                  .originalSenderOtherPartyId("10")
                                  .originalSenderOtherPartyName("John Smith")
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenConfidentialRequestWithInvalidFurtherEvidenceAction_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).hasSize(1);
        for (String error : response.getErrors()) {
            assertThat(error).isEqualTo("Further evidence action must be 'Send to Interloc - Review by Judge' or 'Information received for Interloc - send to Judge' for a confidential document");
        }
    }

    @ParameterizedTest
    @CsvSource({"APPELLANT", "REPRESENTATIVE", "DWP", "JOINT_PARTY", "HMCTS"})
    void shouldIssueToAllParties_willAddFooterTextToDocument(PartyItemList sender) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(ISSUE_FURTHER_EVIDENCE.code, ISSUE_FURTHER_EVIDENCE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(sender.getCode(), sender.getLabel()));
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CHERISHED.getValue())
                                  .includeInBundle(YES)
                                  .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        verify(footerService).addFooter(eq(scannedDocument.getValue().getUrl()), eq(sender.getDocumentFooter()), eq(null));
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentType()).isEqualTo(sender.getDocumentType().getValue());
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getDocumentDateIssued())
            .isEqualTo(getLocalDate().format(DateTimeFormatter.ISO_DATE));
    }

    @ParameterizedTest
    @CsvSource({
        "ISSUE_FURTHER_EVIDENCE,Yes,1",
        "ISSUE_FURTHER_EVIDENCE,No,1",
        "OTHER_DOCUMENT_MANUAL,Yes,1",
        "OTHER_DOCUMENT_MANUAL,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,1",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0"
    })
    void shouldAddDocumentToBundleBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(caseDetails.getState()).thenReturn(READY_TO_LIST);
        when(footerService.getNextBundleAddition(any())).thenReturn("A");

        ScannedDocument scannedDocument = ScannedDocument
            .builder()
            .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                      .url(DOC_LINK).includeInBundle(includeInBundle).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count()).isEqualTo(occurs);
    }

    @ParameterizedTest
    @CsvSource({
        "otherParty,    1, Other Party",
        "otherPartyRep, 2, Rep Party"
    })
    void findOriginalSenderOtherPartyId(String otherPartyCode, String otherPartyId, String expectedName) {
        ScannedDocument scannedDocument = ScannedDocument
            .builder()
            .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                      .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(
            OtherParty.builder()
                      .id("1")
                      .name(Name.builder().firstName("Other").lastName("Party").build())
                      .rep(Representative.builder()
                                         .id("2")
                                         .name(Name.builder().firstName("Rep").lastName("Party").build())
                                         .hasRepresentative(YES)
                                         .build())
                      .build())));

        DynamicList originalSender = buildOriginalSenderItemListForGivenOption(otherPartyCode + otherPartyId, "Other party " + expectedName);
        sscsCaseData.setOriginalSender(originalSender);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyId()).isEqualTo(otherPartyId);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyName()).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @CsvSource({
        "otherParty,    df0d77d2-3852-40b2-9be6-3383e17aa3ad, Other Party",
        "otherPartyRep, b594060e-4506-4dbf-a6fd-07ceaed5099e, Rep Party"
    })
    void findOriginalSenderOtherPartyIdPreservesHexCharsInUuid(String otherPartyCode, String otherPartyId, String expectedName) {
        ScannedDocument scannedDocument = ScannedDocument
                .builder()
                .value(
                        ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(
                OtherParty.builder()
                        .id("df0d77d2-3852-40b2-9be6-3383e17aa3ad")
                        .name(Name.builder().firstName("Other").lastName("Party").build())
                        .rep(Representative.builder()
                                .id("b594060e-4506-4dbf-a6fd-07ceaed5099e")
                                .name(Name.builder().firstName("Rep").lastName("Party").build())
                                .hasRepresentative(YES)
                                .build())
                        .build())));

        DynamicList originalSender = buildOriginalSenderItemListForGivenOption(otherPartyCode + otherPartyId, "Other party " + expectedName);
        sscsCaseData.setOriginalSender(originalSender);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyId()).isEqualTo(otherPartyId);
        assertThat(response.getData().getSscsDocument().getFirst().getValue().getOriginalSenderOtherPartyName()).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {"ISSUE_FURTHER_EVIDENCE,No,0", "ISSUE_FURTHER_EVIDENCE,Yes,0", "ISSUE_FURTHER_EVIDENCE,null,0",
        "OTHER_DOCUMENT_MANUAL,Yes,0", "OTHER_DOCUMENT_MANUAL,No,0", "OTHER_DOCUMENT_MANUAL,null,1",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,0", "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0", "SEND_TO_INTERLOC_REVIEW_BY_TCW,'',1",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,Yes,0", "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,No,0","SEND_TO_INTERLOC_REVIEW_BY_JUDGE,'',1",
        "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,Yes,0", "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,No,0","INFORMATION_RECEIVED_FOR_INTERLOC_TCW,'',1",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,Yes,0", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,No,0", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,null,1"})
    void shouldSetBundleAdditionWarningBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int warnings) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(actionType.code, actionType.label));

        when(callback.isIgnoreWarnings()).thenReturn(false);
        when(caseDetails.getState()).thenReturn(READY_TO_LIST);

        ScannedDocument scannedDocument = ScannedDocument
            .builder()
            .value(
                ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.OTHER.getValue())
                                      .url(DOC_LINK).includeInBundle(includeInBundle).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();

        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getWarnings()).hasSize(warnings);
        if (warnings == 1) {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertThat(warning).isEqualTo("No documents have been ticked to be added as an addition. These document(s) will NOT be added to the bundle. Are you sure?");
        }
    }

    private DatedRequestOutcome createDatedRequestOutcome() {
        return DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1))
                                  .requestOutcome(RequestOutcome.GRANTED).build();
    }

    @Test
    void isThreadSafe() {

        DynamicList furtherEvidenceActionList =
            buildFurtherEvidenceActionItemListForGivenOption(OTHER_DOCUMENT_MANUAL.getCode(),
                OTHER_DOCUMENT_MANUAL.getLabel());

        DynamicList originalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");

        String evidenceHandle = null;

        int numberOfTasks = 3;

        AtomicBoolean noOverwrite = new AtomicBoolean(true);

        try (final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            for (int i = 0; i < numberOfTasks; i++) {

                String expectedId1 = String.valueOf(i);

                SscsCaseData thisCaseData = SscsCaseData.builder()
                                                        .ccdCaseId(expectedId1)
                                                        .scannedDocuments(scannedDocumentList)
                                                        .furtherEvidenceAction(furtherEvidenceActionList)
                                                        .originalSender(originalSender)
                                                        .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
                                                        .build();

                thisCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
                thisCaseData.setOriginalSender(originalSender);
                thisCaseData.setEvidenceHandled(evidenceHandle);

                final Callback<SscsCaseData> mockCallback = mock();
                final CaseDetails<SscsCaseData> mockCaseDetails = mock();

                when(mockCallback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
                when(mockCallback.getCaseDetails()).thenReturn(mockCaseDetails);
                when(mockCaseDetails.getCaseData()).thenReturn(thisCaseData);

                executor.execute(new ThreadRunnable(i, actionFurtherEvidenceAboutToSubmitHandler, mockCallback, expectedId1, noOverwrite));
            }
        } catch (Exception err) {
            log.error("Error occurred", err);
        }

        assertThat(noOverwrite.get()).isTrue();
    }

    @Test
    void givenACaseWithScannedDocuments_shouldGenerateAMapOfSscsDocuments() throws JsonProcessingException {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false, false);
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("new_test.pdf")
                                  .subtype("sscs1")
                                  .type("reinstatementRequest")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                                  .scannedDate("2020-06-13T00:00:00.000")
                                  .controlNumber("4321")
                                  .build()).build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
                                                                .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        assertThat(addedDocuments)
                                       .as("One document has been added to the case and should be added to added documents.")
                                       .containsOnly(entry("reinstatementRequest", 1));
    }

    @Test
    void givenACaseWithScannedDocumentsSubmittedMultipleTimes_shouldGenerateAMapOfSscsDocumentsWithTheMostRecentDocuments()
        throws JsonProcessingException {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false, false);
        ScannedDocument reinstatementRequestScannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("new_test.pdf")
                                  .subtype("sscs1")
                                  .type("reinstatementRequest")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                                  .scannedDate("2020-06-13T00:00:00.000")
                                  .controlNumber("4321")
                                  .build()).build();

        sscsCaseData.setScannedDocuments(singletonList(reinstatementRequestScannedDocument));

        actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        ScannedDocument confidentialityRequestScannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("new_test2.pdf")
                                  .subtype("sscs1")
                                  .type("confidentialityRequest")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                                  .scannedDate("2020-06-13T00:00:00.000")
                                  .controlNumber("111")
                                  .build()).build();

        sscsCaseData.setScannedDocuments(singletonList(confidentialityRequestScannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
                                                                .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        assertThat(addedDocuments)
                                       .as("Added documents should only contain documents from the current event.")
                                       .containsOnly(entry("confidentialityRequest", 1));
    }

    @Test
    void givenACaseWithNoScannedDocument_shouldClearAddedDocuments() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false, false);

        sscsCaseData.setScannedDocuments(null);
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
                                                                 .addedDocuments("{audioEvidence=1}")
                                                                 .build());

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
                                       .as("Added documents should be reset on each event.")
                                       .isNull();
    }

    @Test
    void givenACaseWithScannedDocumentCoversheet_shouldGenerateAnEmptyMapOfSscsDocuments() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false, false);
        List<ScannedDocument> scannedDocuments = new ArrayList<>();
        scannedDocuments.add(ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                                  .fileName("new_test5.pdf")
                                  .subtype("sscs1")
                                  .type("coversheet")
                                  .url(DocumentLink.builder().documentUrl("www.test.com").build())
                                  .editedUrl(DocumentLink.builder().documentUrl("www.edited.com").build())
                                  .scannedDate("2020-06-13T00:00:00.000")
                                  .controlNumber("43215")
                                  .build()).build());

        sscsCaseData.setScannedDocuments(scannedDocuments);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
                                       .as("Only a coversheet has been attached - this should be ignored.")
                                       .isNull();
    }

    @ParameterizedTest
    @CsvSource({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE, CONFIDENTIALITY_REQUEST"})
    void givenScannedDocumentsAreUploaded_thenUpdateCaseWithListOfScannedDocumentTypes(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem, ScannedDocumentType documentType) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);

        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf")
                                  .type(documentType.getValue())

                                  .url(DOC_LINK).build()).build());
        sscsCaseData.setScannedDocuments(docs);

        ActionFurtherEvidenceAboutToSubmitHandler handler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getWarnings()).isEmpty();

        assertThat(sscsCaseData.getWorkAllocationFields().getScannedDocumentTypes()).isEqualTo(
            singletonList(documentType.getValue()));
    }

    void givenAValidPostHearingOtherRequest_andReviewByJudgeIsSelected_thenDontThrowError() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        sscsCaseData.setState(State.DORMANT_APPEAL_STATE);
        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .type(POST_HEARING_OTHER.getLabel())
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState()).isNull();
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocumentDetail.getDocumentType()).isEqualTo(POST_HEARING_OTHER.getLabel());
    }

    @Test
    void givenInternalTribunalDocument_andFlagIsOn_thenPutInTribunalInternalDocumentsCollection() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, true);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        List<SscsDocument> existingInternalDocuments = new ArrayList<>();
        existingInternalDocuments.add(SscsDocument.builder().id("id-1").value(SscsDocumentDetails.builder().build()).build());
        existingInternalDocuments.add(SscsDocument.builder().id("id-2").value(SscsDocumentDetails.builder().build()).build());
        existingInternalDocuments.add(SscsDocument.builder().id("id-3").value(SscsDocumentDetails.builder().build()).build());

        sscsCaseData.setInternalCaseDocumentData(InternalCaseDocumentData.builder().sscsInternalDocument(existingInternalDocuments).build());
        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .documentTabChoice(DocumentTabChoice.INTERNAL)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();
        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInternalCaseDocumentData().getSscsInternalDocument()).hasSize(4);
        assertThat(response.getData().getSscsDocument()).isNull();
        SscsDocumentDetails sscsDocument = response.getData().getInternalCaseDocumentData().getSscsInternalDocument().getFirst().getValue();
        assertThat(sscsDocument.getDocumentLink()).isEqualTo(scannedDocDetails.getUrl());
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = DocumentTabChoice.class, names = {"REGULAR"})
    void givenNonInternalTribunalDocument_andFlagIsOn_thenPutInDocumentsCollection(DocumentTabChoice option) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, true);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .documentTabChoice(option)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.setInternalCaseDocumentData(InternalCaseDocumentData.builder().build());
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getInternalCaseDocumentData().getSscsInternalDocument()).isNull();
        SscsDocumentDetails sscsDocument = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocument.getDocumentLink()).isEqualTo(scannedDocDetails.getUrl());
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = DocumentTabChoice.class, names = {"INTERNAL", "REGULAR"})
    void givenIsInternalTribunalDocumentAny_andFlagIsOff_thenPutInDocumentsCollection(DocumentTabChoice option) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false, false);

        DynamicListItem sendToInterlocListItem = new DynamicListItem(
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getCode(),
            FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel());

        sscsCaseData.getFurtherEvidenceAction().setValue(sendToInterlocListItem);

        ScannedDocumentDetails scannedDocDetails = ScannedDocumentDetails.builder()
                                                                         .fileName("Test.pdf")
                                                                         .url(DOC_LINK)
                                                                         .documentTabChoice(option)
                                                                         .build();
        ScannedDocument scannedDocument = ScannedDocument.builder()
                                                         .value(scannedDocDetails)
                                                         .build();

        sscsCaseData.setScannedDocuments(singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getSscsDocument()).hasSize(1);
        assertThat(response.getData().getInternalCaseDocumentData()).isNull();
        SscsDocumentDetails sscsDocument = response.getData().getSscsDocument().getFirst().getValue();
        assertThat(sscsDocument.getDocumentLink()).isEqualTo(scannedDocDetails.getUrl());
    }
}
