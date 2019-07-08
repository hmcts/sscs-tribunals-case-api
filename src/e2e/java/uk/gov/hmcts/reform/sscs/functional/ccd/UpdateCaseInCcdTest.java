package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@SpringBootTest
public class UpdateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenACase_shouldBeUpdatedInCcd() {
        SscsCaseData testCaseData = buildSscsCaseDataForTestingWithValidMobileNumbers();
        SscsCaseDetails caseDetails = ccdService.createCase(testCaseData, "appealCreated",
            "Appeal created summary", "Appeal created description",
            idamTokens);

        assertNotNull(caseDetails);
        testCaseData.setCaseReference("SC123/12/78765");
        SscsCaseDetails updatedCaseDetails = ccdService.updateCase(testCaseData, caseDetails.getId(),
            "appealReceived", "", "", idamTokens);
        assertEquals("SC123/12/78765", updatedCaseDetails.getData().getCaseReference());
    }

    public static SscsCaseData buildSscsCaseDataForTestingWithValidMobileNumbers() {
        SscsCaseData testCaseData = CaseDataUtils.buildCaseData();

        addFurtherEvidenceActionData(testCaseData);

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

    private static void addFurtherEvidenceActionData(SscsCaseData testCaseData) {
        testCaseData.setInterlocReviewState(null);
        DynamicListItem value = new DynamicListItem("informationReceivedForInterloc", "any");
        DynamicList furtherEvidenceActionList = new DynamicList(value, Collections.singletonList(value));
        testCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
    }
}
