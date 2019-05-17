package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;

import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;

public class TestHelper {

    private TestHelper() {
    }

    public static SscsCaseData buildSscsCaseDataForTesting() {
        SscsCaseData testCaseData = CaseDataUtils.buildCaseData();

        Subscription appellantSubscription = Subscription.builder()
                .tya("app-appeal-number")
                .email("appellant@email.com")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscription appointeeSubscription = Subscription.builder()
                .tya("appointee-appeal-number")
                .email("appointee@hmcts.net")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscription supporterSubscription = Subscription.builder()
                .tya("")
                .email("supporter@email.com")
                .mobile("07700 900555")
                .subscribeEmail("")
                .subscribeSms("")
                .reason("")
                .build();
        Subscription representativeSubscription = Subscription.builder()
                .tya("rep-appeal-number")
                .email("representative@email.com")
                .mobile("07700 900555")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .supporterSubscription(supporterSubscription)
                .representativeSubscription(representativeSubscription)
                .appointeeSubscription(appointeeSubscription)
                .build();

        testCaseData.setSubscriptions(subscriptions);
        return testCaseData;
    }
}
