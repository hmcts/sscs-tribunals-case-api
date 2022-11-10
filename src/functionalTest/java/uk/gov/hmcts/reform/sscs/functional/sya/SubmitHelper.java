package uk.gov.hmcts.reform.sscs.functional.sya;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
public class SubmitHelper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    @Autowired
    private CcdService ccdService;

    public String setLatestMrnDate(String body, LocalDate localDate) {
        return body.replaceAll("01-02-2018", localDate == null ? "" : formatter.format(localDate));
    }

    public String setNino(String body, String nino) {
        return body.replaceAll("AB877533C", nino);
    }

    public SscsCaseDetails findCaseInCcd(Long id, IdamTokens idamTokens) {
        return ccdService.getByCaseId(id, idamTokens);
    }

    public LocalDate getRandomMrnDate() {
        long minDay = LocalDate.now().minusDays(1).toEpochDay();
        long maxDay = LocalDate.now().minusDays(28).toEpochDay();
        @SuppressWarnings("squid:S2245")
        long randomDay = ThreadLocalRandom.current().nextLong(maxDay, minDay);
        return LocalDate.ofEpochDay(randomDay);
    }

    public String getRandomNino() {
        return RandomStringUtils.random(9, true, true).toUpperCase();
    }

    public String setBenefitCode(String body, String benefitCode) {
        return body.replaceFirst("BENEFIT_TYPE_CODE", benefitCode);
    }

    public String setDwpIssuingOffice(String body, String dwpIssuingOffice) {
        return body.replace("MRN_DWP_ISSUING_OFFICE", dwpIssuingOffice);
    }
}
