package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

@FunctionalInterface
public interface ActivityQuestionLookup {

    ActivityQuestion getByKey(String key);

}
