package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private CcdRequestDetails ccdRequestDetails;

    @Autowired
    private IdamService idamService;

    @Autowired
    private SubmitAppealService submitAppealService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenACaseShouldBeSavedIntoCcd() {
        SscsCaseData testCaseData = CaseDataUtils.buildCaseData();

        Subscription appellantSubscription = Subscription.builder()
            .tya("app-appeal-number")
            .email("appellant@email.com")
            .mobile("07777777777")
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
            .mobile("07777777777")
            .subscribeEmail("")
            .subscribeSms("")
            .reason("")
            .build();
        Subscription representativeSubscription = Subscription.builder()
            .tya("rep-appeal-number")
            .email("representative@email.com")
            .mobile("07777777777")
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
        SscsCaseDetails caseDetails = ccdService.createCase(testCaseData, "appealCreated", "Appeal created summary", "Appeal created description",
            idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
            "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseShouldBeSavedIntoCcdViaSubmitAppealService() {
        try {
            submitAppealService.submitAppeal(ALL_DETAILS.getDeserializeMessage());
        } catch (EmailSendFailedException | PdfGenerationException ep) {
            assertTrue(true);
        } catch (Exception ex) {
            fail();
        }

    }

    @Test
    public void givenASyaCaseWithoutAMatchingRpcShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();

        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithSameAddressShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertTrue(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("Yes", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithDifferentAddressShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertFalse(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("No", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithSameAddressButNoAppellantContactDetailsShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertTrue(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("Yes", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

}
