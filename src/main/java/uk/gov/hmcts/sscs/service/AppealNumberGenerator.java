package uk.gov.hmcts.sscs.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.security.SecureRandom;
import org.apache.commons.text.CharacterPredicates;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.sscs.exception.CcdException;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@Component
public class AppealNumberGenerator {
    private static final Logger LOG = getLogger(AppealNumberGenerator.class);
    private static final int LENGTH = 10;
    public static final char MINIMUM_CODE_POINT = '0';
    public static final char MAXIMUM_CODE_POINT = 'z';

    private CcdService ccdService;

    @Autowired
    public AppealNumberGenerator(CcdService ccdService) {
        this.ccdService = ccdService;
    }

    public String generate() {

        String appealNumber = "";
        int count = 3;
        while (count-- > 0 && "".equals(appealNumber)) {
            appealNumber = generateAppealNumber();
            CaseData caseData = ccdService.findCcdCaseByAppealNumber(appealNumber);
            if (isDuplicateInCcd(caseData)) {
                appealNumber = "";
            }
            if (count == 0 && "".equals(appealNumber)) {
                String message = "AppealNumberGenerator has generated duplicate appeal number against CCD";
                CcdException ccdException = new CcdException(message);
                LOG.error(message, ccdException);
                throw ccdException;
            }
        }

        return appealNumber;

    }

    private boolean isDuplicateInCcd(CaseData caseData) {
        return caseData != null;
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
