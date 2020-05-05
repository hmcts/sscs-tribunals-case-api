package uk.gov.hmcts.reform.sscs.service.pdf.util;

import static uk.gov.hmcts.reform.sscs.service.pdf.util.PdfDateUtil.reformatDate;
import static uk.gov.hmcts.reform.sscs.util.DecodeJsonUtil.decodeStringWithWhitespace;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.domain.wrapper.Decision;
import uk.gov.hmcts.reform.sscs.domain.wrapper.OnlineHearing;

@Service
public class OnlineHearingDateReformatter {
    public OnlineHearing getReformattedOnlineHearing(OnlineHearing onlineHearing) {
        Decision originalDecision = onlineHearing.getDecision();
        String reformattedStartDate = reformatDate(originalDecision.getStartDate());
        String reformattedEndDate = reformatDate(originalDecision.getEndDate());

        Decision newDecision = new Decision(
                originalDecision.getDecisionState(),
                originalDecision.getDecisionStateDateTime(),
                originalDecision.getAppellantReply(),
                originalDecision.getAppellantReplyDateTime(),
                reformattedStartDate,
                reformattedEndDate,
                originalDecision.getDecisionRates(),
                decodeStringWithWhitespace(originalDecision.getReason()),
                originalDecision.getActivities()
        );
        return new OnlineHearing(
                onlineHearing.getOnlineHearingId(),
                onlineHearing.getAppellantName(),
                onlineHearing.getCaseReference(),
                onlineHearing.getCaseId(),
                newDecision,
                onlineHearing.getFinalDecision(),
                onlineHearing.isHasFinalDecision(),
                onlineHearing.getAppellantDetails(),
                onlineHearing.getAppealDetails());
    }
}
