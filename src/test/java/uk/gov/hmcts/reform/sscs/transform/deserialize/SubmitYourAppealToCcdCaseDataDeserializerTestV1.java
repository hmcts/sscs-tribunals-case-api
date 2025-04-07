package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static junit.framework.TestCase.assertNull;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseDataV1;
import static uk.gov.hmcts.reform.sscs.util.SscsUtil.getPortsOfEntry;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.APPELLANT_NO_CONTACT_DETAILS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.APPELLANT_NO_CONTACT_DETAILS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.APPELLANT_PHONE_WITHOUT_SPACES_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.APPELLANT_PHONE_WITH_SPACES;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.EVIDENCE_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.EVIDENCE_DOCUMENT_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_OPTIONS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_OPTIONS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_SUPPORT_EMPTY;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_SUPPORT_EMPTY_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.NINO_WITHOUT_SPACES_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.NINO_WITH_SPACES;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WANTS_SUPPORT_WITHOUT_ARRANGEMENTS;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WANTS_SUPPORT_WITHOUT_ARRANGEMENTS_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WANTS_SUPPORT_WITHOUT_SCHEDULE;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WANTS_SUPPORT_WITHOUT_SCHEDULE_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_EMAIL_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_EMAIL_NOTIFICATION_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_HEARING;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_HEARING_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_NOTIFICATION_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_REGIONAL_PROCESSING_CENTER;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_SMS_NOTIFICATION;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_SMS_NOTIFICATION_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_WANTS_SUPPORT;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.WITHOUT_WANTS_SUPPORT_CCD;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Reason;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaBenefitType;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaMrn;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@RunWith(JUnitParamsRunner.class)
public class SubmitYourAppealToCcdCaseDataDeserializerTestV1 {

    private static final String NO = "No";
    public static final String[] IGNORED_PATHS = {
        "jointPartyId",
        "appeal.appellant.appointee.id",
        "appeal.appellant.id",
        "appeal.rep.id",
        "subscriptions.appellantSubscription.tya",
        "subscriptions.appointeeSubscription.tya",
        "subscriptions.representativeSubscription.tya"};

    private RegionalProcessingCenter regionalProcessingCenter;

