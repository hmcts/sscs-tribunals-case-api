package uk.gov.hmcts.reform.sscs.service.coversheet;

import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Service
public class CoversheetService {
    private final OnlineHearingService onlineHearingService;
    private final PdfService pdfService;
    private final String template;
    private final String hmctsImg;
    private final CcdService ccdService;
    private final IdamService idamService;

    public CoversheetService(
            OnlineHearingService onlineHearingService,
            @Qualifier("docmosisPdfService") PdfService pdfService,
            @Value("${evidenceCoverSheet.docmosis.template}") String template,
            @Value("${evidenceCoverSheet.docmosis.hmctsImgVal}") String hmctsImg,
            CcdService ccdService, IdamService idamService) {
        this.onlineHearingService = onlineHearingService;
        this.pdfService = pdfService;
        this.template = template;
        this.hmctsImg = hmctsImg;
        this.ccdService = ccdService;
        this.idamService = idamService;
    }

    public Optional<byte[]> createCoverSheet(String identifier) {
        Optional<SscsCaseDetails> sscsCaseDetails = !NumberUtils.isDigits(identifier)
                ? onlineHearingService.getCcdCase(identifier) :
                Optional.ofNullable(ccdService.getByCaseId(Long.parseLong(identifier), idamService.getIdamTokens()));

        return sscsCaseDetails
                .map(sscsCase -> {
                    SscsCaseData sscsCaseData = sscsCase.getData();
                    Address address = sscsCaseData.getAppeal().getAppellant().getAddress();
                    PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                            "" + sscsCase.getId(),
                            sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle(),
                            address.getLine1(),
                            address.getLine2(),
                            address.getTown(),
                            address.getCounty(),
                            address.getPostcode(),
                            hmctsImg
                    );

                    return pdfService.createPdf(pdfCoverSheet, template);
                });
    }
}
