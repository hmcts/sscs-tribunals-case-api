package uk.gov.hmcts.reform.sscs.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

@Getter
@ToString
public enum SyaJsonMessageSerializer {

    ALL_DETAILS("allDetails.json"),
    ALL_DETAILS_CCD("allDetailsCcd.json"),
    WITHOUT_NOTIFICATION("withoutNotification.json"),
    WITHOUT_NOTIFICATION_CCD("withoutNotificationCcd.json"),
    WITHOUT_EMAIL_NOTIFICATION("withoutEmailNotification.json"),
    WITHOUT_EMAIL_NOTIFICATION_CCD("withoutEmailNotificationCcd.json"),
    WITHOUT_SMS_NOTIFICATION("withoutSmsNotification.json"),
    WITHOUT_SMS_NOTIFICATION_CCD("withoutSmsNotificationCcd.json"),
    WITHOUT_REPRESENTATIVE("withoutRepresentative.json"),
    WITHOUT_REPRESENTATIVE_CCD("withoutRepresentativeCcd.json"),
    WITHOUT_HEARING("withoutHearing.json"),
    WITHOUT_HEARING_CCD("withoutHearingCcd.json"),
    HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING("hearingWithoutSupportAndScheduleHearing.json"),
    HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD("hearingWithoutSupportAndScheduleHearingCcd.json"),
    HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING("hearingWithoutSupportWithScheduleHearing.json"),
    HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD("hearingWithoutSupportWithScheduleHearingCcd.json"),
    HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING("hearingWithSupportWithoutScheduleHearing.json"),
    HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD("hearingWithSupportWithoutScheduleHearingCcd.json"),
    EVIDENCE_DOCUMENT("appealWithEvidenceDocuments.json"),
    EVIDENCE_DOCUMENT_CCD("appealWithEvidenceDocumentsCcd.json"),
    APPELLANT_PHONE_WITH_SPACES("appellantPhoneNumbersWithSpaces.json"),
    APPELLANT_PHONE_WITHOUT_SPACES_CCD("appellantPhoneNumberWithoutSpacesCcd.json"),
    NINO_WITH_SPACES("ninoWithSpaces.json"),
    NINO_WITHOUT_SPACES_CCD("ninoWithoutSpacesCcd.json"),
    WITHOUT_REGIONAL_PROCESSING_CENTER("withoutRpcCcd.json"),
    ALL_DETAILS_WITH_APPOINTEE("allDetailsWithAppointee.json");

    private final String serializedMessage;

    SyaJsonMessageSerializer(String fileName) {
        this.serializedMessage = getSerialisedMessage(fileName);
    }

    private String getSerialisedMessage(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/sya/" + fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public SyaCaseWrapper getDeserializeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            return mapper.readValue(this.serializedMessage, SyaCaseWrapper.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
