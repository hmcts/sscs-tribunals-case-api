package uk.gov.hmcts.reform.sscs.transform.deserialize;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

public class SubmitYourAppealToCcdCaseDataDeserializerTest {

    private SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer;
    private RegionalProcessingCenter regionalProcessingCenter;

    @Before
    public void setUp() {
        submitYourAppealToCcdCaseDataDeserializer = new SubmitYourAppealToCcdCaseDataDeserializer();
        regionalProcessingCenter = getRegionalProcessingCenter();
    }

    @Test
    public void syaAllDetailsTest() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithoutNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_NOTIFICATION_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithoutEmailNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_EMAIL_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_EMAIL_NOTIFICATION_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithoutSmsNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_SMS_NOTIFICATION.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_SMS_NOTIFICATION_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithoutRepresentativeTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_REPRESENTATIVE_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithoutHearingTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_HEARING.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_HEARING_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaHearingWithoutSupportAndScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaHearingWithoutSupportWithScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaHearingWithSupportWithoutScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresent() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(EVIDENCE_DOCUMENT_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void shouldRemoveSpacesFromAppellantPhoneNumbers() {
        final SyaCaseWrapper syaCaseWrapper = APPELLANT_PHONE_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(APPELLANT_PHONE_WITHOUT_SPACES_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void shouldRemoveSpacesFromNino() {
        final SyaCaseWrapper syaCaseWrapper = NINO_WITH_SPACES.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(NINO_WITHOUT_SPACES_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper);
        assertJsonEquals(WITHOUT_REGIONAL_PROCESSING_CENTER.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithAppointeeAtSameAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithAppointeeAtDifferentAddress() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS_CCD.getSerializedMessage(), caseData);
    }

    @Test
    public void syaWithAppointeeAtSameAddressButNoAppointeeContactDetails() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS.getDeserializeMessage();
        SscsCaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
            regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS_CCD.getSerializedMessage(), caseData);
    }
}
