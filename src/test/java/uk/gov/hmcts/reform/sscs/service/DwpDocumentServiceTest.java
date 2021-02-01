package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

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

        assertEquals("test.url", sscsCaseData.getDwpDocuments().get(0).getValue().getDocumentLink().getDocumentUrl());
        assertEquals("existing.com", sscsCaseData.getDwpDocuments().get(1).getValue().getDocumentLink().getDocumentUrl());

    }

}