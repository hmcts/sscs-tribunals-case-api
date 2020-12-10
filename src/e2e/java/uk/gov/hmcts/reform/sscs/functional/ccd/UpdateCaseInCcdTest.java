package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

    public static SscsCaseData buildSscsCaseDataForTesting(final String surname, final String nino) {
        SscsCaseData testCaseData = buildCaseData(surname, nino);

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
        DynamicListItem value = new DynamicListItem("informationReceivedForInterlocJudge", "any");
        DynamicList furtherEvidenceActionList = new DynamicList(value, Collections.singletonList(value));
        testCaseData.setFurtherEvidenceAction(furtherEvidenceActionList);
    }


    public static SscsCaseData buildCaseData(final String surname, final String nino) {
        Name name = Name.builder()
                .title("Mr")
                .firstName("User")
                .lastName(surname)
                .build();
        Address address = Address.builder()
                .line1("123 Hairy Lane")
                .line2("Off Hairy Park")
                .town("Hairyfield")
                .county("Kent")
                .postcode("TN32 6PL")
                .build();
        Contact contact = Contact.builder()
                .email("mail@email.com")
                .phone("01234567890")
                .mobile("01234567890")
                .build();
        Identity identity = Identity.builder()
                .dob("1904-03-10")
                .nino(nino)
                .build();

        Appellant appellant = Appellant.builder()
                .name(name)
                .address(address)
                .contact(contact)
                .identity(identity)
                .build();

        BenefitType benefitType = BenefitType.builder()
                .code("PIP")
                .build();

        HearingOptions hearingOptions = HearingOptions.builder()
                .wantsToAttend(YES)
                .languageInterpreter(YES)
                .languages("Spanish")
                .signLanguageType("A sign language")
                .arrangements(Arrays.asList("hearingLoop", "signLanguageInterpreter"))
                .other("Yes, this...")
                .build();

        MrnDetails mrnDetails = MrnDetails.builder()
                .mrnDate("2018-06-29")
                .dwpIssuingOffice("1")
                .mrnLateReason("Lost my paperwork")
                .build();

        final Appeal appeal = Appeal.builder()
                .appellant(appellant)
                .benefitType(benefitType)
                .hearingOptions(hearingOptions)
                .mrnDetails(mrnDetails)
                .signer("Signer")
                .hearingType("oral")
                .receivedVia("Online")
                .build();


        Subscription appellantSubscription = Subscription.builder()
                .tya("app-appeal-number")
                .email("appellant@email.com")
                .mobile("")
                .subscribeEmail(YES)
                .subscribeSms(YES)
                .reason("")
                .build();
        Subscriptions subscriptions = Subscriptions.builder()
                .appellantSubscription(appellantSubscription)
                .build();
        return SscsCaseData.builder()
                .caseReference("SC068/17/00013")
                .caseCreated(LocalDate.now().toString())
                .appeal(appeal)
                .subscriptions(subscriptions)
                .region("CARDIFF")
                .createdInGapsFrom(READY_TO_LIST.getId())
                .build();
    }
}
