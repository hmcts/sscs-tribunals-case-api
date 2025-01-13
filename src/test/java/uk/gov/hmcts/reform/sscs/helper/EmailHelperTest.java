package uk.gov.hmcts.reform.sscs.helper;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;

public class EmailHelperTest {

    EmailHelper emailHelper = new EmailHelper();

    @Test
    public void generateUniqueEmailIdFromAppellant() {
        String result = emailHelper.generateUniqueEmailId(Appellant.builder()
                .identity(Identity.builder().nino("ABC1234YZ").build())
                .name(Name.builder().lastName("Smith").build()).build());

        assertEquals("Smith_4YZ", result);
    }
}
