package uk.gov.hmcts.reform.sscs.domain.wrapper.audiovideo;

public enum AudioVideoState {
    OK("ok"),
    UNREADABLE("unreadable"),
    UNKNOWN("unknown");

    private final String audioVideoState;

    AudioVideoState(String audioVideoState) {
        this.audioVideoState = audioVideoState;
    }

    public static AudioVideoState of(String avState) {
        for (AudioVideoState answerState : values()) {
            if (answerState.audioVideoState.equals(avState)) {
                return answerState;
            }
        }
        throw new IllegalArgumentException("No audioVideoState mapped for [" + avState + "]");
    }

    public String getAudioVideoState() {
        return audioVideoState;
    }
}
