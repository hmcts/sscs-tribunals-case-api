package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildJointParty;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildOtherParty;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY;
import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY_REPRESENTATIVE;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CcdNotificationService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.GenericLetterPlaceholderService;

@ExtendWith(MockitoExtension.class)
@Slf4j
class IssueGenericLetterHandlerTest {
    @Mock
    private GenericLetterPlaceholderService genericLetterPlaceholderService;

    @Mock
    private IssueGenericLetterHandler handler;

    @Mock
    private BulkPrintService bulkPrintService;

    @Mock
    CoverLetterService coverLetterService;

    @Mock
    CcdNotificationService ccdNotificationService;

    @Captor
    ArgumentCaptor<String> argumentCaptor;

    private Map<LanguagePreference, Map<String, Map<String, String>>> template = new HashMap<>();

    private byte[] letter = new byte[1];

    @BeforeEach
    public void setup() {
        Map<String, String> nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-LET-ENG-Issue-Generic-Letter.docx");
        nameMap.put("cover", "TB-SCS-LET-ENG-Cover-Sheet.docx");
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        englishDocs.put("generic-letter", nameMap);
        template.put(LanguagePreference.ENGLISH, englishDocs);

        DocmosisTemplateConfig docmosisTemplateConfig = new DocmosisTemplateConfig();
        docmosisTemplateConfig.setTemplate(template);

        handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
            docmosisTemplateConfig, true);
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
            DECISION_ISSUED);

        boolean result = handler.canHandle(SUBMITTED, callback);

        Assertions.assertFalse(result);
    }

    @Test
    void shouldReturnFalse_givenFeatureFlagIsFalse() {
        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(),
            READY_TO_LIST,
            ISSUE_GENERIC_LETTER);
        handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
            null, false);

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
    void shouldThrowExceptionInHandler_givenCallbackIsNull() {
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
    void shouldSendLettersToAllPartiesWhenAllPartiesSelected() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToAllParties(YesNo.YES);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = Representative.builder()
            .hasRepresentative("YES")
            .name(Name.builder().firstName("OPRepFirstName").lastName("OPRepLastName").build())
            .build();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
        when(coverLetterService.generateCoverSheet(anyString(), eq("coversheet"), eq(Map.of()))).thenReturn(letter);

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(5)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
            argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getAllValues(), List.of("User Test", "Wendy Giles", "Joint Party", "Other Party", "OPRepFirstName OPRepLastName"));
    }

    @Test
    void shouldSendToSelectedParties() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.YES);
        caseData.setSendToJointParty(YesNo.YES);
        caseData.setSendToOtherParties(YesNo.YES);
        caseData.setSendToRepresentative(YesNo.YES);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = Representative.builder()
            .hasRepresentative("YES")
            .name(Name.builder().firstName("OPRepFirstName").lastName("OPRepLastName").build())
            .build();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(letter);

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(5)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
            argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getAllValues(), List.of("User Test", "Wendy Giles", "Joint Party", "Other Party", "OPRepFirstName OPRepLastName"));
    }

    @Test
    void shouldNotSendLettersWhenNoPartiesSelected() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.NO);
        caseData.setSendToJointParty(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.NO);
        caseData.setSendToRepresentative(YesNo.NO);

        var jointParty = buildJointParty();
        caseData.setJointParty(jointParty);

        var otherParty = new CcdValue<>(buildOtherParty());

        var otherPartyWithRep = buildOtherParty();
        Representative representative = Representative.builder()
            .hasRepresentative("YES")
            .name(Name.builder().firstName("OPRepFirstName").lastName("OPRepLastName").build())
            .build();
        otherPartyWithRep.setRep(representative);

        caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
        caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verifyNoInteractions(ccdNotificationService);
        verifyNoInteractions(bulkPrintService);
    }


    @Test
    void shouldLogErrorWhenIdIsEmpty() {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToAllParties(YesNo.YES);

        when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(Map.of());

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(ccdNotificationService, times(0)).storeNotificationLetterIntoCcd(any(), any(), any(), any());
        verify(bulkPrintService, times(2)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
            argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getAllValues(), List.of("User Test", "Wendy Giles"));
    }

    @Test
    void shouldBundleCoverLetter() throws IOException {
        SscsCaseData caseData = buildCaseData();
        caseData.setSendToApellant(YesNo.YES);
        caseData.setSendToJointParty(YesNo.NO);
        caseData.setSendToOtherParties(YesNo.NO);
        caseData.setSendToRepresentative(YesNo.NO);

        byte[] pdfBytes = IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("myPdf.pdf"));
        when(coverLetterService.generateCoverLetterRetry(any(), anyString(), anyString(), any(), anyInt())).thenReturn(pdfBytes);
        when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(pdfBytes);

        Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST, ISSUE_GENERIC_LETTER);

        handler.handle(SUBMITTED, callback);

        verify(bulkPrintService, times(1)).sendToBulkPrint(eq(callback.getCaseDetails().getId()),
            eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
            argumentCaptor.capture());
        Assertions.assertEquals(argumentCaptor.getAllValues(), List.of("User Test"));
    }

    static List<CcdValue<OtherPartySelectionDetails>> buildOtherPartiesSelection(CcdValue<OtherParty> otherParty, Representative representative) {
        var item1 = new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getValue().getId(), "test");
        var item2 = new DynamicListItem(OTHER_PARTY_REPRESENTATIVE.getCode() + representative.getId(), "test");

        var list1 = new DynamicList(item1, List.of());
        CcdValue<OtherPartySelectionDetails> otherParties1 = new CcdValue<>(new OtherPartySelectionDetails(list1));

        var list2 = new DynamicList(item2, List.of());
        CcdValue<OtherPartySelectionDetails> otherParties2 = new CcdValue<>(new OtherPartySelectionDetails(list2));

        return List.of(otherParties1, otherParties2);
    }
}
