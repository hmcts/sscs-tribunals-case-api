package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildJointParty;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildOtherParty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.SorPlaceholderService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@ExtendWith(MockitoExtension.class)
@Slf4j
public class SorWriteHandlerTest {
    @Mock
    private SorWriteHandler handler;

    @Mock
    private SorPlaceholderService sorPlaceholderService;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    private CoverLetterService coverLetterService;

    @Mock
    PdfStoreService pdfStoreService;

    @Captor
    ArgumentCaptor<String> argumentCaptor;

    @BeforeEach
    public void setup() {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("name", "B-SCS-LET-ENG-Statement-Of-Reasons-Outcome.docx");
        nameMap.put("cover", "TB-SCS-GNO-ENG-00012.docx");
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        englishDocs.put(POST_HEARING_APP_SOR_WRITTEN.getCcdType(), nameMap);
        Map<LanguagePreference, Map<String, Map<String, String>>> template = new HashMap<>();
        template.put(LanguagePreference.ENGLISH, englishDocs);

        DocmosisTemplateConfig docmosisTemplateConfig = new DocmosisTemplateConfig();
        docmosisTemplateConfig.setTemplate(template);

        handler = new SorWriteHandler(docmosisTemplateConfig, sorPlaceholderService, bulkPrintService,
            coverLetterService, pdfStoreService);
    }

    @Test
    void shouldReturnFalse_givenANonQualifyingCallbackType() {
        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            NON_COMPLIANT);

        boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalse_givenANonQualifyingEvent() {
        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            ISSUE_ADJOURNMENT_NOTICE);

