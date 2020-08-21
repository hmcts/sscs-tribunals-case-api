package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Test;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Statement;
import uk.gov.hmcts.reform.sscs.util.DataFixtures;
import uk.gov.hmcts.reform.sscs.util.I18nBuilder;

public class OldPdfServiceTest {
    @Test
    public void createsPdf() throws IOException {
        PDFServiceClient pdfServiceClient = mock(PDFServiceClient.class);
        I18nBuilder i18nBuilder = mock(I18nBuilder.class);
        ResourceManager resourceManager = mock(ResourceManager.class);
        HashMap i18n = new HashMap();
        when(i18nBuilder.build()).thenReturn(i18n);
        PdfService appellantTemplatePath = new OldPdfService(pdfServiceClient, i18nBuilder, resourceManager);

        Statement statement = DataFixtures.someStatement();

        byte[] expectedPdf = new byte[]{ 1, 2, 3};
        when(pdfServiceClient.generateFromHtml(any(), eq(ImmutableMap.of("pdfSummary", statement, "i18n", i18n))))
                .thenReturn(expectedPdf);

        byte[] pdf = appellantTemplatePath.createPdf(statement, "/templates/personalStatement.html");

        assertThat(pdf, is(expectedPdf));
    }
}
