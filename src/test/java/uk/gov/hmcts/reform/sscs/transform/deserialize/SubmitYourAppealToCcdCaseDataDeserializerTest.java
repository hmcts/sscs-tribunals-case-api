package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static junit.framework.TestCase.assertNull;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Reason;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;

@RunWith(JUnitParamsRunner.class)
public class SubmitYourAppealToCcdCaseDataDeserializerTest {

    private static final String NO = "No";

    private RegionalProcessingCenter regionalProcessingCenter;

    @Before
    public void setUp() {
        openMocks(this);
        regionalProcessingCenter = getRegionalProcessingCenter();
    }

    private Subscription removeTyaNumber(Subscription subscription) {
        Subscription subscriptionWithNullTyaNumber = subscription;
        if (subscription != null) {
            subscriptionWithNullTyaNumber = subscription.toBuilder().tya(null).build();
        }
        return subscriptionWithNullTyaNumber;
    }

    private SscsCaseData removeTyaNumber(SscsCaseData caseData) {
        final Subscription rep = removeTyaNumber(caseData.getSubscriptions().getRepresentativeSubscription());
        final Subscription appellant = removeTyaNumber(caseData.getSubscriptions().getAppellantSubscription());
        final Subscription appointee = removeTyaNumber(caseData.getSubscriptions().getAppointeeSubscription());
        return caseData.toBuilder().subscriptions(new Subscriptions(appellant, null, rep, appointee, null)).build();
    }

    @Test
    public void syaAllDetailsTest() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, regionalProcessingCenter.getName(),
            regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaAppellantNoContactDetailsTest() {
        SyaCaseWrapper syaCaseWrapper = APPELLANT_NO_CONTACT_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(APPELLANT_NO_CONTACT_DETAILS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Parameters({"DWP PIP ( 9),PIP,DWP PIP (9)", "null,carersAllowance,Tyneview Park DRT"})
    @Test
    public void syaDwpIssuingOfficeTest(String issuingOffice, String beneiftCode, String expectedIssuing) {

        String actIssuingOffice = "null".equals(issuingOffice) ? null : issuingOffice;

        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getBenefitType().setCode(beneiftCode);
        syaCaseWrapper.getMrn().setDwpIssuingOffice(actIssuingOffice);
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertEquals(expectedIssuing, caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Test
    public void givenAUniversalCreditCaseFromSya_thenDefaultIssuingOfficeToUniversalCredit() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType uc = new SyaBenefitType("Universal Credit Description", "UC");
        syaCaseWrapper.setBenefitType(uc);
        syaCaseWrapper.setMrn(null);
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertEquals("Universal Credit", caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Test
    public void givenAUniversalCreditCaseFromSyaAndMrnIsPopulated_thenUseTheIssuingOfficeFromSya() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType uc = new SyaBenefitType("Universal Credit Description", "UC");
        syaCaseWrapper.setBenefitType(uc);
        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice("My dwp office");
        syaCaseWrapper.setMrn(mrn);
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertEquals("Universal Credit", caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Parameters({
        "DWP PIP (1),PIP,Newcastle", "DWP PIP (2),PIP,Glasgow", "DWP PIP (3),PIP,Bellevale", "DWP PIP (4),PIP,Glasgow",
        "DWP PIP (5),PIP,Springburn", "DWP PIP (6),PIP,Blackpool", "DWP PIP (7),PIP,Blackpool", "DWP PIP (8),PIP,Blackpool",
        "DWP PIP (9),PIP,Blackpool", "Inverness DRT,ESA,Inverness DRT","DWP PIP (),PIP,null",
        "DWP PIP (11),PIP,null", "null,UC,Universal Credit", ",UC,Universal Credit", "null,PIP,null",
        "null,carersAllowance,Tyneview Park DRT", "DWP PIP (5),carersAllowance,Tyneview Park DRT"
    })
    @Test
    public void givenADwpIssuingOffice_shouldMapToTheDwpRegionalCenter(@Nullable String dwpIssuingOffice,
                                                                       @Nullable String benefitCode,
                                                                       @Nullable String expectedDwpRegionalCenter) {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getMrn().setDwpIssuingOffice(dwpIssuingOffice);
        syaCaseWrapper.getBenefitType().setCode(benefitCode);

        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, regionalProcessingCenter.getName(),
            regionalProcessingCenter);

        assertEquals(expectedDwpRegionalCenter, caseData.getDwpRegionalCentre());
    }

    @Parameters({"DWP PIP (1),PIP,Cardiff", "DWP PIP (2),Pip,Glasgow"})
    @Test
    public void dontSetIsScottishCase(@Nullable String dwpIssuingOffice,
                                                                       @Nullable String benefitCode,
                                                                       @Nullable String dwpIssuingOfficeName) {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getMrn().setDwpIssuingOffice(dwpIssuingOffice);
        syaCaseWrapper.getBenefitType().setCode(benefitCode);

        RegionalProcessingCenter rpc = regionalProcessingCenter.toBuilder().name(dwpIssuingOfficeName).build();

        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(),
                rpc);

        assertEquals(null, caseData.getIsScottishCase());
    }

    @Test
    public void syaMissingMrnTest() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setMrn(null);
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnDate());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnLateReason());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnMissingReason());
    }

