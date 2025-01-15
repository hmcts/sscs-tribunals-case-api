package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.ADDRESS_LINE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.REGIONAL_OFFICE_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.SUPPORT_CENTRE_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.config.PdfTemplateConstants.TOWN_LITERAL;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;

@Service
@Slf4j
public class SscsGeneratePdfService {
    private static final String RPC = "rpc_";
    private PDFServiceClient pdfServiceClient;

    @Autowired
    public SscsGeneratePdfService(PDFServiceClient pdfServiceClient) {
        this.pdfServiceClient = pdfServiceClient;
    }

    public byte[] generatePdf(String templatePath, SscsCaseData sscsCaseData, Long caseDetailsId, Map<String, String> notificationPlaceholders) {
        byte[] template;
        try {
            template = getTemplate(templatePath);
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }

        PdfWrapper pdfWrapper = PdfWrapper.builder()
            .sscsCaseData(sscsCaseData)
            .ccdCaseId(caseDetailsId)
            .currentDate(LocalDate.now())
            .build();

        Map<String, Object> placeholders = new HashMap<>(notificationPlaceholders);
        placeholders.put("PdfWrapper", pdfWrapper);
        placeholders.put(RPC + REGIONAL_OFFICE_NAME_LITERAL, notificationPlaceholders.get(REGIONAL_OFFICE_NAME_LITERAL));
        placeholders.put(RPC + SUPPORT_CENTRE_NAME_LITERAL, notificationPlaceholders.get(SUPPORT_CENTRE_NAME_LITERAL));
        placeholders.put(RPC + ADDRESS_LINE_LITERAL, notificationPlaceholders.get(ADDRESS_LINE_LITERAL));
        placeholders.put(RPC + TOWN_LITERAL, notificationPlaceholders.get(TOWN_LITERAL));
        placeholders.put(RPC + COUNTY_LITERAL, notificationPlaceholders.get(COUNTY_LITERAL));
        placeholders.put(RPC + POSTCODE_LITERAL, notificationPlaceholders.get(POSTCODE_LITERAL));
        placeholders.put(RPC + PHONE_NUMBER, notificationPlaceholders.get(PHONE_NUMBER));

        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private byte[] getTemplate(String templatePath) throws IOException {
        InputStream in = getClass().getResourceAsStream(templatePath);
        return IOUtils.toByteArray(in);
    }
}
