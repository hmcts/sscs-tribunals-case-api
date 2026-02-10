package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision;

@FunctionalInterface
public interface ActivityQuestionLookup {

    ActivityQuestion getByKey(String key);

}
