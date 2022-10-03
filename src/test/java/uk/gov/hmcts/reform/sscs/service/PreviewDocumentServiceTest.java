package uk.gov.hmcts.reform.sscs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_ADJOURNMENT_NOTICE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@RunWith(MockitoJUnitRunner.class)
public class PreviewDocumentServiceTest {

    @InjectMocks
    private PreviewDocumentService previewDocumentService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setup() {
        List<SscsDocument> docs = new ArrayList<>(List.of(
            SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                    .documentFileName("oldDraft.doc")
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

    @Test
    public void givenDraftAdjournmentNoticeAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        previewDocumentService.writePreviewDocumentToSscsDocument(sscsCaseData, DRAFT_ADJOURNMENT_NOTICE, sscsCaseData.getAdjournCasePreviewDocument());

        assertThat(sscsCaseData.getSscsDocument())
            .hasSize(1)
            .extracting(AbstractDocument::getValue)
            .extracting(AbstractDocumentDetails::getDocumentFileName)
            .allSatisfy(s -> assertThat(s).containsPattern(Pattern.compile("Draft Adjournment Notice generated on \\d{1,2}-\\d{1,2}-\\d{4}\\.pdf")));
    }
}
