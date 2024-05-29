package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_FURTHER_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.INTERLOCUTORY_REVIEW_STATE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.FurtherEvidenceService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class IssueFurtherEvidenceHandlerTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FurtherEvidenceService furtherEvidenceService;

    @Mock
    private IdamService idamService;

    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    @Mock
    private SscsCcdConvertService sscsCcdConvertService;

    @Mock
    private CcdClient ccdClient;

    @InjectMocks
    private IssueFurtherEvidenceHandler issueFurtherEvidenceHandler;

    @Captor
    ArgumentCaptor<Consumer<SscsCaseData>> captor;

    @Captor
    ArgumentCaptor<Function<SscsCaseData, UpdateCcdCaseService.UpdateResult>> functionArgumentCaptor;

    @Captor
    ArgumentCaptor<CaseDataContent> caseDataContentArgumentCaptor;

    private final SscsDocument sscsDocumentNotIssued = SscsDocument.builder()
        .value(SscsDocumentDetails.builder()
            .documentType(APPELLANT_EVIDENCE.getValue())
            .evidenceIssued("No")
            .build())
        .build();

    private final SscsCaseData caseData = SscsCaseData.builder()
        .ccdCaseId("1563382899630221")
        .sscsDocument(Collections.singletonList(sscsDocumentNotIssued))
        .appeal(Appeal.builder().build())
        .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Before
    public void setup() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(true);
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        issueFurtherEvidenceHandler.handle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"REISSUE_FURTHER_EVIDENCE", "EVIDENCE_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenEventTypeIsNotIssueFurtherEvidence_willThrowAnException(EventType eventType) {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, eventType));
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_shouldThrowException() {
        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = NullPointerException.class)
    public void givenCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED, null);
    }

    @Test(expected = RequiredFieldMissingException.class)
    public void givenCaseDataInCallbackIsNull_whenCanHandleIsCalled_shouldThrowException() {
        issueFurtherEvidenceHandler.canHandle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(null, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = IllegalStateException.class)
    public void givenHandleMethodIsCalled_shouldThrowExceptionIfCanNotBeHandled() {
        given(furtherEvidenceService.canHandleAnyDocument(any())).willReturn(false);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(), INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test(expected = PostIssueFurtherEvidenceTasksException.class)
    public void givenExceptionWhenPostIssueFurtherEvidenceTasks_shouldHandleIt() {
        doThrow(RuntimeException.class).when(updateCcdCaseService).updateCaseV2(
                any(Long.class),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(caseData,
            INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
    }

    @Test
    public void givenExceptionWhenIssuingFurtherEvidence_shouldHandleItAppropriately() {
        doThrow(RuntimeException.class).when(furtherEvidenceService).issue(any(), any(), any(), any(), eq(null));
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        try {
            issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED, HandlerHelper.buildTestCallbackForGivenData(caseData,
                INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));
            fail("no exception thrown");
        } catch (IssueFurtherEvidenceException e) {
            assertEquals("Failed sending further evidence for case(1563382899630221)...", e.getMessage());
        }

        verify(updateCcdCaseService, times(1)).updateCaseV2(any(Long.class),
                eq(EventType.SEND_FURTHER_EVIDENCE_ERROR.getCcdType()),
                eq("Failed to issue further evidence"),
                eq("Review document tab to see document(s) that haven't been issued, then use the"
                        + " \"Reissue further evidence\" within next step and select affected document(s) to re-send"),
                any(IdamTokens.class),
                captor.capture()
        );

        captor.getValue().accept(caseData);

        assertEquals("hmctsDwpState has incorrect value", "failedSendingFurtherEvidence", caseData.getHmctsDwpState());
    }

    @Test
    public void givenIssueFurtherEvidenceCallback_shouldIssueEvidenceForAppellantAndRepAndJointParty() {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        var caseDataContent = CaseDataContent.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDataContent(
                caseData,
                startEventResponse,
                "Update case data",
                "Update issued evidence document flags after issuing further evidence"
        )).thenReturn(caseDataContent);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(APPELLANT_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq(null));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(REPRESENTATIVE_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq(null));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(JOINT_PARTY_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq(null));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(DWP_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq(null));
        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(HMCTS_EVIDENCE),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq(null));
        verify(furtherEvidenceService).canHandleAnyDocument(caseData.getSscsDocument());

        verify(ccdClient).startEvent(any(IdamTokens.class), eq(1L), eq(ISSUE_FURTHER_EVIDENCE.getCcdType()));

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                any(Long.class),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        functionArgumentCaptor.getValue().apply(caseData);

        assertEquals("Yes", caseData.getSscsDocument().get(0).getValue().getEvidenceIssued());

        verifyNoMoreInteractions(updateCcdCaseService);
        verifyNoMoreInteractions(furtherEvidenceService);
    }

    @Test
    public void shouldReturnBaseDescriptionWhenNoResizedDocuments() {
        SscsDocumentDetails docDetails = SscsDocumentDetails.builder().build();
        SscsDocument doc = SscsDocument.builder().value(docDetails).build();

        String result = issueFurtherEvidenceHandler.determineDescription(List.of(doc));
        assertEquals("Update issued evidence document flags after issuing further evidence", result);
    }

    @Test
    public void shouldReturnBaseDescriptionWhenHasResizedDocuments() {
        SscsDocument doc = buildSscsDocument(NO, "resized.pdf", "appellantEvidence", null);

        String result = issueFurtherEvidenceHandler.determineDescription(List.of(doc));
        assertEquals("Update issued evidence document flags after issuing further evidence and attached resized document(s)", result);
    }

    @Test
    public void shouldReturnBaseDescriptionWhenItHasNottHasResizedDocuments() {
        SscsDocument resizedDoc = buildSscsDocument(YES, "resized.pdf", "appellantEvidence", null);
        SscsDocument noResizedDoc = buildSscsDocument(NO, "resized.pdf", "appellantEvidence", null);
        noResizedDoc.getValue().setResizedDocumentLink(null);

        String result = issueFurtherEvidenceHandler.determineDescription(List.of(resizedDoc, noResizedDoc));
        assertEquals("Update issued evidence document flags after issuing further evidence", result);
    }

    @Test
    @Parameters({"OTHER_PARTY_EVIDENCE", "OTHER_PARTY_REPRESENTATIVE_EVIDENCE"})
    public void givenACaseWithAnOtherPartyDocumentNotIssued_shouldIssueEvidenceForOtherParty(DocumentType documentType) {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        SscsDocument otherPartySscsDocumentOtherNotIssued = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1");

        caseData.setSscsDocument(Collections.singletonList(otherPartySscsDocumentOtherNotIssued));

        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(documentType),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq("1"));

        verify(furtherEvidenceService, times(6)).issue(any(), eq(caseData), any(),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), any());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                any(Long.class),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        functionArgumentCaptor.getValue().apply(caseData);

        assertEquals("Yes", caseData.getSscsDocument().get(0).getValue().getEvidenceIssued());
    }

    @Test
    @Parameters({"OTHER_PARTY_EVIDENCE", "OTHER_PARTY_REPRESENTATIVE_EVIDENCE"})
    public void givenACaseWithMultipleOtherPartyDocumentsNotIssuedForTheSameOtherPartyId_shouldIssueEvidenceForOtherParty(DocumentType documentType) {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        SscsDocument otherPartySscsDocumentOtherNotIssued1 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1");

        SscsDocument otherPartySscsDocumentOtherNotIssued2 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "1");

        caseData.setSscsDocument(Arrays.asList(otherPartySscsDocumentOtherNotIssued1, otherPartySscsDocumentOtherNotIssued2));

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(documentType),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq("1"));

        verify(furtherEvidenceService, times(6)).issue(any(), eq(caseData), any(),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), any());

        verify(updateCcdCaseService, times(1)).updateCaseV2(
                any(Long.class),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        functionArgumentCaptor.getValue().apply(caseData);

        assertEquals("Yes", caseData.getSscsDocument().get(0).getValue().getEvidenceIssued());
    }

    @Test
    @Parameters({"OTHER_PARTY_EVIDENCE", "OTHER_PARTY_REPRESENTATIVE_EVIDENCE"})
    public void givenACaseWithMultipleOtherPartyDocumentsNotIssuedForMultipleOtherPartyIds_shouldIssueEvidenceForAllOtherParties(DocumentType documentType) {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        SscsDocument otherPartySscsDocumentOtherNotIssued1 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1");

        SscsDocument otherPartySscsDocumentOtherNotIssued2 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "1");

        SscsDocument otherPartySscsDocumentOtherIssued3 = buildSscsDocument(YES, "test.pdf", documentType.getValue(), "1");

        SscsDocument otherPartySscsDocumentOtherNotIssued4 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "2");

        SscsDocument otherPartySscsDocumentOtherNotIssued5 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "2");

        SscsDocument otherPartySscsDocumentOtherIssued6 = buildSscsDocument(YES, "test2.pdf", documentType.getValue(), "2");

        caseData.setSscsDocument(Arrays.asList(otherPartySscsDocumentOtherNotIssued1, otherPartySscsDocumentOtherNotIssued2, otherPartySscsDocumentOtherIssued3, otherPartySscsDocumentOtherNotIssued4, otherPartySscsDocumentOtherNotIssued5, otherPartySscsDocumentOtherIssued6));

        var caseDataMap = OBJECT_MAPPER.convertValue(caseData, new TypeReference<Map<String, Object>>() {
        });
        var startEventResponse = StartEventResponse.builder().caseDetails(CaseDetails.builder().data(caseDataMap).build()).build();

        when(ccdClient.startEvent(any(IdamTokens.class), any(), eq(EventType.ISSUE_FURTHER_EVIDENCE.getCcdType()))).thenReturn(startEventResponse);

        var sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDetails(startEventResponse)).thenReturn(sscsCaseDetails);

        var caseDataContent = CaseDataContent.builder().data(caseData).build();
        when(sscsCcdConvertService.getCaseDataContent(
                caseData,
                startEventResponse,
                "Update case data",
                "Update issued evidence document flags after issuing further evidence"
        )).thenReturn(caseDataContent);

        issueFurtherEvidenceHandler.handle(CallbackType.SUBMITTED,
            HandlerHelper.buildTestCallbackForGivenData(caseData, INTERLOCUTORY_REVIEW_STATE, ISSUE_FURTHER_EVIDENCE));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(documentType),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq("1"));

        verify(furtherEvidenceService).issue(eq(caseData.getSscsDocument()), eq(caseData), eq(documentType),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), eq("2"));

        verify(furtherEvidenceService, times(7)).issue(any(), eq(caseData), any(),
            eq(Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, JOINT_PARTY_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER)), any());


        verify(updateCcdCaseService, times(1)).updateCaseV2(
                any(Long.class),
                eq(EventType.UPDATE_CASE_ONLY.getCcdType()), any(IdamTokens.class),
                functionArgumentCaptor.capture()
        );

        functionArgumentCaptor.getValue().apply(caseData);

        assertEquals("Yes", caseData.getSscsDocument().get(0).getValue().getEvidenceIssued());
    }

    private SscsDocument buildSscsDocument(YesNo yesNo, String fileName, String documentType, String originalSenderOtherPartyId) {
        SscsDocumentDetails docDetails = SscsDocumentDetails.builder().evidenceIssued(yesNo.getValue())
            .documentType(documentType)
            .resizedDocumentLink(
                DocumentLink.builder().documentFilename(fileName).build()
            )
            .originalSenderOtherPartyId(originalSenderOtherPartyId)
            .build();
        return SscsDocument.builder().value(docDetails).build();
    }

}