    @Test
    public void syaWithoutNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_NOTIFICATION_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithoutEmailNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_EMAIL_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_EMAIL_NOTIFICATION_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithoutSmsNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_SMS_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_SMS_NOTIFICATION_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithoutRepresentativeTestShouldGenerateAnEmptySubscriptionForRep() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        Subscription representativeSubscription = caseData.getSubscriptions().getRepresentativeSubscription();
        assertNotNull(representativeSubscription);
        assertTrue(StringUtils.isNotEmpty(representativeSubscription.getTya()));
        assertEquals(Subscription.builder()
                .subscribeEmail(NO)
                .subscribeSms(NO)
                .wantSmsNotifications(NO)
                .build(),
            removeTyaNumber(representativeSubscription));
    }

    @Test
    public void syaWithoutHearingTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_HEARING.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_HEARING_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithoutWantsSupportTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_WANTS_SUPPORT.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_WANTS_SUPPORT_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWantsSupportWithoutArrangementsTest() {
        SyaCaseWrapper syaCaseWrapper = WANTS_SUPPORT_WITHOUT_ARRANGEMENTS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WANTS_SUPPORT_WITHOUT_ARRANGEMENTS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWantsSupportWithoutScheduleTest() {
        SyaCaseWrapper syaCaseWrapper = WANTS_SUPPORT_WITHOUT_SCHEDULE.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WANTS_SUPPORT_WITHOUT_SCHEDULE_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaHearingWithoutSupportAndScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaHearingWithoutSupportWithScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaHearingWithSupportWithoutScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithNullHearingOptions() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setSyaHearingOptions(null);
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertNull(caseData.getAppeal().getHearingOptions());
    }

    @Test
    public void syaWithHearingOptions() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_OPTIONS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITH_OPTIONS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithEmptyHearing() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_EMPTY.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITH_SUPPORT_EMPTY_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresent() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(EVIDENCE_DOCUMENT_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresentLanguagePreferenceWelsh() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void shouldRemoveSpacesFromAppellantPhoneNumbers() {
        final SyaCaseWrapper syaCaseWrapper = APPELLANT_PHONE_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(APPELLANT_PHONE_WITHOUT_SPACES_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void shouldRemoveSpacesFromNino() {
        final SyaCaseWrapper syaCaseWrapper = NINO_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(NINO_WITHOUT_SPACES_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        assertJsonEquals(WITHOUT_REGIONAL_PROCESSING_CENTER.getSerializedMessage(), removeTyaNumber(caseData));
        assertEquals(null, caseData.getIsScottishCase());
    }

    @Test
    public void draftSyaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setCaseType("draft");
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        assertNull(caseData.getIsScottishCase());
    }

    @Test
    public void willAddTyaNumberForAppointee() {
        final SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        final SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        assertEquals("Tya number should have a size of 10", 10, caseData.getSubscriptions().getAppointeeSubscription().getTya().length());
    }

    @Test
    public void willConvertReasonsForAppealingInTheCorrectOrder() {
        final SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();

        final Reason reason = new Reason();
        reason.setReasonForAppealing("reasonForAppealing");
        reason.setWhatYouDisagreeWith("whatYouDisagreeWith");
        syaCaseWrapper.getReasonsForAppealing().setReasons(Collections.singletonList(reason));

        final SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        assertEquals(1, caseData.getAppeal().getAppealReasons().getReasons().size());
        assertEquals(reason.getReasonForAppealing(), caseData.getAppeal().getAppealReasons().getReasons().get(0).getValue().getDescription());
        assertEquals(reason.getWhatYouDisagreeWith(), caseData.getAppeal().getAppealReasons().getReasons().get(0).getValue().getReason());
    }

    @Test
    public void syaWithAppointeeAtSameAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);

        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithAppointeeAtDifferentAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithAppointeeAtSameAddressButNoAppointeeContactDetails() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS_CCD.getSerializedMessage(), removeTyaNumber(caseData));
    }

    @Test
    public void syaWithRepHavingALandLineWillNotReceiveSmsNotifications() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS
            .getDeserializeMessage();
        syaCaseWrapper.getRepresentative().getContactDetails().setPhoneNumber("0203 444 4432");
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertFalse("rep should be not sms subscribed",
            caseData.getSubscriptions().getRepresentativeSubscription().isSmsSubscribed());
    }

    @Test
    public void syaWithRepHavingAMobileNumberWillReceiveSmsNotifications() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS
            .getDeserializeMessage();
        syaCaseWrapper.getRepresentative().getContactDetails().setPhoneNumber("07404621944");
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertTrue(caseData.getSubscriptions().getRepresentativeSubscription().isSmsSubscribed());
        assertEquals("mobile numbers should be equal", "+447404621944",
            caseData.getSubscriptions().getRepresentativeSubscription().getMobile());
    }

    @Test
    public void syaWithRepNameUndefinedWillMapToNull() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS
                .getDeserializeMessage();
        syaCaseWrapper.getRepresentative().setFirstName("Undefined");
        syaCaseWrapper.getRepresentative().setLastName("Undefined");
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertNull(caseData.getAppeal().getRep().getName().getFirstName());
        assertNull(caseData.getAppeal().getRep().getName().getLastName());
    }

}
