package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.docmosis.service.DocmosisPdfGenerationService;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.docmosis.PdfCoverSheet;

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
        expectedPlaceholders.put("excela_address_line1", "excelaAddressLine1");
        expectedPlaceholders.put("excela_address_line2", "excelaAddressLine2");
        expectedPlaceholders.put("excela_address_line3", "excelaAddressLine3");
        expectedPlaceholders.put("excela_address_postcode", "excelaAddressPostcode");
        expectedPlaceholders.put("hmcts2", "image");
        expectedPlaceholders.put("hmctsWelshImgVal", "welshImg");

        pdfCoverSheet = new PdfCoverSheet(
            "caseId", "name", "addressLine1", "addressLine2", "addressTown", "addressCounty", "addressPostcode",
            "excelaAddressLine1", "excelaAddressLine2", "excelaAddressLine3", "excelaAddressPostcode", "image",
            "welshImg");
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
