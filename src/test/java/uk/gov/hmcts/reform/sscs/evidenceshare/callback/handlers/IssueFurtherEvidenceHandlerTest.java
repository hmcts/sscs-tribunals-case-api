package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
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
    ArgumentCaptor<Consumer<SscsCaseDetails>> captor;

    @Captor
    ArgumentCaptor<Function<SscsCaseDetails, UpdateCcdCaseService.UpdateResult>> functionArgumentCaptor;

    @Captor
    ArgumentCaptor<CaseDataContent> caseDataContentArgumentCaptor;

    private final SscsDocument sscsDocumentNotIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .evidenceIssued("No")
                    .documentLink(
                            DocumentLink
                                    .builder()
                                    .documentUrl("https://www.doclink.com/doc1")
                                    .documentBinaryUrl("https://www.doclink.com/doc1/binary")
                                    .build()
                    ).resizedDocumentLink(
                            DocumentLink
                                    .builder()
                                    .documentUrl("https://www.resizeddoclink.com/doc1")
                                    .documentBinaryUrl("https://www.resizeddoclink.com/doc1/binary")
                                    .build()
                    )
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
        doThrow(new RuntimeException("some error occurred")).when(furtherEvidenceService).issue(any(), any(), any(), any(), eq(null));
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
            assertEquals("Failed sending further evidence for case(1563382899630221) with exception(some error occurred)", e.getMessage());
        }

        verify(updateCcdCaseService, times(1)).updateCaseV2(any(Long.class),
                eq(EventType.SEND_FURTHER_EVIDENCE_ERROR.getCcdType()),
                eq("Failed to issue further evidence"),
                eq("Review document tab to see document(s) that haven't been issued, then use the"
                        + " \"Reissue further evidence\" within next step and select affected document(s) to re-send"),
                any(IdamTokens.class),
                captor.capture()
        );

        captor.getValue().accept(sscsCaseDetails);

        assertEquals("hmctsDwpState has incorrect value", "failedSendingFurtherEvidence", sscsCaseDetails.getData().getHmctsDwpState());
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

        var updatedCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();

        SscsDocument otherPartySscsDocumentOtherNotIssuedWithoutResizedLink = buildSscsDocument("https://www.doclink.com/doc1");

        updatedCaseDetails.getData().setSscsDocument(List.of(otherPartySscsDocumentOtherNotIssuedWithoutResizedLink));

        functionArgumentCaptor.getValue().apply(updatedCaseDetails);

        updatedCaseDetails.getData().getSscsDocument().forEach(sscsDocument -> {
            assertEquals("Yes", sscsDocument.getValue().getEvidenceIssued());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentUrl());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentBinaryUrl());
        });

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

        SscsDocument otherPartySscsDocumentOtherNotIssued = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc1", "https://www.resizedoclink.com/doc1/binary");

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

        var updatedCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();

        SscsDocument otherPartySscsDocumentOtherNotIssuedWithoutResizedLink = buildSscsDocument("https://www.doclink.com/doc1");

        updatedCaseDetails.getData().setSscsDocument(List.of(otherPartySscsDocumentOtherNotIssuedWithoutResizedLink));

        functionArgumentCaptor.getValue().apply(updatedCaseDetails);

        updatedCaseDetails.getData().getSscsDocument().forEach(sscsDocument -> {
            assertEquals("Yes", sscsDocument.getValue().getEvidenceIssued());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentUrl());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentBinaryUrl());
        });
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

        SscsDocument otherPartySscsDocumentOtherNotIssued1 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc1", "https://www.resizedoclink.com/doc1/binary");
        SscsDocument otherPartySscsDocumentOtherNotIssued2 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc2", "https://www.resizedoclink.com/doc2/binary");

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

        var updatedCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();

        SscsDocument otherPartySscsDocumentOtherNotIssuedWithoutResizedLink1 = buildSscsDocument("https://www.doclink.com/doc1");

        SscsDocument otherPartySscsDocumentOtherNotIssuedWithoutResizedLink2 = buildSscsDocument("https://www.doclink.com/doc2");

        updatedCaseDetails.getData().setSscsDocument(List.of(otherPartySscsDocumentOtherNotIssuedWithoutResizedLink1, otherPartySscsDocumentOtherNotIssuedWithoutResizedLink2));

        functionArgumentCaptor.getValue().apply(updatedCaseDetails);

        updatedCaseDetails.getData().getSscsDocument().forEach(sscsDocument -> {
            assertEquals("Yes", sscsDocument.getValue().getEvidenceIssued());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentUrl());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentBinaryUrl());
        });
    }

    @Test
    @Parameters({"OTHER_PARTY_EVIDENCE", "OTHER_PARTY_REPRESENTATIVE_EVIDENCE"})
    public void givenACaseWithMultipleOtherPartyDocumentsNotIssuedForMultipleOtherPartyIds_shouldIssueEvidenceForAllOtherParties(DocumentType documentType) {
        when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());

        SscsDocument otherPartySscsDocumentOtherNotIssued1 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc1", "https://www.resizeddoclink.com/doc1");

        SscsDocument otherPartySscsDocumentOtherNotIssued2 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc2", "https://www.resizeddoclink.com/doc2");

        SscsDocument otherPartySscsDocumentOtherIssued3 = buildSscsDocument(YES, "test.pdf", documentType.getValue(), "1", "https://www.doclink.com/doc3", "https://www.resizeddoclink.com/doc3");

        SscsDocument otherPartySscsDocumentOtherNotIssued4 = buildSscsDocument(NO, "test2.pdf", documentType.getValue(), "2", "https://www.doclink.com/doc4", "https://www.resizeddoclink.com/doc4");

        SscsDocument otherPartySscsDocumentOtherNotIssued5 = buildSscsDocument(NO, "test.pdf", documentType.getValue(), "2", "https://www.doclink.com/doc5", "https://www.resizeddoclink.com/doc5");

        SscsDocument otherPartySscsDocumentOtherIssued6 = buildSscsDocument(YES, "test2.pdf", documentType.getValue(), "2", "https://www.doclink.com/doc6", "https://www.resizeddoclink.com/doc6");

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

        var updatedCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build();

        SscsDocument otherPartySscsDocumentWithoutResizedLink1 = buildSscsDocument("https://www.doclink.com/doc1");
        SscsDocument otherPartySscsDocumentWithoutResizedLink2 = buildSscsDocument("https://www.doclink.com/doc2");
        SscsDocument otherPartySscsDocumentWithoutResizedLink3 = buildSscsDocument("https://www.doclink.com/doc3");
        SscsDocument otherPartySscsDocumentWithoutResizedLink4 = buildSscsDocument("https://www.doclink.com/doc4");
        SscsDocument otherPartySscsDocumentWithoutResizedLink5 = buildSscsDocument("https://www.doclink.com/doc5");
        SscsDocument otherPartySscsDocumentWithoutResizedLink6 = buildSscsDocument("https://www.doclink.com/doc6");

        updatedCaseDetails.getData().setSscsDocument(List.of(
                otherPartySscsDocumentWithoutResizedLink1,
                otherPartySscsDocumentWithoutResizedLink2,
                otherPartySscsDocumentWithoutResizedLink3,
                otherPartySscsDocumentWithoutResizedLink4,
                otherPartySscsDocumentWithoutResizedLink5,
                otherPartySscsDocumentWithoutResizedLink6
        ));

        functionArgumentCaptor.getValue().apply(updatedCaseDetails);

        updatedCaseDetails.getData().getSscsDocument().forEach(sscsDocument -> {
            assertEquals("Yes", sscsDocument.getValue().getEvidenceIssued());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentUrl());
            assertNotNull(sscsDocument.getValue().getResizedDocumentLink().getDocumentBinaryUrl());
        });
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

    private SscsDocument buildSscsDocument(YesNo yesNo, String fileName, String documentType, String originalSenderOtherPartyId, String documentUrl, String resizedDocumentUrl) {
        SscsDocumentDetails docDetails = SscsDocumentDetails.builder().evidenceIssued(yesNo.getValue())
                .documentType(documentType)
                .originalSenderOtherPartyId(originalSenderOtherPartyId)
                .documentLink(
                        DocumentLink
                                .builder()
                                .documentUrl(documentUrl)
                                .documentFilename(fileName)
                                .documentBinaryUrl(documentUrl + "/binary")
                                .build()
                ).resizedDocumentLink(
                        DocumentLink
                                .builder()
                                .documentUrl(resizedDocumentUrl)
                                .documentFilename(fileName)
                                .documentBinaryUrl(resizedDocumentUrl + "/binary")
                                .build()
                )
                .build();
        return SscsDocument.builder().value(docDetails).build();
    }

    private SscsDocument buildSscsDocument(String documentUrl) {
        SscsDocumentDetails docDetails = SscsDocumentDetails.builder().evidenceIssued(NO.getValue())
                .documentLink(
                        DocumentLink
                                .builder()
                                .documentUrl(documentUrl)
                                .documentBinaryUrl(documentUrl + "/binary")
                                .build()
                )
                .build();
        return SscsDocument.builder().value(docDetails).build();
    }
}
