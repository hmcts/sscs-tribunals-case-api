package uk.gov.hmcts.sscs.transform.deserialize;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static uk.gov.hmcts.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.ccd.CaseData;
import uk.gov.hmcts.sscs.model.tya.RegionalProcessingCenter;

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
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(ALL_DETAILS_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithoutNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_NOTIFICATION.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_NOTIFICATION_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithoutEmailNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_EMAIL_NOTIFICATION.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_EMAIL_NOTIFICATION_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithoutSmsNotificationTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_SMS_NOTIFICATION.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_SMS_NOTIFICATION_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithoutRepresentativeTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_REPRESENTATIVE.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_REPRESENTATIVE_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithoutHearingTest() {
        SyaCaseWrapper syaCaseWrapper = WITHOUT_HEARING.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(WITHOUT_HEARING_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaHearingWithoutSupportAndScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaHearingWithoutSupportWithScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaHearingWithSupportWithoutScheduleHearingTest() {
        SyaCaseWrapper syaCaseWrapper = HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void shouldAddEvidenceDocumentDetailsIfPresent() {
        SyaCaseWrapper syaCaseWrapper = EVIDENCE_DOCUMENT.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(EVIDENCE_DOCUMENT_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void shouldRemoveSpacesFromAppellantPhoneNumbers() {
        final SyaCaseWrapper syaCaseWrapper = APPELLANT_PHONE_WITH_SPACES.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper,
                regionalProcessingCenter.getName(), regionalProcessingCenter);
        assertJsonEquals(APPELLANT_PHONE_WITHOUT_SPACES_CCD.getSerializedMessage(), getJson(caseData));
    }

    @Test
    public void syaWithNoRpc() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        CaseData caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(syaCaseWrapper);
        assertJsonEquals(WITHOUT_REGIONAL_PROCESSING_CENTER.getSerializedMessage(), getJson(caseData));
    }

    private String getJson(CaseData caseData) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(caseData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}