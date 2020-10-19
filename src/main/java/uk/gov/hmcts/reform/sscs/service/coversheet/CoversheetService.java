package uk.gov.hmcts.reform.sscs.service.coversheet;

import java.util.Optional;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;

@Service
public class CoversheetService {
    private final OnlineHearingService onlineHearingService;
    private final PdfService pdfService;
    private static final String TEMPLATE = "template";
    private static final String HMCTS_IMG_VALUE = "hmctsImgVal";
    private final CcdService ccdService;
    private final IdamService idamService;
    private final DocumentConfiguration documentConfiguration;

    public CoversheetService(
            OnlineHearingService onlineHearingService,
            @Qualifier("docmosisPdfService") PdfService pdfService,
            CcdService ccdService, IdamService idamService,
            DocumentConfiguration documentConfiguration) {
        this.onlineHearingService = onlineHearingService;
        this.pdfService = pdfService;
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.documentConfiguration = documentConfiguration;
    }

    public Optional<byte[]> createCoverSheet(String identifier) {
        Optional<SscsCaseDetails> sscsCaseDetails = !NumberUtils.isDigits(identifier)
                ? onlineHearingService.getCcdCase(identifier) :
                Optional.ofNullable(ccdService.getByCaseId(Long.parseLong(identifier), idamService.getIdamTokens()));
        return sscsCaseDetails
                .map(sscsCase -> {

                    SscsCaseData sscsCaseData = sscsCase.getData();
                    String derivedTemplate = documentConfiguration.getEvidence()
                            .get(sscsCaseData.getLanguagePreference()).get(TEMPLATE);
                    Address address = sscsCaseData.getAppeal().getAppellant().getAddress();

                    PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                            "" + sscsCase.getId(),
                            sscsCaseData.getAppeal().getAppellant().getName().getFullNameNoTitle(),
                            address.getLine1(),
                            address.getLine2(),
                            address.getTown(),
                            address.getCounty(),
                            address.getPostcode(),
                            documentConfiguration.getEvidence()
                                    .get(LanguagePreference.ENGLISH).get(HMCTS_IMG_VALUE),
                            documentConfiguration.getEvidence()
                                    .get(LanguagePreference.WELSH).get(HMCTS_IMG_VALUE)
                    );

                    return pdfService.createPdf(pdfCoverSheet, derivedTemplate);
                });
    }
}
