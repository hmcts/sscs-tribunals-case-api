package uk.gov.hmcts.reform.sscs.tyanotifications.tya;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("integration")
public class NotificationsTyaLinksIt {
    @Value("${manage.emails.link}")
    private String manageEmailsLink;
    @Value("${track.appeal.link}")
    private String trackAppealLink;
    @Value("${tya.evidence.submit.info.link}")
    private String evidenceSubmissionInfoLink;
    @Value("${claiming.expenses.link}")
    private String claimingExpensesLink;
    @Value("${hearing.info.link}")
    private String hearingInfoLink;

    @Test
    public void shouldVerifyTyaLinksAreInValidFormat() {
        assertThat(manageEmailsLink).endsWith("/manage-email-notifications/mac");
        assertThat(trackAppealLink).endsWith("/trackyourappeal/appeal_id");
        assertThat(evidenceSubmissionInfoLink).endsWith("/evidence/appeal_id");
        assertThat(claimingExpensesLink).endsWith("/expenses/appeal_id");
        assertThat(hearingInfoLink).endsWith("/abouthearing/appeal_id");
    }
}
