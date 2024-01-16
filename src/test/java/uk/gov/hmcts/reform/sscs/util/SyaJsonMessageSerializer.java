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
    ALL_DETAILS_DWP_REGIONAL_CENTRE("allDetailsDwpRegionalCentre.json"),
    ALL_DETAILS_CCD("allDetailsCcd.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN("allDetailsNonSaveAndReturn.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_CHILD_SUPPORT("allDetailsNonSaveAndReturnChildSupport.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_SSCS5("allDetailsNonSaveAndReturnSscs5.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_CCD("allDetailsNonSaveAndReturnCcd.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_CHILD_SUPPORT("allDetailsNonSaveAndReturnCcdChildSupport.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_CCD_SSCS5("allDetailsNonSaveAndReturnCcdSscs5.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_NO_MRN_DATE_CCD("allDetailsNonSaveAndReturnNoMrnDateCcd.json"),
    ALL_DETAILS_NON_SAVE_AND_RETURN_WITH_INTERLOC_CCD("allDetailsNonSaveAndReturnWithInterlocCcd.json"),
    ALL_DETAILS_FROM_DRAFT("allDetailsFromDraft.json"),
    ALL_DETAILS_FROM_DRAFT_CCD("allDetailsFromDraftCcd.json"),
    ALL_DETAILS_FROM_DRAFT_NO_MRN_DATE_CCD("allDetailsFromDraftNoMrnDateCcd.json"),
    ALL_DETAILS_FROM_DRAFT_WITH_INTERLOC_CCD("allDetailsFromDraftWithInterlocCcd.json"),
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
    WITHOUT_WANTS_SUPPORT("withoutWantsSupport.json"),
    WITHOUT_WANTS_SUPPORT_CCD("withoutWantsSupportCcd.json"),
    HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING("hearingWithoutSupportAndScheduleHearing.json"),
    HEARING_WITHOUT_SUPPORT_AND_SCHEDULE_HEARING_CCD("hearingWithoutSupportAndScheduleHearingCcd.json"),
    HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING("hearingWithoutSupportWithScheduleHearing.json"),
    HEARING_WITHOUT_SUPPORT_WITH_SCHEDULE_HEARING_CCD("hearingWithoutSupportWithScheduleHearingCcd.json"),
    HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING("hearingWithSupportWithoutScheduleHearing.json"),
    HEARING_WITH_SUPPORT_WITHOUT_SCHEDULE_HEARING_CCD("hearingWithSupportWithoutScheduleHearingCcd.json"),
    HEARING_WITH_SUPPORT_EMPTY("hearingWithSupportEmpty.json"),
    HEARING_WITH_SUPPORT_EMPTY_CCD("hearingWithSupportEmptyCcd.json"),
    HEARING_WITH_OPTIONS("hearingWithOptions.json"),
    HEARING_WITH_OPTIONS_CCD("hearingWithOptionsCcd.json"),
    EVIDENCE_DOCUMENT("appealWithEvidenceDocuments.json"),
    EVIDENCE_DOCUMENT_CCD("appealWithEvidenceDocumentsCcd.json"),
    EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH("appealWithEvidenceDocumentsLanguagePreferenceWelsh.json"),
    EVIDENCE_DOCUMENT_LANGUAGE_PREFERENCE_WELSH_CCD("appealWithEvidenceDocumentsLanguagePreferenceWelshCcd.json"),
    APPELLANT_PHONE_WITH_SPACES("appellantPhoneNumbersWithSpaces.json"),
    APPELLANT_PHONE_WITHOUT_SPACES_CCD("appellantPhoneNumberWithoutSpacesCcd.json"),
    NINO_WITH_SPACES("ninoWithSpaces.json"),
    NINO_WITHOUT_SPACES_CCD("ninoWithoutSpacesCcd.json"),
    WITHOUT_REGIONAL_PROCESSING_CENTER("withoutRpcCcd.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS("allDetailsWithAppointeeWithSameAddress.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_CCD("allDetailsWithAppointeeWithSameAddressCcd.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS("allDetailsWithAppointeeWithSameAddressButNoAppellantContactDetails.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS_CCD("allDetailsWithAppointeeWithSameAddressButNoAppellantContactDetailsCcd.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS("allDetailsWithAppointeeWithDifferentAddress.json"),
    ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS_CCD("allDetailsWithAppointeeWithDifferentAddressCcd.json"),
    APPELLANT_NO_CONTACT_DETAILS("appellantNoContactDetails.json"),
    APPELLANT_NO_CONTACT_DETAILS_CCD("appellantNoContactDetailsCcd.json"),
    WANTS_SUPPORT_WITHOUT_ARRANGEMENTS("wantsSupportWithoutArrangements.json"),
    WANTS_SUPPORT_WITHOUT_ARRANGEMENTS_CCD("wantsSupportWithoutArrangementsCcd.json"),
    WANTS_SUPPORT_WITHOUT_SCHEDULE("wantsSupportWithoutSchedule.json"),
    WANTS_SUPPORT_WITHOUT_SCHEDULE_CCD("wantsSupportWithoutScheduleCcd.json");

    private final String serializedMessage;

    SyaJsonMessageSerializer(String fileName) {
        this.serializedMessage = getSerialisedMessage(fileName);
    }

    private String getSerialisedMessage(String fileName) {
        try {
            return new String(Files.readAllBytes(Paths.get("src/test/resources/sya/" + fileName)));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
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