    @Before
    public void setup() {
        openMocks(this);
        regionalProcessingCenter = getRegionalProcessingCenter();
    }

    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, String region, RegionalProcessingCenter rpc, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV1(syaCaseWrapper, region, rpc, caseAccessManagementEnabled);
    }

    public SscsCaseData callConvertSyaToCcdCaseDataRelevantVersion(SyaCaseWrapper syaCaseWrapper, boolean caseAccessManagementEnabled) {
        return convertSyaToCcdCaseDataV1(syaCaseWrapper, caseAccessManagementEnabled);
    }

    @Test
    public void syaAllDetailsTest() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, regionalProcessingCenter.getName(),
                regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(ALL_DETAILS_CCD.getSerializedMessage());
    }

    @Test
    public void syaAppellantNoContactDetailsTest() {
        SyaCaseWrapper syaCaseWrapper = APPELLANT_NO_CONTACT_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(APPELLANT_NO_CONTACT_DETAILS_CCD.getSerializedMessage());
    }

    @Parameters({"DWP PIP ( 9),PIP,DWP PIP (9)", "null,carersAllowance,null",
        "null,bereavementBenefit,null","null,maternityAllowance,null",
        "null,bereavementSupportPaymentScheme,null"})
    @Test
    public void syaDwpIssuingOfficeTest(String issuingOffice, String benefitCode, String expectedIssuing) {

        String actIssuingOffice = "null".equals(issuingOffice) ? null : issuingOffice;

        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getBenefitType().setCode(benefitCode);
        syaCaseWrapper.getMrn().setDwpIssuingOffice(actIssuingOffice);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertEquals(expectedIssuing, String.valueOf(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice()));
    }

    @Test
    public void givenAUniversalCreditCaseFromSya_thenDefaultIssuingOfficeToNull() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType uc = new SyaBenefitType("Universal Credit Description", "UC");
        syaCaseWrapper.setBenefitType(uc);
        syaCaseWrapper.setMrn(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }


    @Test
    public void givenAIbaNullValueMainlandUkCaseFromSya_thenAddressBuildsCorrectly() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getContactDetails().setInMainlandUk(null);
        syaCaseWrapper.getContactDetails().setAddressLine1("line1");
        syaCaseWrapper.getContactDetails().setAddressLine2("line2");
        syaCaseWrapper.getContactDetails().setTownCity("townCity");
        syaCaseWrapper.getContactDetails().setCounty("county");
        syaCaseWrapper.getContactDetails().setPostCode("postcode");
        syaCaseWrapper.getContactDetails().setPostcodeLookup("postcodeLookup");
        syaCaseWrapper.getContactDetails().setPostcodeAddress("postcodeAddress");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        Address expectedAddress = Address.builder()
                .line1("line1")
                .line2("line2")
                .town("townCity")
                .county("county")
                .postcode("postcode")
                .postcodeLookup("postcodeLookup")
                .postcodeAddress("postcodeAddress")
                .build();
        assertEquals(expectedAddress, caseData.getAppeal().getAppellant().getAddress());
    }

    @Test
    public void givenAIbaMainlandUkCaseFromSya_thenAddressBuildsCorrectly() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getContactDetails().setInMainlandUk(Boolean.TRUE);
        syaCaseWrapper.getContactDetails().setAddressLine1("line1");
        syaCaseWrapper.getContactDetails().setAddressLine2("line2");
        syaCaseWrapper.getContactDetails().setTownCity("townCity");
        syaCaseWrapper.getContactDetails().setCounty("county");
        syaCaseWrapper.getContactDetails().setPostCode("postcode");
        syaCaseWrapper.getContactDetails().setPostcodeLookup("postcodeLookup");
        syaCaseWrapper.getContactDetails().setPostcodeAddress("postcodeAddress");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        Address expectedAddress = Address.builder()
                .line1("line1")
                .line2("line2")
                .town("townCity")
                .county("county")
                .postcode("postcode")
                .postcodeLookup("postcodeLookup")
                .postcodeAddress("postcodeAddress")
                .inMainlandUk(YesNo.YES)
                .build();
        assertEquals(expectedAddress, caseData.getAppeal().getAppellant().getAddress());
    }

    @Test
    public void givenAIbaNonMainlandUkCaseFromSya_thenAddressBuildsCorrectly() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getContactDetails().setInMainlandUk(Boolean.FALSE);
        syaCaseWrapper.getContactDetails().setAddressLine1("line1");
        syaCaseWrapper.getContactDetails().setAddressLine2("line2");
        syaCaseWrapper.getContactDetails().setTownCity("townCity");
        syaCaseWrapper.getContactDetails().setCountry("country");
        syaCaseWrapper.getContactDetails().setPostCode("some international-postcode");
        syaCaseWrapper.getContactDetails().setPortOfEntry("GB000434");

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        DynamicList expectedPortsOfEntry = getPortsOfEntry();
        expectedPortsOfEntry.setValue(SscsUtil.getPortOfEntryFromCode("GB000434"));
        Address expectedAddress = Address.builder()
                .line1("line1")
                .line2("line2")
                .town("townCity")
                .country("country")
                .portOfEntry("GB000434")
                .ukPortOfEntryList(expectedPortsOfEntry)
                .postcode("some international-postcode")
                .inMainlandUk(YesNo.NO)
                .build();
        assertEquals(expectedAddress, caseData.getAppeal().getAppellant().getAddress());
    }

    @Test
    public void givenAIbaNonMainlandUkCaseFromSyaNoPostcode_thenAddressBuildsCorrectly() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getContactDetails().setInMainlandUk(Boolean.FALSE);
        syaCaseWrapper.getContactDetails().setAddressLine1("line1");
        syaCaseWrapper.getContactDetails().setAddressLine2("line2");
        syaCaseWrapper.getContactDetails().setTownCity("townCity");
        syaCaseWrapper.getContactDetails().setCountry("country");
        syaCaseWrapper.getContactDetails().setPostCode(null);
        syaCaseWrapper.getContactDetails().setPortOfEntry("GB000434");

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        DynamicList expectedPortsOfEntry = getPortsOfEntry();
        expectedPortsOfEntry.setValue(SscsUtil.getPortOfEntryFromCode("GB000434"));
        Address expectedAddress = Address.builder()
                .line1("line1")
                .line2("line2")
                .town("townCity")
                .country("country")
                .portOfEntry("GB000434")
                .ukPortOfEntryList(expectedPortsOfEntry)
                .postcode(null)
                .inMainlandUk(YesNo.NO)
                .build();
        assertEquals(expectedAddress, caseData.getAppeal().getAppellant().getAddress());
    }

    @Test
    public void givenAIbaCaseWithIbcRoleFromSya_thenAppellantBuildsWithIbcRole() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getAppellant().setIbcRole("someRole");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertEquals("someRole", caseData.getAppeal().getAppellant().getIbcRole());
    }

    @Test
    public void givenAIbaCaseWithNoIbcRoleFromSya_thenAppellantBuildsCorrectlyWithoutIbcRole() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.getAppellant().setIbcRole(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getAppellant().getIbcRole());
    }

    @Test
    public void givenANonIbaCaseWithNoIbcRoleFromSya_thenAppellantBuildsCorrectlyWithoutIbcRole() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getAppellant().setIbcRole(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getAppellant().getIbcRole());
    }

    @Test
    public void givenAIbaCaseWithNoSyaAppellantFromSya_thenAppellantBuildsCorrectlyWithoutIbcRole() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType benefitType = new SyaBenefitType("Infected Blood Compensation", "infectedBloodCompensation");
        syaCaseWrapper.setBenefitType(benefitType);
        syaCaseWrapper.setAppellant(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getAppellant().getIbcRole());
    }

    @Test
    public void givenANonIbaCaseWithNoSyaAppellantFromSya_thenAppellantBuildsCorrectlyWithoutIbcRole() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setAppellant(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getAppellant().getIbcRole());
    }

    @Test
    @Parameters({"Universal Credit, Universal Credit", "Recovery from Estates, UC Recovery from Estates"})
    public void givenAUniversalCreditCaseFromSyaAndMrnIsPopulated_thenUseTheIssuingOfficeFromSya(String mrnOffice, String expectedOffice) {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SyaBenefitType uc = new SyaBenefitType("Universal Credit Description", "UC");
        syaCaseWrapper.setBenefitType(uc);
        SyaMrn mrn = new SyaMrn();
        mrn.setDwpIssuingOffice(mrnOffice);
        syaCaseWrapper.setMrn(mrn);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertEquals(expectedOffice, caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
    }

    @Parameters({
        "DWP PIP (1),PIP,Newcastle", "DWP PIP (2),PIP,Glasgow", "DWP PIP (3),PIP,Bellevale", "DWP PIP (4),PIP,Glasgow",
        "DWP PIP (5),PIP,Springburn", "DWP PIP (6),PIP,Blackpool", "DWP PIP (7),PIP,Blackpool", "DWP PIP (8),PIP,Blackpool",
        "DWP PIP (9),PIP,Blackpool", "Inverness DRT,ESA,Inverness DRT","DWP PIP (),PIP,Bellevale",
        "DWP PIP (11),PIP,Bellevale", "null,UC,Universal Credit", ",UC,Universal Credit", "null,PIP,Bellevale",
        "null,carersAllowance,Carers Allowance", "DWP PIP (5),carersAllowance,Carers Allowance",
        "null,bereavementBenefit,Bereavement Benefit", ",bereavementBenefit,Bereavement Benefit",
        "null,bereavementSupportPaymentScheme,Bereavement Support Payment", ",bereavementSupportPaymentScheme,Bereavement Support Payment"
    })
    @Test
    public void givenADwpIssuingOffice_shouldMapToTheDwpRegionalCenter(@Nullable String dwpIssuingOffice,
                                                                       @Nullable String benefitCode,
                                                                       @Nullable String expectedDwpRegionalCenter) {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getMrn().setDwpIssuingOffice(dwpIssuingOffice);
        syaCaseWrapper.getBenefitType().setCode(benefitCode);

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, regionalProcessingCenter.getName(),
                regionalProcessingCenter, false);

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

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, rpc.getName(),
                rpc, false);

        assertNull(caseData.getIsScottishCase());
    }

    @Test
    public void syaMissingMrnThenShouldDoNotSetDefaultDwpIssuingOfficeTest() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setMrn(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnDate());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnLateReason());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnMissingReason());
    }

    @Test
    public void syaMissingMrnTestUcBenefitCode() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setMrn(null);
        syaCaseWrapper.getBenefitType().setCode("UC");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getMrnDetails().getDwpIssuingOffice());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnDate());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnLateReason());
        assertNull(caseData.getAppeal().getMrnDetails().getMrnMissingReason());
    }

    @Test
    public void syaWithoutNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_NOTIFICATION_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithoutEmailNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_EMAIL_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_EMAIL_NOTIFICATION_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithoutSmsNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_SMS_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_SMS_NOTIFICATION_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithoutRepresentativeTestShouldGenerateAnEmptySubscriptionForRep() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        Subscription representativeSubscription = caseData.getSubscriptions().getRepresentativeSubscription();
        assertNotNull(representativeSubscription);
        assertTrue(StringUtils.isNotEmpty(representativeSubscription.getTya()));
        Assertions.assertThat(representativeSubscription)
                .extracting("subscribeEmail","subscribeSms","wantSmsNotifications")
                .containsExactly(NO, NO, NO);
    }

    @Test
    public void syaWithoutHearingTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_HEARING.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_HEARING_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithoutWantsSupportTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_WANTS_SUPPORT.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_WANTS_SUPPORT_CCD.getSerializedMessage());
    }

    @Test
    public void syaWantsSupportWithoutArrangementsTest() {
        SyaCaseWrapper syaCaseWrapper = WANTS_SUPPORT_WITHOUT_ARRANGEMENTS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WANTS_SUPPORT_WITHOUT_ARRANGEMENTS_CCD.getSerializedMessage());
    }

    @Test
    public void syaWantsSupportWithoutScheduleTest() {
        SyaCaseWrapper syaCaseWrapper = WANTS_SUPPORT_WITHOUT_SCHEDULE.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WANTS_SUPPORT_WITHOUT_SCHEDULE_CCD.getSerializedMessage());
    }

    @Test
    public void syaHearingWithoutSupportAndScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD.getSerializedMessage());
    }

    @Test
    public void syaHearingWithoutSupportWithScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD.getSerializedMessage());
    }

    @Test
    public void syaHearingWithSupportWithoutScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithNullHearingOptions() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setSyaHearingOptions(null);
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getHearingOptions());
    }

    @Test
    public void syaWithHearingOptions() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_OPTIONS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(HEARING_WITH_OPTIONS_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithEmptyHearing() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_EMPTY.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(HEARING_WITH_SUPPORT_EMPTY_CCD.getSerializedMessage());
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresent() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(EVIDENCE_DOCUMENT_CCD.getSerializedMessage());
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresentLanguagePreferenceWelsh() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH_CCD.getSerializedMessage());
    }

    @Test
    public void shouldRemoveSpacesFromAppellantPhoneNumbers() {
        final SyaCaseWrapper syaCaseWrapper = APPELLANT_PHONE_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(APPELLANT_PHONE_WITHOUT_SPACES_CCD.getSerializedMessage());
    }

    @Test
    public void shouldRemoveSpacesFromNino() {
        final SyaCaseWrapper syaCaseWrapper = NINO_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(NINO_WITHOUT_SPACES_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(WITHOUT_REGIONAL_PROCESSING_CENTER.getSerializedMessage());
        assertNull(caseData.getIsScottishCase());
    }

    @Test
    public void draftSyaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.setCaseType("draft");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, false);
        assertNull(caseData.getIsScottishCase());
    }

    @Test
    public void willAddTyaNumberForAppointee() {
        final SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        final SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, false);
        assertEquals("Tya number should have a size of 10", 10, caseData.getSubscriptions().getAppointeeSubscription().getTya().length());
    }

    @Test
    public void willConvertReasonsForAppealingInTheCorrectOrder() {
        final SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();

        final Reason reason = new Reason();
        reason.setReasonForAppealing("reasonForAppealing");
        reason.setWhatYouDisagreeWith("whatYouDisagreeWith");
        syaCaseWrapper.getReasonsForAppealing().setReasons(Collections.singletonList(reason));

        final SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, false);
        assertEquals(1, caseData.getAppeal().getAppealReasons().getReasons().size());
        assertEquals(reason.getReasonForAppealing(), caseData.getAppeal().getAppealReasons().getReasons().get(0).getValue().getDescription());
        assertEquals(reason.getWhatYouDisagreeWith(), caseData.getAppeal().getAppealReasons().getReasons().get(0).getValue().getReason());
    }

    @Test
    public void syaWithAppointeeAtSameAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);

        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithAppointeeAtDifferentAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithAppointeeAtSameAddressButNoAppointeeContactDetails() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertThatJson(caseData)
                .whenIgnoringPaths(IGNORED_PATHS)
                .isEqualTo(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS_CCD.getSerializedMessage());
    }

    @Test
    public void syaWithRepHavingALandLineWillNotReceiveSmsNotifications() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS
                .getDeserializeMessage();
        syaCaseWrapper.getRepresentative().getContactDetails().setPhoneNumber("0203 444 4432");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertFalse("rep should be not sms subscribed",
                caseData.getSubscriptions().getRepresentativeSubscription().isSmsSubscribed());
    }

    @Test
    public void syaWithRepHavingAMobileNumberWillReceiveSmsNotifications() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS
                .getDeserializeMessage();
        syaCaseWrapper.getRepresentative().getContactDetails().setPhoneNumber("07404621944");
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter, false);
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
        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, regionalProcessingCenter.getName(), regionalProcessingCenter, false);
        assertNull(caseData.getAppeal().getRep().getName().getFirstName());
        assertNull(caseData.getAppeal().getRep().getName().getLastName());
    }

    @Test
    public void givenSyaWithAppellantName_shouldSetCaseAccessManagementFields() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        syaCaseWrapper.getAppellant().setFirstName("George");
        syaCaseWrapper.getAppellant().setLastName("Foreman");

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, regionalProcessingCenter.getName(), regionalProcessingCenter, true);

        assertEquals("George Foreman", caseData.getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("George Foreman", caseData.getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("George Foreman", caseData.getCaseAccessManagementFields().getCaseNamePublic());

        assertEquals("Personal Independence Payment", caseData.getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("PIP", caseData.getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("personalIndependencePayment", caseData.getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("DWP", caseData.getCaseAccessManagementFields().getOgdType());
    }

    @Test
    public void givenSyaWithSscs5BenefitType_shouldSetCaseAccessManagementFields() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5.getDeserializeMessage();

        SscsCaseData caseData = callConvertSyaToCcdCaseDataRelevantVersion(syaCaseWrapper, regionalProcessingCenter.getName(), regionalProcessingCenter, true);

        assertEquals("Joe Bloggs", caseData.getCaseAccessManagementFields().getCaseNameHmctsInternal());
        assertEquals("Joe Bloggs", caseData.getCaseAccessManagementFields().getCaseNameHmctsRestricted());
        assertEquals("Joe Bloggs", caseData.getCaseAccessManagementFields().getCaseNamePublic());

        assertEquals("Tax Credit", caseData.getCaseAccessManagementFields().getCaseManagementCategory().getValue().getLabel());
        assertEquals("taxCredit", caseData.getCaseAccessManagementFields().getCaseManagementCategory().getValue().getCode());
        assertEquals("taxCredit", caseData.getCaseAccessManagementFields().getCaseAccessCategory());
        assertEquals("HMRC", caseData.getCaseAccessManagementFields().getOgdType());
    }
}
