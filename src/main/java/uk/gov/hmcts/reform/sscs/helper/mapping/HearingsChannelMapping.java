package uk.gov.hmcts.reform.sscs.helper.mapping;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;

import java.util.Arrays;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;
import uk.gov.hmcts.reform.sscs.utility.HearingChannelUtil;

@Slf4j
@SuppressWarnings("PMD.CyclomaticComplexity")
public final class HearingsChannelMapping {

    private HearingsChannelMapping() {

    }

    public static List<HearingChannel> getHearingChannels(@Valid SscsCaseData caseData) {
        return List.of(HearingChannelUtil.getHearingChannel(caseData));
    }

    public static List<HearingChannel> getHearingChannels(@Valid SscsCaseData caseData, boolean adjournmentInProgress) {
        return List.of(getHearingChannel(caseData, adjournmentInProgress));
    }

    public static HearingChannel getHearingChannel(@Valid SscsCaseData caseData, boolean adjournmentInProgress) {
        log.debug("Adjournment for Case ID {} next hearing channel {}",
                  caseData.getCcdCaseId(), caseData.getAdjournment().getTypeOfNextHearing());

        if (adjournmentInProgress && nonNull(caseData.getAdjournment().getTypeOfNextHearing())) {
            return getAdjournmentNextHearingChannel(caseData);
        }

        return HearingChannelUtil.getHearingChannel(caseData);
    }

    private static HearingChannel getAdjournmentNextHearingChannel(SscsCaseData caseData) {
        log.debug("Adjournment Next hearing type {} for case code {}",
                  caseData.getAdjournment().getTypeOfNextHearing(), caseData.getCaseCode());

        return Arrays.stream(HearingChannel.values())
            .filter(hearingChannel ->
                caseData.getAdjournment().getTypeOfNextHearing().getHearingChannel().getValueTribunals()
                    .equalsIgnoreCase(hearingChannel.getValueTribunals()))
            .findFirst().orElse(PAPER);
    }
}
