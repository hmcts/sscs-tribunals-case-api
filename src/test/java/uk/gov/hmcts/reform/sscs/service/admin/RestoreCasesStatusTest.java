package uk.gov.hmcts.reform.sscs.service.admin;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RestoreCasesStatusTest {

    @Test
    public void testStatusWithFailures() {
        int processedCount = 10;
        int successCount = 4;
        List<Long> failureIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        boolean completed = false;

        RestoreCasesStatus status = new RestoreCasesStatus(processedCount, successCount, failureIds, completed);

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=10, "
            + "successCount=4, failureCount=6, failureIds=[1, 2, 3, 4, 5, 6], "
            + "completed=false}", status.toString());

    }

    @Test
    public void testStatusWithFailure() {
        int processedCount = 10;
        int successCount = 9;
        List<Long> failureIds = Arrays.asList(1L);
        boolean completed = false;

        RestoreCasesStatus status = new RestoreCasesStatus(processedCount, successCount, failureIds, completed);

        Assert.assertFalse(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=10, "
            + "successCount=9, failureCount=1, failureIds=[1], "
            + "completed=false}", status.toString());

    }

    @Test
    public void testStatusWithFailureWhenCompleted() {
        int processedCount = 10;
        int successCount = 9;
        List<Long> failureIds = Arrays.asList(1L);
        boolean completed = true;

        RestoreCasesStatus status = new RestoreCasesStatus(processedCount, successCount, failureIds, completed);

        Assert.assertTrue(status.isCompleted());
        Assert.assertFalse(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=10, "
            + "successCount=9, failureCount=1, failureIds=[1], "
            + "completed=true}", status.toString());

    }

    @Test
    public void testStatusWithoutFailures() {
        int processedCount = 10;
        int successCount = 10;
        List<Long> failureIds = Arrays.asList();
        boolean completed = false;

        RestoreCasesStatus status = new RestoreCasesStatus(processedCount, successCount, failureIds, completed);

        Assert.assertFalse(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=10, "
            + "successCount=10, failureCount=0, failureIds=[], "
            + "completed=false}", status.toString());

    }

    @Test
    public void testStatusWithoutFailuresWhenCompleted() {
        int processedCount = 10;
        int successCount = 10;
        List<Long> failureIds = Arrays.asList();
        boolean completed = true;

        RestoreCasesStatus status = new RestoreCasesStatus(processedCount, successCount, failureIds, completed);

        Assert.assertTrue(status.isCompleted());
        Assert.assertTrue(status.isOk());
        Assert.assertEquals("RestoreCasesStatus{processedCount=10, "
            + "successCount=10, failureCount=0, failureIds=[], "
            + "completed=true}", status.toString());

    }
}
