package uk.gov.hmcts.reform.sscs.functional.mya;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class SubscriptionTest extends BaseFunctionTest {

    @Test
    public void shouldUpdateSubscription() throws IOException, InterruptedException {
        String newUserEmail = createRandomEmail();
        CreatedCcdCase ccdCase = createCcdCase(createRandomEmail());
        String appellantTya = ccdCase.getAppellantTya();

        // Give ES time to index
        Thread.sleep(5000L);

        String benefitType = sscsMyaBackendRequests.updateSubscription(appellantTya, newUserEmail);
        assertThat(benefitType, is("{\"benefitType\":\"pip\"}"));
        SscsCaseDetails updatedCase = getCaseDetails(ccdCase.getCaseId());
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getTya(), is(appellantTya));
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getEmail(), is(newUserEmail));
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getSubscribeEmail(), is(YES));
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getSubscribeSms(), is(NO));
    }

    @Test
    public void shouldUnsubscribeSubscription() throws IOException, InterruptedException {
        String userEmail = createRandomEmail();
        CreatedCcdCase ccdCase = createCcdCase(userEmail);
        String appellantTya = ccdCase.getAppellantTya();

        // Give ES time to index
        Thread.sleep(3000L);

        sscsMyaBackendRequests.unsubscribeSubscription(appellantTya, userEmail);
        SscsCaseDetails updatedCase = getCaseDetails(ccdCase.getCaseId());
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getTya(), is(appellantTya));
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getSubscribeEmail(), is(NO));
        assertThat(updatedCase.getData().getSubscriptions().getAppellantSubscription().getSubscribeSms(), is(NO));
    }
}
