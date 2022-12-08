package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@ExtendWith(MockitoExtension.class)
class PreviewDocumentServiceTest {

    public static final String OLD_DRAFT_DOC = "oldDraft.doc";
    @InjectMocks
    private PreviewDocumentService previewDocumentService;
    private SscsCaseData sscsCaseData;

    @BeforeEach
    void setup() {
        List<SscsDocument> docs = new ArrayList<>(List.of(
            SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentFileName(OLD_DRAFT_DOC)
                    .documentType(DRAFT_ADJOURNMENT_NOTICE.getValue())
                    .build())
                .build()
        ));
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .sscsDocument(docs)
            .build();
    }

    @DisplayName("Given draft adjournment notice already exists on case, then overwrite existing draft")
    @Test
    void givenDraftAdjournmentNoticeAlreadyExists_thenOverwritesExistingDraft() {
        previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, sscsCaseData.getAdjournment().getPreviewDocument());

        assertThat(sscsCaseData.getSscsDocument())
            .hasSize(1)
            .extracting(AbstractDocument::getValue)
            .extracting(AbstractDocumentDetails::getDocumentFileName)
            .allSatisfy(fileName -> {
                assertThat(fileName).containsPattern(Pattern.compile("Draft Adjournment Notice generated on \\d{1,2}-\\d{1,2}-\\d{4}\\.pdf"));
                assertThat(fileName).contains(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
            });
    }
}
