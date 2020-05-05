package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.util.IOUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.util.I18nBuilder;

@Service
public class OldPdfService implements PdfService {
    private final PDFServiceClient pdfServiceClient;
    private final Map i18n;
    private final Map<String, byte[]> templatesCache;

    public OldPdfService(PDFServiceClient pdfServiceClient, I18nBuilder i18nBuilder) throws IOException {
        this.pdfServiceClient = pdfServiceClient;
        this.templatesCache = new HashMap<>();
        this.i18n = i18nBuilder.build();
    }

    @Override
    public byte[] createPdf(Object pdfSummary, String templatePath) {
        Map<String, Object> placeholders = ImmutableMap.of("pdfSummary", pdfSummary, "i18n", i18n);

        return pdfServiceClient.generateFromHtml(getTemplate(templatePath), placeholders);
    }

    private synchronized byte[] getTemplate(String templatePath) {
        if (!templatesCache.containsKey(templatePath)) {
            try {
                templatesCache.put(templatePath, getResource(templatePath));
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot load template [" + templatePath + "]");
            }
        }
        return templatesCache.get(templatePath);
    }

    private byte[] getResource(String file) throws IOException {
        InputStream in = getClass().getResourceAsStream(file);
        return IOUtils.toByteArray(in);
    }
}
