package uk.gov.hmcts.reform.sscs.service.pdf;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfEvidenceDescription;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;
import uk.gov.hmcts.reform.sscs.service.pdf.data.EvidenceDescriptionPdfData;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Service
public class StoreEvidenceDescriptionService extends StorePdfService<PdfEvidenceDescription, EvidenceDescriptionPdfData> {

    public static final String TEMP_UNIQUE_ID = "temporal unique Id ec7ae162-9834-46b7-826d-fdc9935e3187";

    StoreEvidenceDescriptionService(
            @Qualifier("oldPdfService") PdfService pdfService,
            @Value("${evidenceDescription.html.template.path}")String pdfTemplatePath,
            @Value("${evidenceDescription.html.welsh.template.path}")String welshPdfTemplatePath,
            CcdPdfService ccdPdfService,
            IdamService idamService,
            PdfStoreService pdfStoreService) {
        super(pdfService, pdfTemplatePath, welshPdfTemplatePath, ccdPdfService, idamService, pdfStoreService);
    }

    @Override
    protected boolean pdfHasNotAlreadyBeenCreated(SscsCaseDetails caseDetails, String documentNamePrefix) {
        return true;
    }

    @Override
    protected String documentNamePrefix(SscsCaseDetails caseDetails, String onlineHearingId,
                                        EvidenceDescriptionPdfData data) {
        return TEMP_UNIQUE_ID + " Evidence Description - ";
    }

    @Override
    protected PdfEvidenceDescription getPdfContent(EvidenceDescriptionPdfData data, String onlineHearingId,
                                                   PdfAppealDetails appealDetails) {
        return new PdfEvidenceDescription(appealDetails, data.getDescription().getBody(), data.getFileNames());
    }
}
