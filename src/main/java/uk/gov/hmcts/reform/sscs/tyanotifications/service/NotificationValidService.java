package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_BUNDLED_LETTER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.config.NotificationEventTypeLists.EVENT_TYPES_FOR_MANDATORY_LETTERS;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;

@Service
public class NotificationValidService {

    private static final String HEARING_TYPE_ONLINE_RESOLUTION = "cor";

    static boolean isMandatoryLetterEventType(NotificationEventType eventType) {
        return EVENT_TYPES_FOR_MANDATORY_LETTERS.contains(eventType);
    }

    static boolean isBundledLetter(NotificationEventType eventType) {
        return EVENT_TYPES_FOR_BUNDLED_LETTER.contains(eventType);
    }

    protected boolean isHearingTypeValidToSendNotification(SscsCaseData sscsCaseData, NotificationEventType eventType) {
        boolean isOralCase = sscsCaseData.getAppeal().getHearingOptions().isWantsToAttendHearing();
        boolean isOnlineHearing = HEARING_TYPE_ONLINE_RESOLUTION.equalsIgnoreCase(sscsCaseData.getAppeal().getHearingType());

        if (isOralCase && !isOnlineHearing && eventType.isSendForOralCase()) {
            return true;
        } else if (!isOralCase && !isOnlineHearing && eventType.isSendForPaperCase()) {
            return true;
        } else {
            return isOnlineHearing && eventType.isSendForCohCase();
        }
    }

    boolean isNotificationStillValidToSend(List<Hearing> hearings, NotificationEventType eventType) {
        return switch (eventType) {
            case HEARING_BOOKED, HEARING_REMINDER -> checkHearingIsInFuture(hearings);
            default -> true;
        };
    }

    boolean checkHearingIsInFuture(List<Hearing> hearings) {
        if (hearings != null && !hearings.isEmpty()) {

            Hearing latestHearing = hearings.get(0);

            LocalDateTime hearingDateTime = latestHearing.getValue().getHearingDateTime();
            if (isNull(hearingDateTime)) {
                hearingDateTime = latestHearing.getValue().getStart();
            }

            String hearingAdjourned = latestHearing.getValue().getAdjourned();
            return LocalDateTime.now().isBefore(hearingDateTime) && !"YES".equalsIgnoreCase(hearingAdjourned);
        } else {
            return false;
        }
    }
}
