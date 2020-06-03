package uk.gov.hmcts.reform.sscs.domain.wrapper;

public enum AnswerState {
    draft("answer_drafted"),
    submitted("answer_submitted"),
    unanswered("unanswered");

    private final String cohAnswerState;

    AnswerState(String cohAnswerState) {
        this.cohAnswerState = cohAnswerState;
    }

    public String getCohAnswerState() {
        return cohAnswerState;
    }

    public static AnswerState of(String cohAnswerState) {
        for (AnswerState answerState : values()) {
            if (answerState.cohAnswerState.equals(cohAnswerState)) {
                return answerState;
            }
        }
        throw new IllegalArgumentException("No AnswerState mapped for [" + cohAnswerState + "]");
    }
}