        boolean result = handler.canHandle(SUBMITTED, callback);
        Assertions.assertFalse(result);
    }

    @Test
    void shouldThrowException_givenCallbackIsNull() {
        assertThrows(NullPointerException.class, () ->
            handler.canHandle(SUBMITTED, null)
        );
    }

    @Test
    void shouldThrowExceptionInHandler_givenNonValidCallback() {
        SscsCaseData caseData = buildCaseData();
        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_ADJOURNMENT_NOTICE);

        assertThrows(IllegalStateException.class, () ->
            handler.handle(MID_EVENT, callback)
        );

        assertThrows(IllegalStateException.class, () ->
            handler.handle(SUBMITTED, callback)
        );
    }

    @Test
    void checkJointPartyAndAppointeeNameReturnsGivenCaseData() {
        SscsCaseData caseData = buildCaseData();
        caseData.setCcdCaseId("1");

        var sorDocumentDetails = SscsDocumentDetails.builder()
            .documentType(DocumentType.STATEMENT_OF_REASONS.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url").build())
            .build();
        var sorDocument = SscsDocument.builder()
            .value(sorDocumentDetails)
            .build();
        caseData.setSscsDocument(List.of(sorDocument));
        caseData.getAppeal().getAppellant().setIsAppointee(YesNo.YES.getValue());
        caseData.getAppeal().getRep().setHasRepresentative(YesNo.NO.getValue());

        var jointParty = buildJointParty();
        jointParty.setHasJointParty(YesNo.YES);
        caseData.setJointParty(jointParty);


        Map<String, Object> placeholders1 = new HashMap<>();
        placeholders1.put(PlaceholderConstants.NAME, caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), anyString(), eq(null))).thenReturn(placeholders1);

        Map<String, Object> placeholders2 = new HashMap<>();
        placeholders2.put(PlaceholderConstants.NAME, jointParty.getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(FurtherEvidenceLetterType.JOINT_PARTY_LETTER), anyString(), eq(null))).thenReturn(placeholders2);
        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, POST_HEARING_APP_SOR_WRITTEN);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(2)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(POST_HEARING_APP_SOR_WRITTEN),
            argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getAllValues(), List.of(
            caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle(),
            jointParty.getName().getFullNameNoTitle()));
    }

    @Ignore
    @Test
    void checkOtherPartyNameReturns() { //might not need these and above tests, only need to test if correct letter gets generated for each party as party name test is already created
        SscsCaseData caseData = buildCaseData();
        caseData.setCcdCaseId("1");

        var sorDocumentDetails = SscsDocumentDetails.builder()
            .documentType(DocumentType.STATEMENT_OF_REASONS.getValue())
            .documentLink(DocumentLink.builder().documentUrl("url").build())
            .build();
        var sorDocument = SscsDocument.builder()
            .value(sorDocumentDetails)
            .build();
        caseData.setSscsDocument(List.of(sorDocument));

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = Representative.builder()
            .hasRepresentative(YES)
            .name(Name.builder().firstName("OPRepFirstName").lastName("OPRepLastName").build())
            .build();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));

        JointParty jointParty = JointParty.builder().name(Name.builder().firstName("James").lastName("Smith").build()).build();
        caseData.setJointParty(jointParty);

        Map<String, Object> appealentPlaceholders = new HashMap<>();
        appealentPlaceholders.put(PlaceholderConstants.NAME, caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), anyString(), eq(null))).thenReturn(appealentPlaceholders);

        Map<String, Object> otherPartyRepPlaceholders = new HashMap<>();
        otherPartyRepPlaceholders.put(PlaceholderConstants.NAME, otherPartyWithRep.getRep().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER), anyString(), anyString())).thenReturn(otherPartyRepPlaceholders);

        Map<String, Object> repPlaceHolders = new HashMap<>();
        repPlaceHolders.put(PlaceholderConstants.NAME, caseData.getAppeal().getRep().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(FurtherEvidenceLetterType.REPRESENTATIVE_LETTER), anyString(), eq(null))).thenReturn(repPlaceHolders);


        Map<String, Object> otherPartyPlaceholders = new HashMap<>();
        otherPartyPlaceholders.put(PlaceholderConstants.NAME, otherParty.getValue().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(FurtherEvidenceLetterType.OTHER_PARTY_LETTER), anyString(), anyString())).thenReturn(otherPartyPlaceholders);

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, POST_HEARING_APP_SOR_WRITTEN);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(5)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(POST_HEARING_APP_SOR_WRITTEN),
            argumentCaptor.capture());
        Assertions.assertEquals(List.of(
                caseData.getAppeal().getAppellant().getName().getFullNameNoTitle(),
                caseData.getAppeal().getRep().getName().getFullNameNoTitle(),
                otherParty.getValue().getName().getFullNameNoTitle(),
                otherParty.getValue().getName().getFullNameNoTitle(),
                otherPartyWithRep.getRep().getName().getFullNameNoTitle()),
            argumentCaptor.getAllValues());
    }


    @Test
    void checkAppellantNameReturns() {
        SscsCaseData caseData = buildCaseData();
        caseData.setCcdCaseId("1");

        var sorDocumentDetails = SscsDocumentDetails.builder()
            .documentType(DocumentType.STATEMENT_OF_REASONS.getValue())
            .documentLink(DocumentLink.builder().build())
            .build();
        var sorDocument = SscsDocument.builder()
            .value(sorDocumentDetails)
            .build();
        caseData.setSscsDocument(List.of(sorDocument));
        caseData.getAppeal().setRep(null);

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put(PlaceholderConstants.NAME, caseData.getAppeal().getAppellant().getName().getFullNameNoTitle());
        when(sorPlaceholderService.populatePlaceholders(eq(caseData), eq(APPELLANT_LETTER), anyString(), eq(null))).thenReturn(placeholders);

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, POST_HEARING_APP_SOR_WRITTEN);
        handler.handle(SUBMITTED, callback);

        verify(coverLetterService).generateCoverLetterRetry(eq(APPELLANT_LETTER), any(), any(), any(), eq(1));
        verify(bulkPrintService, times(1)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(POST_HEARING_APP_SOR_WRITTEN),
            eq(caseData.getAppeal().getAppellant().getName().getFullNameNoTitle()));
    }

}
