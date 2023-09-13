package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.DORMANT_APPEAL_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.ActionFurtherEvidenceAboutToSubmitHandler.FURTHER_EVIDENCE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.APPELLANT;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.DWP;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.JOINT_PARTY;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.REPRESENTATIVE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.ScannedDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;
import uk.gov.hmcts.reform.sscs.service.BundleAdditionFilenameBuilder;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;

@RunWith(JUnitParamsRunner.class)
public class ActionFurtherEvidenceAboutToSubmitHandlerTest {

    public static final DocumentLink DOC_LINK = DocumentLink.builder().documentUrl("test.com").build();
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String NO = "No";
    private static final String YES = "Yes";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

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

    @Before
    public void setUp() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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
        DynamicList originalSender = new DynamicList(value, Collections.singletonList(value));

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
    public void givenAHandleEvidenceEvent_thenReturnTrue() {
        assertTrue(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenANonHandleEvidenceEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertFalse(actionFurtherEvidenceAboutToSubmitHandler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenAPostponementRequestWithoutDetails_thenAddAnError() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
            new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .fileName("Testing.jpg").url(DocumentLink.builder()
                    .documentUrl("test1.com").build()).build()).build();


        sscsCaseData.setScannedDocuments(Arrays.asList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), is(not(empty())));
        assertThat(response.getErrors().iterator().next(),
            is(ActionFurtherEvidenceAboutToSubmitHandler.POSTPONEMENT_DETAILS_IS_MANDATORY));
    }

    @Test
    public void givenAValidPostponementRequest_thenSscsDocumentTypeIsPostponementRequestAndOriginalSenderIsSetAndNoteIsCreatedAndFlagUnprocessedIsYes() {
        when(caseDetails.getState()).thenReturn(State.HEARING);
        sscsCaseData.getFurtherEvidenceAction().setValue(
            new DynamicListItem(FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getCode(),
                FurtherEvidenceActionDynamicListItems.SEND_TO_INTERLOC_REVIEW_BY_TCW.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder()
            .value(ScannedDocumentDetails.builder().type(DocumentType.POSTPONEMENT_REQUEST.getValue())
                .fileName("Testing.jpg").url(DocumentLink.builder()
                    .documentUrl("test.com").build()).build()).build();


        sscsCaseData.setScannedDocuments(Arrays.asList(scannedDocument));
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().postponementRequestDetails("Request Detail Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(DocumentType.POSTPONEMENT_REQUEST.getValue()));
        assertThat(sscsDocumentDetail.getOriginalPartySender(), is(sscsCaseData.getOriginalSender().getValue().getCode()));
        assertThat(sscsCaseData.getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.YES));
        assertThat(sscsCaseData.getAppealNotePad().getNotesCollection().stream()
            .anyMatch(note -> note.getValue().getNoteDetail().equals("Request Detail Test")), is(true));
    }

    @Test
    @DisplayName("Given a valid post hearing application request, "
        + "and review by judge is selected, "
        + "and original sender is DWP, "
        + "then DWP State is updated and types are set")
    @Parameters({
        "setAsideApplication, SET_ASIDE_REQUESTED, SET_ASIDE",
        "correctionApplication, CORRECTION_REQUESTED, CORRECTION",
        "statementOfReasonsApplication, STATEMENT_OF_REASONS_REQUESTED, STATEMENT_OF_REASONS",
        "libertyToApplyApplication, LIBERTY_TO_APPLY_REQUESTED, LIBERTY_TO_APPLY",
        "permissionToAppealApplication, PERMISSION_TO_APPEAL_REQUESTED, PERMISSION_TO_APPEAL"
    })
    public void givenAValidPostHearingApplicationRequest_andReviewByJudgeIsSelected_andOriginalSenderIsDwp_thenDwpStateIsUpdatedAndTypesAreSet(
        String documentType,
        DwpState dwpState,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(dwpState));
        assertThat(response.getData().getPostHearing().getRequestType(), is(requestType));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(documentType));
    }

