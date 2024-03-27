package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;

import java.util.*;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.DocmosisTemplateConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.model.PdfDocument;

@RunWith(JUnitParamsRunner.class)
public class FurtherEvidenceServiceTest {

    private static final List<FurtherEvidenceLetterType> ALLOWED_LETTER_TYPES = Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER, DWP_LETTER, JOINT_PARTY_LETTER);

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private CoverLetterService coverLetterService;
    @Mock
    private SscsDocumentService sscsDocumentService;
    @Mock
    private BulkPrintService bulkPrintService;
    @Mock
    private DocmosisTemplateConfig docmosisTemplateConfig;

    private FurtherEvidenceService furtherEvidenceService;

    private SscsCaseData caseData;
    private Pdf pdf;
    private List<Pdf> pdfList;
    private List<PdfDocument> pdfDocumentList;

    private final String furtherEvidenceOriginalSenderTemplateName = "TB-SCS-GNO-ENG-00068.doc";
    private final String furtherEvidenceOriginalSenderWelshTemplateName = "TB-SCS-GNO-WEL-00469.docx";
    private final String furtherEvidenceOriginalSenderDocName = "609-97-template (original sender)";
    private final String furtherEvidenceOtherPartiesTemplateName = "TB-SCS-GNO-ENG-00069.doc";
    private final String furtherEvidenceOtherPartiesWelshTemplateName = "TB-SCS-GNO-WEL-00470.docx";
    private final String furtherEvidenceOtherPartiesDocName = "609-98-template (other parties)";
    private final String furtherEvidenceOtherPartiesDwpDocName = "609-98-template (DWP)";
    Map<LanguagePreference, Map<String, Map<String, String>>> template = new HashMap<>();

    @Before
    public void setup() throws Exception {
        Map<String, String> nameMap;
        Map<String, Map<String, String>> englishDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        englishDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        englishDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00068.doc");
        englishDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00069.doc");
        englishDocs.put("d609-98", nameMap);

        Map<String, Map<String, String>> welshDocs = new HashMap<>();
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00010.doc");
        welshDocs.put(DocumentType.DL6.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-ENG-00011.doc");
        welshDocs.put(DocumentType.DL16.getValue(), nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00469.docx");
        welshDocs.put("d609-97", nameMap);
        nameMap = new HashMap<>();
        nameMap.put("name", "TB-SCS-GNO-WEL-00470.docx");
        welshDocs.put("d609-98", nameMap);

        template.put(LanguagePreference.ENGLISH, englishDocs);
        template.put(LanguagePreference.WELSH, welshDocs);

        furtherEvidenceService = new FurtherEvidenceService(coverLetterService, sscsDocumentService, bulkPrintService,
            docmosisTemplateConfig);

        byte[] pdfBytes = IOUtils.toByteArray(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("myPdf.pdf")));
        pdf = new Pdf(pdfBytes, "some doc name");
        pdfList = Collections.singletonList(pdf);
        pdfDocumentList = Collections.singletonList(PdfDocument.builder().pdf(pdf).document(AbstractDocument.builder().build()).build());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_shouldGenerateCoverLetterOriginalSenderAnd609_98ForDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndNoRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAnd609_98ForDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAnd609_98ForRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAnd609_98ForRepAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_shouldGenerateCoverLetterOriginalSenderAnd609_98ForAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, REPRESENTATIVE_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenRepIssueFurtherEvidenceCallbackWithAppellantRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAnd609_98ForAppellantAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, REPRESENTATIVE_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOriginalSenderWelshTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAnd609_98ForAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER), null);

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellant_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAnd609_98ForAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER), null);

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_shouldGenerateCoverLetterOriginalSenderAnd609_98ForAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER), null);

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), any(), any(), any(), any());
    }

    @Test
    public void givenJointPartyIssueFurtherEvidenceCallbackWithAppellant_shouldGenerateCoverLetterOriginalSenderAnd609_98ForAppellantAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, JOINT_PARTY_EVIDENCE);
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, JOINT_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), null);

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(JOINT_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(JOINT_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Joint Party"));
    }

    @Test
    public void givenOriginalSenderAsOtherPartyAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyAnd609_98ForAppellantThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(false, false, "1", null, null);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "1");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("1"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Harry Kane"));
    }

    @Test
    public void givenOriginalSenderAsOtherPartyAppointeeAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyAppointeeAnd609_98ForAppellantThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, false, "1", "2", null);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "2");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("2"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Henry Smith"));
    }

    @Test
    public void givenOriginalSenderAsOtherPartyRepAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyRepAnd609_98ForAppellantAndOtherPartyThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(false, true, "1", null, "3");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_REPRESENTATIVE_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "3");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("3"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("1"));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Harry Kane"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Wendy Wendy"));
    }

    @Test
    public void givenOriginalSenderAsOtherPartyRepAndThereIsOtherPartyAppointeeIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyRepAnd609_98ForAppellantAndOtherPartyAppointeeThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, true, "1", "2", "3");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_REPRESENTATIVE_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "3");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("3"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("2"));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Henry Smith"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Wendy Wendy"));
    }

    @Test
    public void givenOriginalSenderAsOtherPartyAppointeeAndThereIsOtherPartyRepIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyAppointeeAnd609_98ForAppellantAndOtherPartyRepThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, true, "1", "2", "3");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "2");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("2"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("3"));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Henry Smith"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Wendy Wendy"));
    }

    @Test
    public void givenMultipleOtherPartiesAndOriginalSenderAsOtherPartyAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyAnd609_98ForAppellantAndSecondOtherPartyThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(false, false, "1", null, null);
        withOtherPartyOrRepOrAppointee(false, false, "4", null, null);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "1");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("1"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("4"));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
    }

    @Test
    public void givenMultipleOtherPartiesAndOriginalSenderAsOtherPartyAppointeeAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyAppointeeAnd609_98ForAppellantAndSecondOtherPartyThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, false, "1", "2", null);
        withOtherPartyOrRepOrAppointee(false, false, "4", null, null);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "2");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("2"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("4"));
        then(coverLetterService).should(times(3)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
    }

    @Test
    public void givenMultipleOtherPartiesAndOriginalSenderAsOtherPartyRepAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyRepAnd609_98ForAppellantAndSecondOtherPartyThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, true, "1", "2", "3");
        withOtherPartyOrRepOrAppointee(false, false, "4", null, null);
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_REPRESENTATIVE_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "3");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("3"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("4"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("2"));
        then(coverLetterService).should(times(4)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Wendy Wendy"));
    }

    @Test
    public void givenMultipleOtherPartiesAndOriginalSenderAsOtherPartyAppointeeAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForOtherPartyRepAnd609_98ForAppellantAndAllSecondOtherPartyThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, true, "1", "2", "3");
        withOtherPartyOrRepOrAppointee(true, true, "4", "5", "6");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, OTHER_PARTY_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), "2");

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq("2"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("3"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("5"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("6"));
        then(coverLetterService).should(times(5)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
    }

    @Test
    public void givenMultipleOtherPartiesAndOriginalSenderAsAppellantAndIssueFurtherEvidenceCallback_shouldGenerateCoverLetterForAppellantAnd609_98ForAllOtherPartiesThenBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false, OTHER_PARTY_EVIDENCE);
        withOtherPartyOrRepOrAppointee(true, true, "1", "2", "3");
        withOtherPartyOrRepOrAppointee(true, true, "4", "5", "6");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE,
            Arrays.asList(DWP_LETTER, APPELLANT_LETTER, JOINT_PARTY_LETTER, REPRESENTATIVE_LETTER, OTHER_PARTY_LETTER, OTHER_PARTY_REP_LETTER), null);

        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("2"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("3"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("5"));
        then(coverLetterService).should(times(1)).generateCoverLetter(eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq("6"));
        then(coverLetterService).should(times(5)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(OTHER_PARTY_REP_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), any());
    }

    @Test
    public void givenDwpIssueFurtherEvidenceCallbackWithAppellantAndRep_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterOriginalSenderAnd609_98ForAppellantAndRepAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE,
            Arrays.asList(APPELLANT_LETTER, REPRESENTATIVE_LETTER), null);

        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOriginalSenderWelshTemplateName),
                eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOtherPartiesWelshTemplateName),
                eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(REPRESENTATIVE_LETTER),
                eq(furtherEvidenceOtherPartiesWelshTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(2)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER", "JOINT_PARTY_LETTER"})
    public void givenIssueForParty_shouldGenerateCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType), null);

        String templateName = furtherEvidenceOtherPartiesTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    @Parameters({"OTHER_PARTY_LETTER, 1", "OTHER_PARTY_REP_LETTER, 2"})
    public void givenIssueForOtherParty_shouldGenerateCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType, String expectedOtherPartyId) {
        createTestDataAndConfigureSscsDocumentServiceMock("No", true);
        withRep();
        withOtherPartyOrRepOrAppointee(false, true, "1", null, "2");
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType), null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(expectedOtherPartyId));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    @Parameters({"APPELLANT_LETTER", "REPRESENTATIVE_LETTER", "DWP_LETTER", "JOINT_PARTY_LETTER"})
    public void givenIssueForParty_whenLanguageIsWelsh_shouldGenerateWelshCoverLetterForSelectedParty(FurtherEvidenceLetterType furtherEvidenceLetterType) {
        createTestDataAndConfigureSscsDocumentServiceMock("Yes", false);
        withRep();
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, DWP_EVIDENCE, Collections.singletonList(furtherEvidenceLetterType), null);

        String templateName = furtherEvidenceOtherPartiesWelshTemplateName;
        String docName = furtherEvidenceOtherPartiesDocName;
        if (furtherEvidenceLetterType.equals(DWP_LETTER)) {
            templateName = furtherEvidenceOriginalSenderWelshTemplateName;
            docName = furtherEvidenceOriginalSenderDocName;
        }
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(furtherEvidenceLetterType), eq(templateName), eq(docName), eq(null));
        then(coverLetterService).should(times(1)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), any(), any(), any());
        then(coverLetterService).shouldHaveNoMoreInteractions();
        then(bulkPrintService).shouldHaveNoMoreInteractions();
    }

    @Test
    public void givenAppellantIssueFurtherEvidenceCallbackWithAppellantAndJointParty_shouldGenerateCoverLetterOriginalSenderAnd609_98ForAndDwpAndBulkPrintDocs() {
        createTestDataAndConfigureSscsDocumentServiceMock("No", false);
        withJointParty();
        when(docmosisTemplateConfig.getTemplate()).thenReturn(template);
        furtherEvidenceService.issue(caseData.getSscsDocument(), caseData, APPELLANT_EVIDENCE, ALLOWED_LETTER_TYPES, null);

        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(APPELLANT_LETTER), eq(furtherEvidenceOriginalSenderTemplateName), eq(furtherEvidenceOriginalSenderDocName), eq(null));
        then(coverLetterService).should(times(1))
            .generateCoverLetter(eq(caseData), eq(JOINT_PARTY_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDocName), eq(null));
        then(coverLetterService).should(times(0))
            .generateCoverLetter(eq(caseData), eq(DWP_LETTER), eq(furtherEvidenceOtherPartiesTemplateName), eq(furtherEvidenceOtherPartiesDwpDocName), eq(null));
        then(coverLetterService).should(times(2)).appendCoverLetter(any(), anyList(), any());
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(APPELLANT_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("ApFirstname ApLastname"));
        then(bulkPrintService).should(times(1)).sendToBulkPrint(eq(pdfList), eq(caseData), eq(JOINT_PARTY_LETTER), eq(EventType.ISSUE_FURTHER_EVIDENCE), eq("Joint Party"));
    }

    private void withJointParty() {
        caseData.getJointParty().setHasJointParty(YES);
        caseData.getJointParty().setName(Name.builder().lastName("Party").firstName("Joint").build());
        caseData.getJointParty().setJointPartyAddressSameAsAppellant(YES);
    }

    private void withOtherPartyOrRepOrAppointee(boolean withAppointee, boolean withRep, String otherPartyId, String otherPartyAppointeeId, String otherPartyRepId) {
        CcdValue<OtherParty> otherParty = CcdValue.<OtherParty>builder()
            .value(OtherParty.builder()
                .id(otherPartyId)
                .name(Name.builder().firstName("Harry").lastName("Kane").build())
                .isAppointee(NO.getValue())
                .build())
            .build();

        if (withAppointee) {
            otherParty.getValue().setIsAppointee(YES.getValue());
            otherParty.getValue().setAppointee(Appointee.builder().id(otherPartyAppointeeId).name(Name.builder().firstName("Henry").lastName("Smith").build()).build());
        }

        if (withRep) {
            otherParty.getValue().setRep(Representative.builder().id(otherPartyRepId).hasRepresentative(YES.getValue()).name(Name.builder().firstName("Wendy").lastName("Wendy").build()).build());
        }

        List<CcdValue<OtherParty>> otherParties = new ArrayList<>();
        if (caseData.getOtherParties() != null && !caseData.getOtherParties().isEmpty()) {
            otherParties = caseData.getOtherParties();
        }

        otherParties.add(otherParty);
        caseData.setOtherParties(otherParties);
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock(String languagePreferenceFlag, boolean isConfidentialCase) {
        createTestDataAndConfigureSscsDocumentServiceMock(languagePreferenceFlag, isConfidentialCase, APPELLANT_EVIDENCE);
    }

    private void createTestDataAndConfigureSscsDocumentServiceMock(String languagePreferenceFlag, boolean isConfidentialCase, DocumentType documentType) {
        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(documentType.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        Appellant appellant = Appellant.builder().name(Name.builder().firstName("ApFirstname").lastName("ApLastname").build()).build();

        caseData = SscsCaseData.builder()
            .ccdCaseId("1563382899630221")
            .languagePreferenceWelsh(languagePreferenceFlag)
            .isConfidentialCase(isConfidentialCase ? YES : NO)
            .sscsDocument(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued))
            .appeal(Appeal.builder().appellant(appellant).build())
            .build();

        doReturn(pdfDocumentList).when(sscsDocumentService).getPdfsForGivenDocTypeNotIssued(
            eq(Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued)), any(), eq(isConfidentialCase), any());

        when(sscsDocumentService.sizeNormalisePdfs(any())).thenReturn(pdfDocumentList);
    }

    private void withRep() {
        caseData.getAppeal().setRep(Representative.builder().hasRepresentative("Yes").build());
    }

    @Test
    @Parameters(method = "generateDifferentTestScenarios")
    public void givenDocList_shouldBeHandledUnderCertainConditions(List<SscsDocument> documentList,
                                                                   boolean expected) {

        boolean actual = furtherEvidenceService.canHandleAnyDocument(documentList);

        assertEquals(expected, actual);
    }

    @Test
    public void updateSscsCaseDocumentsWhichHaveResizedDocumentsAndMatchingDocTypeAndMatchingDocIdentifier() {
        DocumentLink resizedDocLink = DocumentLink.builder().documentUrl("resized.com").build();

        SscsDocument updatedDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument updatedDoc2 = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original2.com").build())
                    .build()).build();

        SscsDocument updatedDoc3 = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original2.com").build())
                    .build()).build();

        SscsWelshDocument updatedWelshDoc = SscsWelshDocument
            .builder()
            .value(
                SscsWelshDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .resizedDocumentLink(resizedDocLink)
                    .documentLink(DocumentLink.builder().documentBinaryUrl("welsh.com").build())
                    .build()).build();

        SscsWelshDocument originalWelshDoc = SscsWelshDocument
            .builder()
            .value(
                SscsWelshDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("welsh.com").build())
                    .build()).build();

        SscsDocument originalDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument differentTypeDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("original.com").build())
                    .build()).build();

        SscsDocument differentLinkDoc = SscsDocument
            .builder()
            .value(
                SscsDocumentDetails
                    .builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .documentLink(DocumentLink.builder().documentBinaryUrl("not-original2.com").build())
                    .build()).build();

        SscsCaseData caseData = SscsCaseData
            .builder()
            .sscsDocument(Arrays.asList(originalDoc, differentTypeDoc, differentLinkDoc))
            .sscsWelshDocuments(Collections.singletonList(originalWelshDoc))
            .build();

        furtherEvidenceService.updateCaseDocuments(Arrays.asList(updatedDoc, updatedDoc2, updatedDoc3, updatedWelshDoc), caseData, APPELLANT_EVIDENCE);

        assertEquals(resizedDocLink, caseData.getSscsDocument().get(0).getValue().getResizedDocumentLink());
        assertNull(caseData.getSscsDocument().get(1).getValue().getResizedDocumentLink());
        assertNull(caseData.getSscsDocument().get(2).getValue().getResizedDocumentLink());
        assertNull(caseData.getSscsWelshDocuments().get(0).getValue().getResizedDocumentLink());
    }

    @SuppressWarnings("unused")
    private Object[] generateDifferentTestScenarios() {

        SscsDocument sscsDocument1WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument2WithAppellantEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        SscsDocument sscsDocument3WithAppellantEvidenceAndYesIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(APPELLANT_EVIDENCE.getValue())
                .evidenceIssued("Yes")
                .build())
            .build();

        SscsDocument sscsDocument4WithRepEvidenceAndNoIssued = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentType(REPRESENTATIVE_EVIDENCE.getValue())
                .evidenceIssued("No")
                .build())
            .build();

        return new Object[]{
            //happy path sceanrios
            new Object[]{Collections.singletonList(sscsDocument1WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Collections.singletonList(sscsDocument3WithAppellantEvidenceAndYesIssued), false},
            new Object[]{Collections.singletonList(sscsDocument4WithRepEvidenceAndNoIssued), true},

            new Object[]{Arrays.asList(sscsDocument1WithAppellantEvidenceAndNoIssued,
                sscsDocument2WithAppellantEvidenceAndNoIssued), true},
            new Object[]{Arrays.asList(sscsDocument3WithAppellantEvidenceAndYesIssued,
                sscsDocument1WithAppellantEvidenceAndNoIssued), true},

            //edge scenarios
            new Object[]{null, false},
            new Object[]{Collections.singletonList(SscsDocument.builder().build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder().build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .evidenceIssued("No")
                    .documentType(null)
                    .build())
                .build()), false},
            new Object[]{Collections.singletonList(SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentType(APPELLANT_EVIDENCE.getValue())
                    .build())
                .build()), false},
            new Object[]{Arrays.asList(null, sscsDocument1WithAppellantEvidenceAndNoIssued), true}
        };
    }

}
