package uk.gov.hmcts.reform.sscs.service.coversheet;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.DocumentConfiguration;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.OnlineHearingService;
import uk.gov.hmcts.reform.sscs.thirdparty.pdfservice.PdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.LetterUtils;

@Service
@Slf4j
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

                    Appellant appellant = sscsCaseData.getAppeal().getAppellant();
                    Address address;
                    String fullName;

                    if ("yes".equalsIgnoreCase(appellant.getIsAppointee())
                            && appellant.getAppointee() != null
                            && appellant.getAppointee().getAddress() != null) {
                        address = appellant.getAppointee().getAddress();
                        fullName = getSafeName(appellant.getAppointee().getName());
                    } else {
                        address = appellant.getAddress();
                        fullName = getSafeName(appellant.getName());
                    }

                    if (fullName == null) {
                        log.error("Coversheet generated with no name for case {}", sscsCase.getId());
                        fullName = StringUtils.EMPTY;
                    }

                    var lines = LetterUtils.lines(address);
                    for (int i = lines.size(); i < 5; i++) {
                        lines.add("");
                    }

                    var lineNum = 0;
                    PdfCoverSheet pdfCoverSheet = new PdfCoverSheet(
                            "" + sscsCase.getId(),
                            fullName,
                            lines.get(lineNum++),
                            lines.get(lineNum++),
                            lines.get(lineNum++),
                            lines.get(lineNum++),
                            lines.get(lineNum),
                            documentConfiguration.getEvidence()
                                    .get(LanguagePreference.ENGLISH).get(HMCTS_IMG_VALUE),
                            documentConfiguration.getEvidence()
                                    .get(LanguagePreference.WELSH).get(HMCTS_IMG_VALUE)
                    );

                    return pdfService.createPdf(pdfCoverSheet, derivedTemplate);
                });
    }

    private static String getSafeName(Name name) {
        if (name == null) {
            return null;
        }
        return name.getFullNameNoTitle();
    }
}

