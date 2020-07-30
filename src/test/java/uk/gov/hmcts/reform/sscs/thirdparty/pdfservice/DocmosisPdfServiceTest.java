package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.service.coversheet.PdfCoverSheet;

public class DocmosisPdfServiceTest {

    private byte[] expectedPdf;
    private String template;
    private HashMap<String, Object> expectedPlaceholders;
    private PdfCoverSheet pdfCoverSheet;
    private DocmosisPdfGenerationService docmosisPdfGenerationService;

    @Before
    public void setUp() {
        expectedPdf = new byte[]{2, 4, 6, 0, 1};
        template = "template";

        expectedPlaceholders = new HashMap<>();
        expectedPlaceholders.put("case_id", "caseId");
        expectedPlaceholders.put("name", "name");
        expectedPlaceholders.put("address_line1", "addressLine1");
        expectedPlaceholders.put("address_line2", "addressLine2");
        expectedPlaceholders.put("address_town", "addressTown");
        expectedPlaceholders.put("address_county", "addressCounty");
        expectedPlaceholders.put("address_postcode", "addressPostcode");
        expectedPlaceholders.put("hmcts2", "hmcts.img");
        expectedPlaceholders.put("welshhmcts2", "welshhmcts.img");

        pdfCoverSheet = new PdfCoverSheet(
                "caseId", "name", "addressLine1", "addressLine2", "addressTown", "addressCounty", "addressPostcode",
                "hmcts.img", "welshhmcts.img"
        );
        docmosisPdfGenerationService = mock(DocmosisPdfGenerationService.class);
    }

    @Test
    public void canCreatePdf() {
        when(docmosisPdfGenerationService.generatePdf(
                argThat(argument ->
                        argument.getPlaceholders().equals(expectedPlaceholders)
                                && argument.getTemplate().getTemplateName().equals(template)
                                && argument.getTemplate().getHmctsDocName().equals("")
                )
        )).thenReturn(expectedPdf);

        byte[] pdfBytes = new DocmosisPdfService(docmosisPdfGenerationService).createPdf(pdfCoverSheet, template);

        assertThat(pdfBytes, is(expectedPdf));
    }

}
