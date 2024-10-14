package uk.gov.hmcts.reform.sscs.model.single.hearing;

import static java.util.Arrays.stream;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.FACE_TO_FACE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.NOT_ATTENDING;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.PAPER;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.TELEPHONE;
import static uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel.VIDEO;

import java.util.List;
import java.util.Optional;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.reference.data.model.HearingChannel;

@Getter
public enum HearingSubChannel {
    INTER(FACE_TO_FACE, List.of("INTER")),
    NA(NOT_ATTENDING, List.of("NA")),
    ONPPRS(PAPER, List.of("ONPPRS")),
    TEL(TELEPHONE, List.of("TEL", "TELBTM", "TELCVP", "TELOTHER", "TELSKYP")),
    VID(VIDEO, List.of("VID", "VIDCVP", "VIDOTHER", "VIDPVL", "VIDSKYPE", "VIDTEAMS", "VIDVHS"));

    private final HearingChannel hearingChannel;
    private final List<String> subChannels;

    HearingSubChannel(HearingChannel hearingChannel, List<String> subChannels) {
        this.hearingChannel = hearingChannel;
        this.subChannels = subChannels;
    }

    public static Optional<HearingSubChannel> getHearingSubChannel(String subChannelName) {
        return stream(values())
                .filter(hearingSubChannel -> hearingSubChannel.getSubChannels().contains(subChannelName))
                .findFirst();
    }
}
