// package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;
//
// import static java.util.Objects.requireNonNull;
// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyInt;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.ArgumentMatchers.nullable;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.verifyNoInteractions;
// import static org.mockito.Mockito.when;
// import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
// import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
// import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
// import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DECISION_ISSUED;
// import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_ADJOURNMENT_NOTICE;
// import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ISSUE_GENERIC_LETTER;
// import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.NON_COMPLIANT;
// import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
// import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
// import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
// import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
// import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildJointParty;
// import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildOtherParty;
// import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY;
// import static uk.gov.hmcts.reform.sscs.model.PartyItemList.OTHER_PARTY_REPRESENTATIVE;
//
// import java.io.IOException;
// import java.util.EnumMap;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import lombok.extern.slf4j.Slf4j;
// import org.apache.commons.io.IOUtils;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Captor;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
// import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
// import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
// import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
// import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
// import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
// import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
// import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
// import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
// import uk.gov.hmcts.reform.sscs.ccd.domain.OtherPartySelectionDetails;
// import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
// import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
// import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
// import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
// import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
// import uk.gov.hmcts.reform.sscs.evidenceshare.service.BulkPrintService;
// import uk.gov.hmcts.reform.sscs.evidenceshare.service.CcdNotificationService;
// import uk.gov.hmcts.reform.sscs.evidenceshare.service.CoverLetterService;
// import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.GenericLetterPlaceholderService;
//
// @ExtendWith(MockitoExtension.class)
// @Slf4j
// class IssueGenericLetterHandlerTest {
//     @Mock
//     private GenericLetterPlaceholderService genericLetterPlaceholderService;
//
//     @Mock
//     private IssueGenericLetterHandler handler;
//
//     @Mock
//     private BulkPrintService bulkPrintService;
//
//     @Mock
//     CoverLetterService coverLetterService;
//
//     @Mock
//     CcdNotificationService ccdNotificationService;
//
//     @Captor
//     ArgumentCaptor<String> argumentCaptor;
//
//     @Captor
//     ArgumentCaptor<List<Pdf>> documentsCaptor;
//
//     private final Map<LanguagePreference, Map<String, Map<String, String>>> template = new EnumMap<>(LanguagePreference.class);
//
//     private final byte[] letter = new byte[1];
//
//     @BeforeEach
//     void setup() {
//         Map<String, String> nameMap = new HashMap<>();
//         nameMap.put("name", "TB-SCS-LET-ENG-Issue-Generic-Letter.docx");
//         nameMap.put("cover", "TB-SCS-LET-ENG-Cover-Sheet.docx");
//         Map<String, Map<String, String>> englishDocs = new HashMap<>();
//         englishDocs.put("generic-letter", nameMap);
//         template.put(LanguagePreference.ENGLISH, englishDocs);
//
//         DocmosisTemplateConfig docmosisTemplateConfig = new DocmosisTemplateConfig();
//         docmosisTemplateConfig.setTemplate(template);
//
//         handler = new IssueGenericLetterHandler(bulkPrintService, genericLetterPlaceholderService, coverLetterService,
//             docmosisTemplateConfig);
//     }
//
//     @Test
//     void shouldReturnFalse_givenANonQualifyingCallbackType() {
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(),
//             READY_TO_LIST,
//             NON_COMPLIANT);
//
//         boolean result = handler.canHandle(ABOUT_TO_SUBMIT, callback);
//
//         assertThat(result).isFalse();
//     }
//
//     @Test
//     void shouldReturnFalse_givenANonQualifyingEvent() {
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().build(),
//             READY_TO_LIST,
//             DECISION_ISSUED);
//
//         boolean result = handler.canHandle(SUBMITTED, callback);
//
//         assertThat(result).isFalse();
//     }
//
//     @Test
//     void shouldThrowException_givenCallbackIsNull() {
//         assertThatThrownBy(() -> handler.canHandle(SUBMITTED, null))
//             .isInstanceOf(NullPointerException.class);
//     }
//
//     @Test
//     void shouldThrowExceptionInHandler_givenCallbackIsNull() {
//         SscsCaseData caseData = buildCaseData();
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_ADJOURNMENT_NOTICE);
//
//         assertThatThrownBy(() -> handler.handle(MID_EVENT, callback))
//             .isInstanceOf(IllegalStateException.class);
//
//         assertThatThrownBy(() -> handler.handle(SUBMITTED, callback))
//             .isInstanceOf(IllegalStateException.class);
//     }
//
//     @Test
//     void shouldSendLettersToAllPartiesWhenAllPartiesSelected() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToAllParties(YesNo.YES);
//
//         var jointParty = buildJointParty();
//         caseData.setJointParty(jointParty);
//
//         var otherParty = new CcdValue<>(buildOtherParty());
//
//         var otherPartyWithRep = buildOtherParty();
//         Representative representative = Representative.builder()
//                                                       .hasRepresentative("YES")
//                                                       .name(Name
//                                                           .builder()
//                                                           .firstName("OPRepFirstName")
//                                                           .lastName("OPRepLastName")
//                                                           .build())
//                                                       .build();
//         otherPartyWithRep.setRep(representative);
//
//         caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
//         caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(APPELLANT_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
//         when(coverLetterService.generateCoverSheet(anyString(), eq("coversheet"), eq(Map.of()))).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(5)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("User Test", "Wendy Giles", "Joint Party", "Other Party",
//             "OPRepFirstName OPRepLastName");
//     }
//
//     @Test
//     void shouldSendToSelectedParties() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.YES);
//         caseData.setSendToJointParty(YesNo.YES);
//         caseData.setSendToOtherParties(YesNo.YES);
//         caseData.setSendToRepresentative(YesNo.YES);
//
//         var jointParty = buildJointParty();
//         caseData.setJointParty(jointParty);
//
//         var otherParty = new CcdValue<>(buildOtherParty());
//
//         var otherPartyWithRep = buildOtherParty();
//         Representative representative = Representative.builder()
//                                                       .hasRepresentative("YES")
//                                                       .name(Name
//                                                           .builder()
//                                                           .firstName("OPRepFirstName")
//                                                           .lastName("OPRepLastName")
//                                                           .build())
//                                                       .build();
//         otherPartyWithRep.setRep(representative);
//
//         caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
//         caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(
//             APPELLANT_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(5)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("User Test", "Wendy Giles", "Joint Party", "Other Party",
//             "OPRepFirstName OPRepLastName");
//     }
//
//     @Test
//     void shouldNotSendLettersWhenNoPartiesSelected() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.NO);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//
//         var jointParty = buildJointParty();
//         caseData.setJointParty(jointParty);
//
//         var otherParty = new CcdValue<>(buildOtherParty());
//
//         var otherPartyWithRep = buildOtherParty();
//         Representative representative = Representative.builder()
//                                                       .hasRepresentative("YES")
//                                                       .name(Name
//                                                           .builder()
//                                                           .firstName("OPRepFirstName")
//                                                           .lastName("OPRepLastName")
//                                                           .build())
//                                                       .build();
//         otherPartyWithRep.setRep(representative);
//
//         caseData.setOtherParties(List.of(otherParty, new CcdValue<>(otherPartyWithRep)));
//         caseData.setOtherPartySelection(buildOtherPartiesSelection(otherParty, representative));
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verifyNoInteractions(ccdNotificationService);
//         verifyNoInteractions(bulkPrintService);
//     }
//
//
//     @Test
//     void shouldLogErrorWhenIdIsEmpty() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToAllParties(YesNo.YES);
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(ccdNotificationService, times(0)).storeNotificationLetterIntoCcd(any(), any(), any(), any());
//         verify(bulkPrintService, times(2)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("User Test", "Wendy Giles");
//     }
//
//     @Test
//     void shouldBundleCoverLetter() throws IOException {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.YES);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//
//         byte[] pdfBytes = IOUtils.toByteArray(
//             requireNonNull(getClass().getClassLoader().getResourceAsStream("myPdf.pdf")));
//         when(coverLetterService.generateCoverLetterRetry(eq(APPELLANT_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(pdfBytes);
//         when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(pdfBytes);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(1)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("User Test");
//     }
//
//     @Test
//     void shouldReturnLatestDispatchPriority() {
//         assertThat(handler.getPriority()).isEqualTo(DispatchPriority.LATEST);
//     }
//
//     @Test
//     void shouldIncludeSelectedDocumentsWhenAddDocumentsIsYes() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setAddDocuments(YesNo.YES);
//         caseData.setSendToApellant(YesNo.YES);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//
//         Pdf selectedDocument = new Pdf(new byte[]{2}, "selected.pdf");
//         when(coverLetterService.getSelectedDocuments(caseData)).thenReturn(List.of(selectedDocument));
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(APPELLANT_LETTER), eq("TB-SCS-LET-ENG-Issue-Generic-Letter.docx"), anyString(), any(), anyInt())).thenReturn(letter);
//         when(coverLetterService.generateCoverSheet(eq("TB-SCS-LET-ENG-Cover-Sheet.docx"), anyString(), any())).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), documentsCaptor.capture(), eq(ISSUE_GENERIC_LETTER), eq("User Test"));
//
//         List<Pdf> sentDocuments = documentsCaptor.getValue();
//         assertThat(sentDocuments).hasSize(2).contains(selectedDocument);
//
//         Pdf coverLetterPdf = sentDocuments.stream().filter(pdf -> !pdf.equals(selectedDocument)).findFirst().orElseThrow();
//         assertThat(coverLetterPdf.getName()).startsWith("Generic Letter to null ");
//     }
//
//     @Test
//     void shouldNotSendLetterWhenOtherPartyCannotBeFound() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.NO);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.YES);
//
//         caseData.setOtherParties(List.of(new CcdValue<>(buildOtherParty())));
//
//         var unknownItem = new DynamicListItem(OTHER_PARTY.getCode() + "unknown-id", "test");
//         var unknownList = new DynamicList(unknownItem, List.of());
//         caseData.setOtherPartySelection(List.of(new CcdValue<>(new OtherPartySelectionDetails(unknownList))));
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verifyNoInteractions(bulkPrintService);
//     }
//
//     @Test
//     void shouldSendOtherPartyLetterNotRepLetterWhenOtherPartySelectedByOwnId() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.NO);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.YES);
//
//         var otherPartyWithRep = buildOtherParty();
//         Representative representative = Representative.builder()
//                                                       .hasRepresentative("YES")
//                                                       .name(Name
//                                                           .builder()
//                                                           .firstName("OPRepFirstName")
//                                                           .lastName("OPRepLastName")
//                                                           .build())
//                                                       .build();
//         otherPartyWithRep.setRep(representative);
//
//         caseData.setOtherParties(List.of(new CcdValue<>(otherPartyWithRep)));
//
//         var item = new DynamicListItem(OTHER_PARTY.getCode() + otherPartyWithRep.getId(), "test");
//         var list = new DynamicList(item, List.of());
//         caseData.setOtherPartySelection(List.of(new CcdValue<>(new OtherPartySelectionDetails(list))));
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(OTHER_PARTY_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
//         when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(1)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("Other Party");
//     }
//
//     @Test
//     void shouldNotSendLetterWhenOtherPartyWithRepresentativeSelectionIdDoesNotMatch() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.NO);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.YES);
//
//         var otherPartyWithRep = buildOtherParty();
//         Representative representative = Representative.builder()
//                                                       .hasRepresentative("YES")
//                                                       .name(Name
//                                                           .builder()
//                                                           .firstName("OPRepFirstName")
//                                                           .lastName("OPRepLastName")
//                                                           .build())
//                                                       .build();
//         otherPartyWithRep.setRep(representative);
//
//         caseData.setOtherParties(List.of(new CcdValue<>(otherPartyWithRep)));
//
//         var unknownItem = new DynamicListItem(OTHER_PARTY.getCode() + "unknown-id", "test");
//         var unknownList = new DynamicList(unknownItem, List.of());
//         caseData.setOtherPartySelection(List.of(new CcdValue<>(new OtherPartySelectionDetails(unknownList))));
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verifyNoInteractions(bulkPrintService);
//     }
//
//     @Test
//     void shouldSendOtherPartyLetterToAppointeeWhenSelectedByAppointeeId() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToApellant(YesNo.NO);
//         caseData.setSendToJointParty(YesNo.NO);
//         caseData.setSendToRepresentative(YesNo.NO);
//         caseData.setSendToOtherParties(YesNo.YES);
//
//         var otherPartyWithAppointee = buildOtherParty();
//         Appointee appointee = Appointee.builder()
//                                        .name(Name.builder().firstName("AppointeeFirstName").lastName("AppointeeLastName").build())
//                                        .build();
//         otherPartyWithAppointee.setAppointee(appointee);
//         otherPartyWithAppointee.setIsAppointee("Yes");
//
//         caseData.setOtherParties(List.of(new CcdValue<>(otherPartyWithAppointee)));
//
//         var item = new DynamicListItem(OTHER_PARTY.getCode() + appointee.getId(), "test");
//         var list = new DynamicList(item, List.of());
//         caseData.setOtherPartySelection(List.of(new CcdValue<>(new OtherPartySelectionDetails(list))));
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(OTHER_PARTY_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
//         when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(1)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("AppointeeFirstName AppointeeLastName");
//     }
//
//     @Test
//     void shouldSendAllPartiesExceptRepresentativeWhenThereIsNoRepresentative() {
//         SscsCaseData caseData = buildCaseData();
//         caseData.setSendToAllParties(YesNo.YES);
//         caseData.getAppeal().getRep().setHasRepresentative("No");
//
//         when(genericLetterPlaceholderService.populatePlaceholders(eq(caseData), any(), nullable(String.class))).thenReturn(
//             Map.of());
//         when(coverLetterService.generateCoverLetterRetry(eq(APPELLANT_LETTER), anyString(), anyString(), any(), anyInt())).thenReturn(letter);
//         when(coverLetterService.generateCoverSheet(anyString(), anyString(), any())).thenReturn(letter);
//
//         Callback<SscsCaseData> callback = HandlerHelper.buildTestCallbackForGivenData(caseData, READY_TO_LIST,
//             ISSUE_GENERIC_LETTER);
//
//         handler.handle(SUBMITTED, callback);
//
//         verify(bulkPrintService, times(1)).sendLetterToBulkPrintAndSaveAllDocumentsIntoCcdNotification(
//             eq(callback.getCaseDetails().getId()),
//             eq(caseData), any(), eq(ISSUE_GENERIC_LETTER),
//             argumentCaptor.capture());
//         assertThat(argumentCaptor.getAllValues()).containsExactly("User Test");
//     }
//
//     static List<CcdValue<OtherPartySelectionDetails>> buildOtherPartiesSelection(CcdValue<OtherParty> otherParty,
//         Representative representative) {
//         var item1 = new DynamicListItem(OTHER_PARTY.getCode() + otherParty.getValue().getId(), "test");
//         var item2 = new DynamicListItem(OTHER_PARTY_REPRESENTATIVE.getCode() + representative.getId(), "test");
//
//         var list1 = new DynamicList(item1, List.of());
//         CcdValue<OtherPartySelectionDetails> otherParties1 = new CcdValue<>(new OtherPartySelectionDetails(list1));
//
//         var list2 = new DynamicList(item2, List.of());
//         CcdValue<OtherPartySelectionDetails> otherParties2 = new CcdValue<>(new OtherPartySelectionDetails(list2));
//
//         return List.of(otherParties1, otherParties2);
//     }
// }