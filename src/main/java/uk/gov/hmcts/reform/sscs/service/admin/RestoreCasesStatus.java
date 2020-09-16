package uk.gov.hmcts.reform.sscs.service.admin;

import java.util.List;

public class RestoreCasesStatus {

    private int processedCount;
    private int successCount;
    private List<Long> failureIds;
    private boolean completed;

    public RestoreCasesStatus(int processedCount, int successCount, List<Long> failureIds, boolean completed) {
        this.processedCount = processedCount;
        this.successCount = successCount;
        this.failureIds = failureIds;
        this.completed = completed;
    }

    @Override
    public String toString() {
        return "RestoreCasesStatus{"
            + "processedCount=" + processedCount
            + ", successCount=" + successCount
            + ", failureCount=" + failureIds.size()
            + ", failureIds=" + getFailureIds(failureIds)
            + ", completed=" + completed
            + '}';
    }

    private String getFailureIds(List<Long> ids) {
        return ids.toString();
    }

    public boolean isOk() {
        return processedCount == successCount;
    }

    public boolean isCompleted() {
        return completed;
    }
}
