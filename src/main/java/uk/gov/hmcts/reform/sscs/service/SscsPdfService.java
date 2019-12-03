package uk.gov.hmcts.reform.sscs.service;

import static com.google.common.collect.Lists.newArrayList;
import static uk.gov.hmcts.reform.sscs.domain.email.EmailAttachment.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.service.client.PDFServiceClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.email.SubmitYourAppealEmailTemplate;
import uk.gov.hmcts.reform.sscs.domain.pdf.PdfWrapper;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SscsPdfService {

    private String appellantTemplatePath;
    private PDFServiceClient pdfServiceClient;
    private EmailService emailService;
    private SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate;
    private CcdPdfService ccdPdfService;

    @Autowired
    public SscsPdfService(@Value("${appellant.appeal.html.template.path}") String appellantTemplatePath,
                          PDFServiceClient pdfServiceClient,
                          EmailService emailService,
                          SubmitYourAppealEmailTemplate submitYourAppealEmailTemplate,
                          CcdPdfService ccdPdfService) {
        this.pdfServiceClient = pdfServiceClient;
        this.emailService = emailService;
        this.submitYourAppealEmailTemplate = submitYourAppealEmailTemplate;
        this.appellantTemplatePath = appellantTemplatePath;
        this.ccdPdfService = ccdPdfService;
    }

    public byte[] generateAndSendPdf(SscsCaseData sscsCaseData, Long caseDetailsId, IdamTokens idamTokens, String documentType) {
        byte[] pdf = generatePdf(sscsCaseData, caseDetailsId);

        log.info("Case {} PDF successfully created for benefit type {}",
                caseDetailsId,
                sscsCaseData.getAppeal().getBenefitType().getCode());

        sendPdfByEmail(sscsCaseData.getAppeal(), pdf, caseDetailsId);

        prepareCcdCaseForPdf(caseDetailsId, sscsCaseData, pdf, idamTokens, documentType);

        return pdf;
    }

    private byte[] generatePdf(SscsCaseData sscsCaseData, Long caseDetailsId) {
        byte[] template;
        try {
            template = getTemplate();
        } catch (IOException e) {
            throw new PdfGenerationException("Error getting template", e);
        }

        PdfWrapper pdfWrapper = PdfWrapper.builder().sscsCaseData(sscsCaseData).ccdCaseId(caseDetailsId)
                .isSignLanguageInterpreterRequired(sscsCaseData.getAppeal().getHearingOptions().wantsSignLanguageInterpreter())
                .isHearingLoopRequired(sscsCaseData.getAppeal().getHearingOptions().wantsHearingLoop())
                .isAccessibleHearingRoomRequired(sscsCaseData.getAppeal().getHearingOptions().wantsAccessibleHearingRoom())
                .currentDate(LocalDate.now())
                .repFullName(getRepFullName(sscsCaseData.getAppeal().getRep())).build();

        Map<String, Object> placeholders = Collections.singletonMap("PdfWrapper", pdfWrapper);
        return pdfServiceClient.generateFromHtml(template, placeholders);
    }

    private static String getRepFullName(Representative representative) {
        if (representative != null && representative.getName() != null) {
            return representative.getName().getFullName();
        } else {
            return null;
        }
    }

    private void prepareCcdCaseForPdf(Long caseId, SscsCaseData caseData, byte[] pdf, IdamTokens idamTokens, String documentType) {
        String fileName = emailService.generateUniqueEmailId(caseData.getAppeal().getAppellant()) + ".pdf";
        ccdPdfService.mergeDocIntoCcd(fileName, pdf, caseId, caseData, idamTokens, documentType);
    }

    private void sendPdfByEmail(Appeal appeal, byte[] pdf, Long caseDetailsId) {
        String appellantUniqueId = emailService.generateUniqueEmailId(appeal.getAppellant());
        emailService.sendEmail(submitYourAppealEmailTemplate.generateEmail(
                appellantUniqueId,
                newArrayList(pdf(pdf, appellantUniqueId + ".pdf")))
        );

        log.info("Case {} PDF email sent successfully for benefit type {}", caseDetailsId,
                appeal.getBenefitType().getCode());
    }


    private byte[] getTemplate() throws IOException {
        InputStream in = getClass().getResourceAsStream(appellantTemplatePath);
        return IOUtils.toByteArray(in);
    }


}
