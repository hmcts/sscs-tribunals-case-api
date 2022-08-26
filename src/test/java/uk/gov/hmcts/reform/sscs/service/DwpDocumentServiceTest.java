package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class DwpDocumentServiceTest {

    private SscsCaseData sscsCaseData;

    private DwpDocumentService dwpDocumentService;

    @Before
    public void setUp() {
        dwpDocumentService = new DwpDocumentService();

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1234")
                .createdInGapsFrom(State.READY_TO_LIST.getId())
                .appeal(Appeal.builder().build())
                .build();

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
