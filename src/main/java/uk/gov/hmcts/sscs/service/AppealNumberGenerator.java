package uk.gov.hmcts.sscs.service;

import java.security.SecureRandom;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.sscs.domain.corecase.CcdCase;
import uk.gov.hmcts.sscs.exception.CcdException;

@Component
public class AppealNumberGenerator {

    private static final int LENGTH = 10;
    public static final char MINIMUM_CODE_POINT = '0';
    public static final char MAXIMUM_CODE_POINT = 'z';

    private CcdService ccdService;

    @Autowired
    public AppealNumberGenerator(CcdService ccdService) {
        this.ccdService = ccdService;
    }

    public String generate() throws CcdException {

        String appealNumber = "";
        int count = 3;
        while (count-- > 0 && "".equals(appealNumber)) {
            appealNumber = generateAppealNumber();
            CcdCase ccdCase = ccdService.findCcdCaseDetailsByAppealNumber(appealNumber);
            if (isDuplicateInCcd(ccdCase)) {
                appealNumber = "";
            }
            if (count == 0 && "".equals(appealNumber)) {
                throw new CcdException("AppealNumberGenerator has generated duplicate appeal number against CCD");
            }
        }

        return appealNumber;

    }

    private boolean isDuplicateInCcd(CcdCase ccdCase) {
        return ccdCase != null && ccdCase.getAppeal() != null;
    }

    protected String generateAppealNumber() {

        SecureRandom random = new SecureRandom();
        RandomStringGenerator generator = new RandomStringGenerator.Builder()
                .withinRange(MINIMUM_CODE_POINT, MAXIMUM_CODE_POINT)
                .filteredBy(CharacterPredicates.DIGITS, CharacterPredicates.LETTERS).usingRandom(random::nextInt)
                .build();
        return generator.generate(LENGTH);
    }

}
