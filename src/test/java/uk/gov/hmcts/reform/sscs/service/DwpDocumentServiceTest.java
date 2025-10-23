package uk.gov.hmcts.reform.sscs.service;

import static java.time.LocalDateTime.now;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpResponseDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@ExtendWith(MockitoExtension.class)
public class DwpDocumentServiceTest {

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;

    @BeforeEach
    public void setUp() {
        dwpDocumentService = new DwpDocumentService();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

    }

    @Test
    public void givenNoDwpDocument_thenDwpUploadedCollectionIsUpdated() {
        DwpDocument dwpResponseDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("response.pdf")
                        .documentBinaryUrl("/responsebinaryurl").documentUrl("/responseurl").build())
                .documentType(DwpDocumentType.DWP_RESPONSE.getValue()).build()).build();
        DwpDocument dwpAt38Document = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("at38.pdf")
                        .documentBinaryUrl("/binaryurl").documentUrl("/url").build())
                .documentType(DwpDocumentType.AT_38.getValue()).build()).build();
        DwpDocument dwpEvidenceDocument = DwpDocument.builder().value(DwpDocumentDetails.builder()
                .documentLink(DocumentLink.builder().documentFilename("evidence.pdf")
                        .documentBinaryUrl("/evidencebinaryurl").documentUrl("/evidenceurl").build())
                .documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).build()).build();
        SscsCaseData caseData = SscsCaseData.builder()
                .dwpResponseDocument(new DwpResponseDocument(dwpResponseDocument.getValue().getDocumentLink(),
                                dwpResponseDocument.getValue().getDocumentFileName()))
                .dwpAT38Document(new DwpResponseDocument(dwpAt38Document.getValue().getDocumentLink(),
                        dwpAt38Document.getValue().getDocumentFileName()))
                .dwpEvidenceBundleDocument(new DwpResponseDocument(dwpEvidenceDocument.getValue().getDocumentLink(),
                        dwpEvidenceDocument.getValue().getDocumentFileName())).build();

        dwpDocumentService.moveDocsToCorrectCollection(caseData);

        String todayDate = now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        assertThat(caseData.getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("/evidenceurl")),
                                hasProperty("documentBinaryUrl", is("/evidencebinaryurl")),
                                hasProperty("documentFilename",
                                        is("FTA evidence received on " + todayDate + ".pdf"))
                        ))
                ))
        ));

        assertThat(caseData.getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("/responseurl")),
                                hasProperty("documentBinaryUrl", is("/responsebinaryurl")),
                                hasProperty("documentFilename",
                                        is("FTA response received on " + todayDate + ".pdf"))
                        ))
                ))
        ));

        assertThat(caseData.getDwpDocuments(), hasItem(
                hasProperty("value", allOf(
                        hasProperty("documentLink", allOf(
                                hasProperty("documentUrl", is("/url")),
                                hasProperty("documentBinaryUrl", is("/binaryurl")),
                                hasProperty("documentFilename", is("AT38 received on " + todayDate + ".pdf"))
                        )),
                        hasProperty("documentFileName", is("AT38 received on " + todayDate))
                ))
        ));

        assertNull(caseData.getDwpResponseDocument());
        assertNull(caseData.getDwpAT38Document());
        assertNull(caseData.getDwpEvidenceBundleDocument());
    }

    @Test
    public void givenCaseWithEmptyDwpDocuments_thenMoveDocToDwpDocuments() {
        dwpDocumentService.addToDwpDocuments(sscsCaseData, DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("test.url").build()).build(), DwpDocumentType.UCB);

        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void givenCaseWithExistingDwpDocuments_thenAddDocToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder().dwpDocuments(dwpDocuments).build();

        dwpDocumentService.addToDwpDocuments(sscsCaseData, DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("test.url").build()).build(), DwpDocumentType.UCB);

        assertEquals("existing.com", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl());
    }

    @Test
    public void givenCaseWithExistingDwpDocumentsAndEditedDocIsAdded_thenAddOriginalAndEditedDocsToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder().dwpDocuments(dwpDocuments).build();

        dwpDocumentService.addToDwpDocumentsWithEditedDoc(sscsCaseData, DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("test.url").build()).build(), DwpDocumentType.UCB, DocumentLink.builder().documentUrl("edited.url").build(), "Edited reason");

        assertEquals("existing.com", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("edited.url", sscsCaseData.getDwpDocuments().get(1).getValue().getEditedDocumentLink().getDocumentUrl());
        assertEquals("Edited reason", sscsCaseData.getDwpDocuments().get(1).getValue().getDwpEditedEvidenceReason());
    }

    @Test
    public void givenDwpResponseDocWithExistingDocumentName_thenMoveToDwpDocumentsWithDocumentName() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_RESPONSE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder()
                .dwpResponseDocument(DwpResponseDocument.builder().documentFileName("My filename").documentLink(DocumentLink.builder().documentUrl("test.url").build()).build())
                .dwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("edited.url").build()).build())
                .dwpEditedEvidenceReason("Edited reason")
                .dwpDocuments(dwpDocuments).build();

        dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);

        assertEquals(1, sscsCaseData.getDwpDocuments().size());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("edited.url", sscsCaseData.getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentUrl());
        assertEquals("Edited reason", sscsCaseData.getDwpDocuments().get(0).getValue().getDwpEditedEvidenceReason());
        assertEquals(DwpDocumentType.DWP_RESPONSE.getValue(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("My filename", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpResponseDoc_thenMoveToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_RESPONSE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder()
                .dwpResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("test.url").build()).build())
                .dwpEditedResponseDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("edited.url").build()).build())
                .dwpEditedEvidenceReason("Edited reason")
                .dwpDocuments(dwpDocuments).build();

        dwpDocumentService.moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);

        assertEquals(1, sscsCaseData.getDwpDocuments().size());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("edited.url", sscsCaseData.getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentUrl());
        assertEquals("Edited reason", sscsCaseData.getDwpDocuments().get(0).getValue().getDwpEditedEvidenceReason());
        assertEquals(DwpDocumentType.DWP_RESPONSE.getValue(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals(DwpDocumentType.DWP_RESPONSE.getLabel(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceBundleWithExistingDocumentName_thenMoveToDwpDocumentsWithDocumentName() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder()
                .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentFileName("My filename").documentLink(DocumentLink.builder().documentUrl("test.url").build()).build())
                .dwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("edited.url").build()).build())
                .dwpEditedEvidenceReason("Edited reason")
                .dwpDocuments(dwpDocuments).build();

        dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);

        assertEquals(1, sscsCaseData.getDwpDocuments().size());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("edited.url", sscsCaseData.getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentUrl());
        assertEquals("Edited reason", sscsCaseData.getDwpDocuments().get(0).getValue().getDwpEditedEvidenceReason());
        assertEquals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals("My filename", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenDwpEvidenceBundle_thenMoveToDwpDocuments() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder()
                .dwpEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("test.url").build()).build())
                .dwpEditedEvidenceBundleDocument(DwpResponseDocument.builder().documentLink(DocumentLink.builder().documentUrl("edited.url").build()).build())
                .dwpEditedEvidenceReason("Edited reason")
                .dwpDocuments(dwpDocuments).build();

        dwpDocumentService.moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);

        assertEquals(1, sscsCaseData.getDwpDocuments().size());
        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("edited.url", sscsCaseData.getDwpDocuments().get(0).getValue().getEditedDocumentLink().getDocumentUrl());
        assertEquals("Edited reason", sscsCaseData.getDwpDocuments().get(0).getValue().getDwpEditedEvidenceReason());
        assertEquals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getValue(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentType());
        assertEquals(DwpDocumentType.DWP_EVIDENCE_BUNDLE.getLabel(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenCaseWithExistingDwpDocumentType_thenRemoveAllDocumentsOfThisType() {
        List<DwpDocument> dwpDocuments = new ArrayList<>();
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.UCB.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing.com").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.UCB.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("existing2.com").build()).build()).build());
        dwpDocuments.add(DwpDocument.builder().value(DwpDocumentDetails.builder().documentType(DwpDocumentType.APPENDIX_12.getValue()).documentDateAdded(LocalDate.now().minusDays(1).toString()).documentLink(DocumentLink.builder().documentUrl("appendix12.com").build()).build()).build());

        sscsCaseData = sscsCaseData.toBuilder().dwpDocuments(dwpDocuments).build();

        dwpDocumentService.removeDwpDocumentTypeFromCollection(sscsCaseData, DwpDocumentType.UCB);

        assertEquals(1, sscsCaseData.getDwpDocuments().size());
        assertEquals(DwpDocumentType.APPENDIX_12.getValue(), sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentType());
    }

    @Test
    public void removeOldDwpDocuments_setsOldValuesToNull() {
        DwpResponseDocument dwpDocument = DwpResponseDocument.builder().documentFileName("My filename").documentLink(DocumentLink.builder().documentUrl("test.url").build()).build();
        sscsCaseData = sscsCaseData.toBuilder()
                .dwpResponseDocument(dwpDocument)
                .dwpEditedResponseDocument(dwpDocument)
                .dwpAT38Document(dwpDocument)
                .dwpEvidenceBundleDocument(dwpDocument)
                .dwpEditedEvidenceBundleDocument(dwpDocument)
                .appendix12Doc(dwpDocument)
                .build();
        dwpDocumentService.removeOldDwpDocuments(sscsCaseData);
        assertNull(sscsCaseData.getDwpAT38Document());
        assertNull(sscsCaseData.getDwpResponseDocument());
        assertNull(sscsCaseData.getDwpEditedResponseDocument());
        assertNull(sscsCaseData.getDwpEvidenceBundleDocument());
        assertNull(sscsCaseData.getDwpEditedEvidenceBundleDocument());
        assertNull(sscsCaseData.getAppendix12Doc());
    }

}
