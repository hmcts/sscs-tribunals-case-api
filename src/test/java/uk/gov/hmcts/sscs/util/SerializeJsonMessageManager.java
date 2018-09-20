package uk.gov.hmcts.sscs.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import lombok.Getter;
import lombok.ToString;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Getter
@ToString
public enum SerializeJsonMessageManager {

    APPEAL_RECEIVED("appealReceived.json"),
    APPEAL_RECEIVED_CCD("appealReceivedCcd.json"),
    DWP_RESPOND("dwpRespond.json"),
    DWP_RESPOND_CCD("dwpRespondCcd.json"),
    HEARING_BOOKED("hearingBooked.json"),
    HEARING_BOOKED_CCD("hearingBookedCcd.json"),
    ADJOURNED("adjourned.json"),
    ADJOURNED_CCD("adjournedCcd.json"),
    DORMANT("dormant.json"),
    DORMANT_CCD("dormantCcd.json"),
    HEARING("hearing.json"),
    HEARING_CCD("hearingCcd.json"),
    NEW_HEARING_BOOKED("newHearingBooked.json"),
    NEW_HEARING_BOOKED_CCD("newHearingBookedCcd.json"),
    NOT_PAST_HEARING_BOOKED("notPastHearingBooked.json"),
    NOT_PAST_HEARING_BOOKED_CCD("notPastHearingBookedCcd.json"),
    PAST_HEARING_BOOKED("pastHearingBooked.json"),
    PAST_HEARING_BOOKED_CCD("pastHearingBookedCcd.json"),
    DWP_RESPOND_OVERDUE("dwpRespondOverdue.json"),
    DWP_RESPOND_OVERDUE_CCD("dwpRespondOverdueCcd.json"),
    POSTPONED("postponed.json"),
    POSTPONED_CCD("postponedCcd.json"),
    WITHDRAWN("withdrawn.json"),
    WITHDRAWN_CCD("withdrawnCcd.json"),
    CLOSED("closed.json"),
    CLOSED_CCD("closedCcd.json"),
    LAPSED_REVISED("lapsedRevised.json"),
    LAPSED_REVISED_CCD("lapsedRevisedCcd.json"),
    EMPTY_EVENT_CCD("emptyEventCcd.json"),
    NO_EVENTS_CCD("noEventsCcd.json"),
    APPEAL_CREATED("appealCreated.json"),
    APPEAL_CREATED_CCD("appealCreatedCcd.json"),
    MISSING_HEARING_CCD("missingHearingBookedCcd.json"),
    MISSING_HEARING("missingHearingBooked.json"),
    APPEAL_WITH_HEARING_TYPE("appealWithHearingType.json"),
    APPEAL_WITH_HEARING_TYPE_CCD("appealWithHearingTypeCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_YES("appealWithWantsToAttendYes.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_YES_CCD("appealWithWantsToAttendYesCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_NO("appealWithWantsToAttendYes.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_NO_CCD("appealWithWantsToAttendYesCcd.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT("appealWithWantsToAttendFieldNotPresent.json"),
    APPEAL_WITH_WANTS_TO_ATTEND_IS_NOT_PRESENT_CCD("appealWithWantsToAttendFieldNotPresentInCcd.json"),
    APPEAL_WITH_NO_HEARING_OPTIONS("appealWithNoHearingOptions.json"),
    APPEAL_WITH_NO_HEARING_OPTIONS_IN_CCD("appealWithNoHearingOptionsInCcd.json");

    private final String serializedMessage;

    SerializeJsonMessageManager(String fileName) {
        this.serializedMessage = getSerialisedMessage(fileName);
    }

    private String getSerialisedMessage(String fileName) {
        try {

            ClassLoader classLoader = getClass().getClassLoader();
            File file = new File(classLoader.getResource("tya/" + fileName).getFile());
            return new String(Files.readAllBytes(file.toPath()));

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    public SscsCaseData getDeserializeMessage() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        try {
            return mapper.readValue(this.serializedMessage, SscsCaseData.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