    @Test
    @DisplayName("Given a valid post hearing application request, "
        + "and review by judge is selected, "
        + "and original sender is NOT DWP, "
        + "then DWP State is NOT updated and types are set")
    @Parameters({
        "setAsideApplication, SET_ASIDE",
        "correctionApplication, CORRECTION",
        "statementOfReasonsApplication, STATEMENT_OF_REASONS",
        "libertyToApplyApplication, LIBERTY_TO_APPLY",
        "permissionToAppealApplication, PERMISSION_TO_APPEAL"
    })
    public void givenAValidPostHearingApplicationRequest_andReviewByJudgeIsSelected_andOriginalSenderIsNotDwp_thenDwpStateIsNotUpdatedAndTypesAreSet(
        String documentType,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(nullValue()));
        assertThat(response.getData().getPostHearing().getRequestType(), is(requestType));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(documentType));
    }

    @Test
    @DisplayName("Given a valid post hearing application request from an FTA user with 'Admin Action X' selected, "
        + "then DWP state and Interloc Review state are updated, "
        + "and post hearing request type and document type are identified as expected.")
    @Parameters({
        "ADMIN_ACTION_CORRECTION, correctionApplication, CORRECTION_REQUESTED, CORRECTION",
    })
    public void givenAValidPostHearingApplicationRequest_andAdminActionIsSelected_thenDwpStateIsUpdatedAndTypesAreSet(
        FurtherEvidenceActionDynamicListItems adminAction,
        String documentType,
        DwpState dwpState,
        PostHearingRequestType requestType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(dwpState));
        assertThat(response.getData().getPostHearing().getRequestType(), is(requestType));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(documentType));
    }

    @Test
    @DisplayName("Given a post hearing document is uploaded from an FTA user "
        + "and post hearings flag is disabled, "
        + "then DWP state and post hearing are not updated.")
    @Parameters({
        "setAsideApplication",
        "correctionApplication",
        "statementOfReasonsApplication",
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    public void givenAPostHearingDocumentUpload_andPostHearingsFlagIsDisabled_thenDoesNotUpdatePostHearingOrDwpState(
        String documentType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(nullValue()));
        assertThat(response.getData().getPostHearing().getRequestType(), is(nullValue()));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(documentType));
    }


    @Test
    @DisplayName("Given a post hearing B document is uploaded from an FTA user "
        + "and post hearings B flag is disabled, "
        + "then DWP state and post hearing are not updated.")
    @Parameters({
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    public void givenAPostHearingBDocumentUpload_andPostHearingsBFlagIsDisabled_thenDoesNotUpdatePostHearingOrDwpState(
        String documentType
    ) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, false);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(DWP.getCode(), DWP.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(nullValue()));
        assertThat(response.getData().getPostHearing().getRequestType(), is(nullValue()));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(documentType));
    }

    @Test
    @Parameters({
        "setAsideApplication",
        "statementOfReasonsApplication",
        "libertyToApplyApplication",
        "permissionToAppealApplication"
    })
    public void givenAValidPostHearingApplicationWithInvalidFurtherEvidenceAction_thenThrowInvalidActionError(String documentType) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), hasSize(1));
        assertThat(response.getErrors(), hasItem(
                String.format("'Further Evidence Action' must be set to '%s'",
                        SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel())));
    }

    @Test
    @Parameters({
        "correctionApplication,Admin action correction",
    })
    public void givenAValidPostHearingApplicationWithInvalidFurtherEvidenceAction_thenThrowInvalidActionError(String documentType, String actionLabel) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getErrors(), hasSize(1));
        assertThat(response.getErrors(), hasItem(
            String.format("'Further Evidence Action' must be set to '%s' or '%s'",
                SEND_TO_INTERLOC_REVIEW_BY_JUDGE.getLabel(), actionLabel)));
    }

    @Test
    @Parameters({
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
    public void givenAValidPostHearingRequestFromParty_thenOriginalSenderSetOnSscsDocument(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOriginalSender().getValue().getCode(), is(sender.getCode()));
        assertThat(response.getData().getOriginalSender().getValue().getLabel(), is(sender.getLabel()));
    }

    @Test
    @Parameters({
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
    public void givenValidAPostHearingRequestFromParty_thenAddFooterTextToDocument(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).addFooter(eq(scannedDocument.getValue().getUrl()), eq(sender.getDocumentFooter()), eq(null));
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
        assertEquals(documentType, response.getData().getSscsDocument().get(0).getValue().getDocumentType());
    }

    @Test
    @Parameters({
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
    public void givenValidAPostHearingRequestFromPartyWhenIncludeInBundle_thenAddDocumentToBundle(String documentType, PartyItemList sender) {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    @Test
    @Parameters(method = "generateFurtherEvidenceActionListScenarios")
    public void givenACaseWithScannedDocuments_shouldMoveToSscsDocuments(@Nullable DynamicList furtherEvidenceActionList,
                                                                         @Nullable DynamicList originalSender,
                                                                         @Nullable String evidenceHandle,
                                                                         DocumentType expectedDocumentType) {
        sscsCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
        sscsCaseData.setOriginalSender(originalSender);
        sscsCaseData.setEvidenceHandled(evidenceHandle);

        PreSubmitCallbackResponse<SscsCaseData> response = null;
        try {
            response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        } catch (IllegalStateException e) {
            assertTrue(furtherEvidenceActionList == null || originalSender == null);
        }
        if (null != furtherEvidenceActionList && null != originalSender) {
            assertHappyPaths(expectedDocumentType, response);
        }
    }

    @Test
    public void givenAConfidentialCaseWithScannedDocumentsAndEditedDocument_shouldMoveToSscsDocumentsWithEditedDocument() {
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
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertEquals("Appellant evidence received on 13-06-2020", sscsDocumentDetail.getDocumentFileName());
        assertEquals("appellantEvidence", sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("www.edited.com", sscsDocumentDetail.getEditedDocumentLink().getDocumentUrl());
        assertEquals("2020-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        assertEquals(NO, response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getEvidenceHandled());
    }

    @Test
    @Parameters({"true", "false"})
    public void givenACaseWithScannedDocumentOfTypeCoversheet_shouldNotMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
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

        assertTrue(CollectionUtils.isEmpty(response.getData().getSscsDocument()));
        assertEquals(YES, response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("Coversheet will be ignored, are you happy to proceed?", warning);
        }
    }

    @Test
    @Parameters({"true", "false"})
    public void givenACaseWithAnEmptyScannedDocumentType_shouldMoveToSscsDocumentsAndWarningShouldBeReturned(boolean ignoreWarnings) {
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

        assertEquals(response.getData().getSscsDocument().size(), 1);
        assertEquals(YES, response.getData().getEvidenceHandled());

        if (ignoreWarnings) {
            assertEquals(0, response.getWarnings().size());
        } else {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("Document type is empty, are you happy to proceed?", warning);
        }
    }

    @Test
    public void edgeCaseWhereEvidenceIsSentToBulkPrintSinceNoFurtherActionIsSelected() {
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

        assertEquals(response.getData().getSscsDocument().size(), 1);
        assertEquals(response.getData().getSscsDocument().get(0).getValue().getEvidenceIssued(), NO);
        assertEquals(YES, response.getData().getEvidenceHandled());
        assertEquals(0, response.getWarnings().size());
    }

    private void assertHappyPaths(DocumentType expectedDocumentType,
                                  PreSubmitCallbackResponse<SscsCaseData> response) {

        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(1).getValue();
        assertEquals((expectedDocumentType.getLabel() != null ? expectedDocumentType.getLabel() : expectedDocumentType.getValue()) + " received on 13-06-2019", sscsDocumentDetail.getDocumentFileName());
        assertEquals(expectedDocumentType.getValue(), sscsDocumentDetail.getDocumentType());
        assertEquals("www.test.com", sscsDocumentDetail.getDocumentLink().getDocumentUrl());
        assertEquals("2019-06-13", sscsDocumentDetail.getDocumentDateAdded());
        assertEquals("123", sscsDocumentDetail.getControlNumber());
        String expectedEvidenceIssued = ACTIONS_THAT_REQUIRES_EVIDENCE_ISSUED_SET_TO_YES_AND_NOT_BULK_PRINTED.contains(response.getData().getFurtherEvidenceAction().getValue().getCode()) ? YES : NO;
        assertEquals(expectedEvidenceIssued, response.getData().getSscsDocument().get(1).getValue().getEvidenceIssued());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getEvidenceHandled());
    }

    private Object[] generateFurtherEvidenceActionListScenarios() {
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

        return new Object[]{
            //other options scenarios
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListOtherDocuments, dwpOriginalSender, YES, DWP_EVIDENCE},
            //issue parties scenarios
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, YES, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListIssueParties, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            //interloc scenarios
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, null, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, NO, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, appellantOriginalSender, YES, APPELLANT_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, null, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, NO, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, representativeOriginalSender, YES, REPRESENTATIVE_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, null, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, NO, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, dwpOriginalSender, YES, DWP_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, null, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, NO, JOINT_PARTY_EVIDENCE},
            new Object[]{furtherEvidenceActionListInterloc, jointPartyOriginalSender, YES, JOINT_PARTY_EVIDENCE},
            //edge cases scenarios
            new Object[]{null, representativeOriginalSender, "", null}, //edge case: furtherEvidenceActionOption is null
            new Object[]{furtherEvidenceActionListIssueParties, null, null, null} //edge case: originalSender is null
        };
    }

    protected static DynamicList buildOriginalSenderItemListForGivenOption(String code, String label) {
        DynamicListItem value = new DynamicListItem(code, label);
        return new DynamicList(value, Collections.singletonList(value));
    }

    private DynamicList buildFurtherEvidenceActionItemListForGivenOption(String code, String label) {
        DynamicListItem selectedOption = new DynamicListItem(code, label);
        return new DynamicList(selectedOption,
            Collections.singletonList(selectedOption));
    }

    @Test
    public void givenACaseWithScannedDocumentsAndSscsCaseDocuments_thenAppendNewDocumentsToSscsDocumentsList() {
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

        assertEquals("Appellant evidence received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals("Appellant evidence received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
        assertNotNull(response.getData().getSscsDocument().get(0).getValue().getControlNumber());
        assertNull(response.getData().getScannedDocuments());
    }

    @Test
    public void givenAWelshCaseWithScannedDocuments_thenSetTranslationStatusToRequired() {
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

        assertEquals("Appellant evidence received on 12-06-2019", response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
        assertEquals("2019-06-12", response.getData().getSscsDocument().get(0).getValue().getDocumentDateAdded());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, response.getData().getSscsDocument().get(0).getValue().getDocumentTranslationStatus());

        assertEquals("Appellant evidence received on 13-06-2019", response.getData().getSscsDocument().get(1).getValue().getDocumentFileName());
        assertEquals("2019-06-13", response.getData().getSscsDocument().get(1).getValue().getDocumentDateAdded());
        assertEquals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED, response.getData().getSscsDocument().get(1).getValue().getDocumentTranslationStatus());

        assertEquals("exist.pdf", response.getData().getSscsDocument().get(2).getValue().getDocumentFileName());
        assertNull(response.getData().getSscsDocument().get(2).getValue().getDocumentTranslationStatus());
        assertNull(response.getData().getScannedDocuments());
        assertEquals(YES, response.getData().getTranslationWorkOutstanding());
    }

    @Test
    public void givenACaseWithScannedDocumentWithEmptyValues_thenHandleTheDocument() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder()
                .url(DocumentLink.builder().documentUrl("www.test.com").build())
                .build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("www.test.com", response.getData().getSscsDocument().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void givenACaseWithNoScannedDocuments_thenAddAnErrorToResponse() {
        sscsCaseData.setScannedDocuments(null);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No further evidence to process", error);
        }
    }

    private Callback<SscsCaseData> buildCallback(String dynamicListItemCode, State state) {
        DynamicList dynamicList = new DynamicList(new DynamicListItem(dynamicListItemCode, "label"),
            Collections.singletonList(new DynamicListItem(dynamicListItemCode, "label")));
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .originalSender(dynamicList)
            .furtherEvidenceAction(dynamicList)
            .state(state)
            .scannedDocuments(Collections.singletonList(ScannedDocument.builder().build()))
            .appeal(Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("My Road").postcode("TS1 2BA").build()).build()).build())
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), EventType.ACTION_FURTHER_EVIDENCE, false);
    }

    @Test
    public void givenIssueFurtherEvidenceWhenStateNotWithDwp_shouldUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(FURTHER_EVIDENCE_RECEIVED, updated.getData().getDwpFurtherEvidenceStates());
        assertEquals(DwpState.FE_RECEIVED, updated.getData().getDwpState());
    }

    @Test
    public void givenIssueFurtherEvidenceAndStateIsWithDwp_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpState());
        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    @Parameters(method = "generateIssueFurtherEvidenceAddressEmptyScenarios")
    public void givenIssueFurtherEvidenceAndEmptyAppellantAddress_shouldReturnAnErrorToUser(Appeal appeal, String... parties) {
        Callback<SscsCaseData> callback = buildCallback(ISSUE_FURTHER_EVIDENCE.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        callback.getCaseDetails().getCaseData().setAppeal(appeal);
        PreSubmitCallbackResponse<SscsCaseData> result = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        for (String party : parties) {
            String expectedError = "Address details are missing for the " + party + ", please validate or process manually";
            assertTrue(result.getErrors().contains(expectedError));
        }
    }

    private Object[] generateIssueFurtherEvidenceAddressEmptyScenarios() {

        return new Object[]{
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().line1("Line1").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(Address.builder().postcode("TS1 2BA").build()).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().address(null).build()).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(null).build(), "Appellant"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(Address.builder().build()).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().appellant(Appellant.builder().isAppointee(YES).appointee(Appointee.builder().address(null).build()).build()).build(), "Appointee"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(null).build()).appellant(Appellant.builder().address(Address.builder().line1("The road").build()).build()).build(), "Representative"},
            new Object[]{Appeal.builder().rep(Representative.builder().hasRepresentative(YES).address(Address.builder().build()).build()).appellant(Appellant.builder().address(null).build()).build(), "Appellant", "Representative"},
        };
    }

    @Test
    public void givenOtherDocument_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(OTHER_DOCUMENT_MANUAL.getCode(), State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenNullFurtherEvidenceAction_shouldNotUpdateDwpFurtherEvidenceStates() {
        Callback<SscsCaseData> callback = buildCallback(null, State.INTERLOCUTORY_REVIEW_STATE);

        PreSubmitCallbackResponse<SscsCaseData> updated = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(updated.getData().getDwpFurtherEvidenceStates());
    }

    @Test
    public void givenADocumentWithNoUrl_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document URL so could not process", error);
        }
    }

    @Test
    public void givenANonConfidentialCaseAndEditedDocumentPopulated_thenAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DOC_LINK)
                .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.NO);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("Case is not marked as confidential so cannot upload an edited document", error);
        }
    }

    @Test
    public void givenAConfidentialCaseAndEditedDocumentPopulated_thenDoNotAddAnErrorToResponse() {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("Testing.jpg").url(DOC_LINK)
                .editedUrl(DocumentLink.builder().documentUrl("test").build()).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);
        sscsCaseData.setIsConfidentialCase(YesNo.YES);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    @Parameters({"null", " ", "    "})
    public void givenADocumentWithNoDocFileName_thenAddAnErrorToResponse(@Nullable String filename) {
        List<ScannedDocument> docs = new ArrayList<>();

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName(filename).url(DOC_LINK).build()).build();

        docs.add(scannedDocument);

        sscsCaseData.setScannedDocuments(docs);

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("No document file name so could not process", error);
        }
    }

    @Test
    @Parameters({"null", "DORMANT_APPEAL_STATE", "VOID_STATE"})
    public void shouldReviewByJudgeAndUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

        List<ScannedDocument> docs = new ArrayList<>();
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData.getPreviousState());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getReinstatementOutcome());
        assertEquals(LocalDate.now(), sscsCaseData.getReinstatementRegistered());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    @Parameters({"VALID_APPEAL", "READY_TO_LIST"})
    public void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasReinstatementRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getReinstatementOutcome());
        assertEquals(LocalDate.now(), sscsCaseData.getReinstatementRegistered());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void skipReinstatementRequestSettingIfWelsh() {

        State previousState = State.VALID_APPEAL;

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertTrue(sscsCaseData.isTranslationWorkOutstanding());
    }

    @Test
    @Parameters({"VALID_APPEAL", "READY_TO_LIST"})
    public void shouldReviewByJudgeButNotUpdatePreviousStateWhenActionManuallyAndHasUrgentHearingRequestDocument(@Nullable State previousState) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);


        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        sscsCaseData.setPreviousState(previousState);

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.URGENT_HEARING_REQUEST.getValue())
                .url(DOC_LINK).includeInBundle(NO).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(previousState, sscsCaseData.getPreviousState());
        assertEquals(1, response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count());
    }

    @Test
    @Parameters({"READY_TO_LIST,1", "RESPONSE_RECEIVED,1", "DORMANT_APPEAL_STATE,1", "HEARING,1", "WITH_DWP,1", "VALID_APPEAL,0", "NOT_LISTABLE,1", "LISTING_ERROR,1", "HANDLING_ERROR,1"})
    public void shouldSetBundleAdditionBasedOnPreviousState(@Nullable State state, int occurrence) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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

        assertEquals(occurrence, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    @Test
    @Parameters({"ISSUE_FURTHER_EVIDENCE,No,1", "OTHER_DOCUMENT_MANUAL,Yes,1", "OTHER_DOCUMENT_MANUAL,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,1", "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,Yes,1", "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,No,0",
        "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,Yes,1", "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,No,0",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,Yes,1", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,No,0"})
    public void shouldSetBundleAdditionBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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

        assertEquals(occurs, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    @Parameters({"ADMIN_ACTION_CORRECTION","SEND_TO_INTERLOC_REVIEW_BY_JUDGE"})
    public void shouldSetBundleAdditionForCorrectionApplication(FurtherEvidenceActionDynamicListItems actionType) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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

        assertEquals(1, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoReinstatementRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getReinstatementOutcome());
        assertNull(sscsCaseData.getReinstatementRegistered());
        assertNull(sscsCaseData.getInterlocReviewState());
    }

    @Test
    public void shouldNotUpdateInterlocReviewStateWhenActionManuallyAndHasNoUrgentHearingRequestDocument() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(OTHER_DOCUMENT_MANUAL.code, OTHER_DOCUMENT_MANUAL.label));
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertNull(sscsCaseData.getPreviousState());
        assertNull(sscsCaseData.getInterlocReviewState());
        assertEquals(0, response.getData().getSscsDocument().stream().filter(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType())).count());
    }

    @Test
    public void givenWhenNotActionManuallyAndHasReinstatementRequestDocumentThenSetReinstateCaseFields() {
        sscsCaseData.setPreviousState(null);
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.REINSTATEMENT_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(LocalDate.now(), sscsCaseData.getReinstatementRegistered());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, sscsCaseData.getInterlocReviewState());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData.getPreviousState());
    }

    @Test
    @Parameters({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFields(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

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
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
    }

    @Test
    @Parameters({
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, Yes",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE, No",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, Yes",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, No",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE, null"
    })
    public void givenConfidentialRequestWhenJointPartyExistsAndAlreadyHasConfidentialityFromOriginalSenderAppellant_thenUpdateCaseWithConfidentialFieldsAndDisplayWarning(
        FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem,
        @Nullable String isProgressingViaGaps
    ) {
        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        sscsCaseData.setIsProgressingViaGaps(isProgressingViaGaps);
        sscsCaseData.setConfidentialityRequestOutcomeJointParty(createDatedRequestOutcome(RequestOutcome.GRANTED));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());

        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeAppellant().getRequestOutcome());
        assertEquals(createDatedRequestOutcome(RequestOutcome.GRANTED), sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeAppellant().getDate());
    }

    @Test
    public void givenAnAppellantHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());

        assertEquals(1, response.getWarnings().size());
        assertEquals("This case has a confidentiality flag, ensure any evidence from the appellant (or appointee) has confidential information redacted", response.getWarnings().iterator().next());
    }

    @Test
    public void givenAJointPartyHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());

        assertEquals(1, response.getWarnings().size());
        assertEquals("This case has a confidentiality flag, ensure any evidence from the joint party has confidential information redacted", response.getWarnings().iterator().next());
    }

    @Test
    public void givenAConfidentialChildSupportCase_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());

        assertEquals(2, response.getWarnings().size());
        assertEquals("This case has a confidentiality flag, ensure any evidence from the appellant (or appointee) has confidential information redacted", response.getWarnings().iterator().next());
    }

    @Test
    public void givenAnAppellantHasConfidentialityRequestGrantedAndJointPartySendsFurtherEvidence_thenDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenAJointPartyHasConfidentialityRequestGrantedAndAppellantSendsFurtherEvidence_thenDoNotDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
    }

    @Test
    public void givenANonConfidentialChildSupportCase_thenDoNotDisplayWarningWhenActionFurtherEvidenceEventTriggered() {
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
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
        assertEquals("1", response.getData().getSscsDocument().get(0).getValue().getOriginalSenderOtherPartyId());
        assertEquals("Other Party", response.getData().getSscsDocument().get(0).getValue().getOriginalSenderOtherPartyName());
    }

    @Test
    @Parameters({"SEND_TO_INTERLOC_REVIEW_BY_JUDGE", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE"})
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderAppellant_thenShowError(FurtherEvidenceActionDynamicListItems furtherEvidenceActionDynamicListItem) {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(furtherEvidenceActionDynamicListItem.code, furtherEvidenceActionDynamicListItem.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", response.getErrors().iterator().next());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
    }

    @Test
    public void givenConfidentialRequestWhenJointPartyExistsFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

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
        assertEquals(0, response.getErrors().size());
        assertEquals(RequestOutcome.IN_PROGRESS, sscsCaseData.getConfidentialityRequestOutcomeJointParty().getRequestOutcome());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
        assertEquals(LocalDate.now(), sscsCaseData.getConfidentialityRequestOutcomeJointParty().getDate());
    }

    @Test
    public void givenConfidentialRequestWhenJointPartyDoesNotExistFromOriginalSenderJointParty_thenUpdateCaseWithConfidentialFields() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(JOINT_PARTY.getCode(), JOINT_PARTY.getLabel()));

        ScannedDocument scannedDocument = ScannedDocument.builder().value(
            ScannedDocumentDetails.builder().fileName("filename.pdf").type(ScannedDocumentType.CONFIDENTIALITY_REQUEST.getValue())
                .url(DOC_LINK).build()).build();
        List<ScannedDocument> docs = new ArrayList<>();
        docs.add(scannedDocument);
        sscsCaseData.setScannedDocuments(docs);
        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());
        assertEquals("Document type \"Confidentiality Request\" is invalid as there is no joint party on the case", response.getErrors().iterator().next());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeJointParty());
        assertNull(sscsCaseData.getConfidentialityRequestOutcomeAppellant());
    }


    @Test
    public void givenConfidentialRequestFromRep_thenShowAnError() {

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
        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Original sender must be appellant or joint party for a confidential document", error);
        }
    }

    @Test
    public void givenChildSupportWithConfidentialRequestFromOtherPartyDoNotMatchWithDocumentOriginalSender_thenShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue ccdValue = CcdValue.builder().value(otherParty).build();
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
        assertEquals(2, response.getErrors().size());
        assertTrue(response.getErrors().stream().anyMatch(e -> e.equals("The PDF evidence does not match the Original Sender selected")));
    }

    @Test
    public void givenChildSupportWithOtherRequestFromOtherPartyWithDocumentOriginalSenderIsNull_thenDonNotShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue ccdValue = CcdValue.builder().value(otherParty).build();
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
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenChildSupportWithConfidentialRequestFromOtherPartyDoMatchWithDocumentOriginalSender_thenDontShowAnError() {

        sscsCaseData.getFurtherEvidenceAction().setValue(new DynamicListItem(SEND_TO_INTERLOC_REVIEW_BY_JUDGE.code, SEND_TO_INTERLOC_REVIEW_BY_JUDGE.label));
        sscsCaseData.getJointParty().setHasJointParty(YesNo.YES);
        List<CcdValue<OtherParty>> otherPartyList = new ArrayList<>();
        OtherParty otherParty = OtherParty.builder().id("10").name(Name.builder().firstName("John").lastName("smith").build()).build();
        CcdValue ccdValue = CcdValue.builder().value(otherParty).build();
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
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenChildSupportWithOtherRequestWhenOtherPartyIsEmpty_thenDontShowAnError() {

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
        assertEquals(0, response.getErrors().size());
    }

    @Test
    public void givenConfidentialRequestWithInvalidFurtherEvidenceAction_thenShowAnError() {

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
        assertEquals(1, response.getErrors().size());
        for (String error : response.getErrors()) {
            assertEquals("Further evidence action must be 'Send to Interloc - Review by Judge' or 'Information received for Interloc - send to Judge' for a confidential document", error);
        }
    }

    @Test
    @Parameters({"APPELLANT", "REPRESENTATIVE", "DWP", "JOINT_PARTY", "HMCTS"})
    public void shouldIssueToAllParties_willAddFooterTextToDocument(PartyItemList sender) {
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
        assertEquals(0, response.getErrors().size());
        assertEquals(0, response.getWarnings().size());
        assertEquals(sender.getDocumentType().getValue(), response.getData().getSscsDocument().get(0).getValue().getDocumentType());
    }

    @Test
    @Parameters({
        "ISSUE_FURTHER_EVIDENCE,Yes,1",
        "ISSUE_FURTHER_EVIDENCE,No,1",
        "OTHER_DOCUMENT_MANUAL,Yes,1",
        "OTHER_DOCUMENT_MANUAL,No,0",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,1",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0"
    })
    public void shouldAddDocumentToBundleBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int occurs) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);

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

        assertEquals(occurs, response.getData().getSscsDocument().stream().filter(doc -> "A".equals(doc.getValue().getBundleAddition())).count());
    }

    @Test
    @Parameters({
        "otherParty,    1, Other Party",
        "otherPartyRep, 2, Rep Party"
    })
    public void findOriginalSenderOtherPartyId(String otherPartyCode, String otherPartyId, String expectedName) {
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

        assertEquals(otherPartyId, response.getData().getSscsDocument().get(0).getValue().getOriginalSenderOtherPartyId());
        assertEquals(expectedName, response.getData().getSscsDocument().get(0).getValue().getOriginalSenderOtherPartyName());
    }

    @Parameters({"ISSUE_FURTHER_EVIDENCE,No,0", "ISSUE_FURTHER_EVIDENCE,Yes,0", "ISSUE_FURTHER_EVIDENCE,null,0",
        "OTHER_DOCUMENT_MANUAL,Yes,0", "OTHER_DOCUMENT_MANUAL,No,0", "OTHER_DOCUMENT_MANUAL,null,1",
        "SEND_TO_INTERLOC_REVIEW_BY_TCW,Yes,0", "SEND_TO_INTERLOC_REVIEW_BY_TCW,No,0", "SEND_TO_INTERLOC_REVIEW_BY_TCW,,1",
        "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,Yes,0", "SEND_TO_INTERLOC_REVIEW_BY_JUDGE,No,0","SEND_TO_INTERLOC_REVIEW_BY_JUDGE,,1",
        "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,Yes,0", "INFORMATION_RECEIVED_FOR_INTERLOC_TCW,No,0","INFORMATION_RECEIVED_FOR_INTERLOC_TCW,,1",
        "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,Yes,0", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,No,0", "INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE,null,1"})
    public void shouldSetBundleAdditionWarningBasedOnFurtherEvidenceActionType(FurtherEvidenceActionDynamicListItems actionType, String includeInBundle, int warnings) {

        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), false, false);
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

        assertEquals(warnings, response.getWarnings().size());
        if (warnings == 1) {
            String warning = response.getWarnings().stream().findFirst().orElse("");
            assertEquals("No documents have been ticked to be added as an addition. These document(s) will NOT be added to the bundle. Are you sure?", warning);
        }
    }

    private DatedRequestOutcome createDatedRequestOutcome(RequestOutcome requestOutcome) {
        return DatedRequestOutcome.builder().date(LocalDate.now().minusDays(1))
            .requestOutcome(requestOutcome).build();
    }

    @Test
    public void isThreadSafe() throws Exception {

        DynamicList furtherEvidenceActionList =
            buildFurtherEvidenceActionItemListForGivenOption(OTHER_DOCUMENT_MANUAL.getCode(),
                OTHER_DOCUMENT_MANUAL.getLabel());

        DynamicList originalSender = buildOriginalSenderItemListForGivenOption("appellant",
            "Appellant (or Appointee)");

        String evidenceHandle = null;

        int numberOfTasks = 3;
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        AtomicBoolean noOverwrite = new AtomicBoolean(true);

        try {
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

                Callback<SscsCaseData> mockCallback = mock(Callback.class);
                CaseDetails<SscsCaseData> mockCaseDetails = mock(CaseDetails.class);

                when(mockCallback.getEvent()).thenReturn(EventType.ACTION_FURTHER_EVIDENCE);
                when(mockCallback.getCaseDetails()).thenReturn(mockCaseDetails);
                when(mockCaseDetails.getCaseData()).thenReturn(thisCaseData);

                executor.execute(new ThreadRunnable(i, actionFurtherEvidenceAboutToSubmitHandler, mockCallback, expectedId1, noOverwrite));
            }
        } catch (Exception err) {
            err.printStackTrace();
        }

        Thread.sleep(3000);
        assertTrue(noOverwrite.get());
        executor.shutdown();
    }

    @Test
    public void givenACaseWithScannedDocuments_shouldGenerateAMapOfSscsDocuments() throws JsonProcessingException {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false);
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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("One document has been added to the case and should be added to added documents.")
            .containsOnly(org.assertj.core.api.Assertions.entry("reinstatementRequest", 1));
    }

    @Test
    public void givenACaseWithScannedDocumentsSubmittedMultipleTimes_shouldGenerateAMapOfSscsDocumentsWithTheMostRecentDocuments()
        throws JsonProcessingException {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false);
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

        sscsCaseData.setScannedDocuments(Collections.singletonList(reinstatementRequestScannedDocument));

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(confidentialityRequestScannedDocument));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Map<String, Integer> addedDocuments = new ObjectMapper().readerFor(Map.class)
            .readValue(response.getData().getWorkAllocationFields().getAddedDocuments());

        org.assertj.core.api.Assertions.assertThat(addedDocuments)
            .as("Added documents should only contain documents from the current event.")
            .containsOnly(org.assertj.core.api.Assertions.entry("confidentialityRequest", 1));
    }

    @Test
    public void givenACaseWithNoScannedDocument_shouldClearAddedDocuments() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false);

        sscsCaseData.setScannedDocuments(null);
        sscsCaseData.setWorkAllocationFields(WorkAllocationFields.builder()
            .addedDocuments("{audioEvidence=1}")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
            ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        org.assertj.core.api.Assertions.assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Added documents should be reset on each event.")
            .isNull();
    }

    @Test
    public void givenACaseWithScannedDocumentCoversheet_shouldGenerateAnEmptyMapOfSscsDocuments() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService,
            bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(true), false, false);
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

        org.assertj.core.api.Assertions.assertThat(response.getData().getWorkAllocationFields().getAddedDocuments())
            .as("Only a coversheet has been attached - this should be ignored.")
            .isNull();
    }

    public void givenAValidPostHearingOtherRequest_andReviewByJudgeIsSelected_thenDontThrowError() {
        actionFurtherEvidenceAboutToSubmitHandler = new ActionFurtherEvidenceAboutToSubmitHandler(footerService, bundleAdditionFilenameBuilder, userDetailsService, new AddedDocumentsUtil(false), true, true);

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

        sscsCaseData.setScannedDocuments(Collections.singletonList(scannedDocument));
        sscsCaseData.getOriginalSender().setValue(new DynamicListItem(APPELLANT.getCode(), APPELLANT.getLabel()));

        PreSubmitCallbackResponse<SscsCaseData> response = actionFurtherEvidenceAboutToSubmitHandler.handle(
                ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getDwpState(), is(nullValue()));
        SscsDocumentDetails sscsDocumentDetail = response.getData().getSscsDocument().get(0).getValue();
        assertThat(sscsDocumentDetail.getDocumentType(), is(POST_HEARING_OTHER.getLabel()));
    }
}
