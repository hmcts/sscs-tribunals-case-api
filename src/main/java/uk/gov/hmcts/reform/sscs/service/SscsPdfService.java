package uk.gov.hmcts.reform.sscs.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.ExcludeDate;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.service.conversion.LocalDateToWelshStringConverter;

@Service
@Slf4j
public class SscsPdfService {

    private String appellantTemplatePath;
    private String appellantWelshTemplatePath;
    private PDFServiceClient pdfServiceClient;
    private CcdPdfService ccdPdfService;

    @Autowired
    public SscsPdfService(@Value("${appellant.appeal.html.template.path}") String appellantTemplatePath,
                          @Value("${appellant.appeal.html.welsh.template.path}") String appellantWelshTemplatePath,
                          PDFServiceClient pdfServiceClient,
                          CcdPdfService ccdPdfService) {
        this.pdfServiceClient = pdfServiceClient;
        this.appellantTemplatePath = appellantTemplatePath;
        this.appellantWelshTemplatePath = appellantWelshTemplatePath;
        this.ccdPdfService = ccdPdfService;
    }

    public SscsCaseData generatePdf(SscsCaseData sscsCaseData, Long caseDetailsId, String documentType, String fileName) {
        byte[] pdf = generatePdf(sscsCaseData, caseDetailsId);

        log.info("Case {} PDF successfully created for benefit type {}",
                caseDetailsId,
                sscsCaseData.getAppeal().getBenefitType().getCode());

        return ccdPdfService.updateDoc(fileName, pdf, caseDetailsId, sscsCaseData, documentType);
    }

    private byte[] generatePdf(SscsCaseData sscsCaseData, Long caseDetailsId) {
        byte[] template;
        try {
            template = getTemplate(sscsCaseData.isLanguagePreferenceWelsh());
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }
        PdfWrapper pdfWrapper = PdfWrapper.builder().sscsCaseData(sscsCaseData).ccdCaseId(caseDetailsId)
                .isSignLanguageInterpreterRequired(sscsCaseData.getAppeal().getHearingOptions().wantsSignLanguageInterpreter())
                .isHearingLoopRequired(sscsCaseData.getAppeal().getHearingOptions().wantsHearingLoop())
                .isAccessibleHearingRoomRequired(sscsCaseData.getAppeal().getHearingOptions().wantsAccessibleHearingRoom())
                .currentDate(LocalDate.now())
                .repFullName(getRepFullName(sscsCaseData.getAppeal().getRep())).build();

        Map<String, Object> placeholders = new HashMap<>();
        placeholders.put("PdfWrapper", pdfWrapper);
        if (sscsCaseData.isLanguagePreferenceWelsh()) {
            placeholders.put("appellant_identity_dob",
                LocalDateToWelshStringConverter.convert(sscsCaseData.getAppeal().getAppellant().getIdentity().getDob()));
            placeholders.put("appellant_appointee_identity_dob",
                LocalDateToWelshStringConverter.convert(sscsCaseData.getAppeal().getAppellant().getIdentity().getDob()));
            placeholders.put("date_of_decision",
                    LocalDateToWelshStringConverter.convert(sscsCaseData.getAppeal().getMrnDetails().getMrnDate()));

            List<String> welshExcludesDates = new ArrayList<>();
            List<ExcludeDate> excludesDates = sscsCaseData.getAppeal().getHearingOptions().getExcludeDates();
            if (excludesDates != null) {
                for (ExcludeDate excludeDate : excludesDates) {
                    welshExcludesDates.add(LocalDateToWelshStringConverter.convert(excludeDate.getValue().getStart()));
                }
                placeholders.put("welsh_exclude_dates",welshExcludesDates);
            }
            placeholders.put("welshCurrentDate",LocalDateToWelshStringConverter.convert(LocalDate.now()));
        }
        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private static String getRepFullName(Representative representative) {
        if (representative != null && representative.getName() != null) {
            return representative.getName().getFullName();
        } else {
            return null;
        }
    }

    private byte[] getTemplate(boolean isWelsh) throws IOException {
        InputStream in = getClass().getResourceAsStream(isWelsh ? appellantWelshTemplatePath : appellantTemplatePath);
        return IOUtils.toByteArray(in);
    }
}
