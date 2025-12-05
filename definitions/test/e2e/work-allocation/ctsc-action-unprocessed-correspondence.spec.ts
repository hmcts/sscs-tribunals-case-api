import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';

test.describe('Work Allocation - CTSC - Action Unprocessed Correspondence', { tag: ['@work-allocation'] }, async () => {
  let caseId: string;

  test.beforeEach(async () => {
    caseId = await createCaseBasedOnCaseType('PIP');
  });

  test("As a CTSC Team Leader with case allocator role, assign CTSC - Action Unprocessed Correspondence task to a CTSC admin and they complete it", async ({ ctscActionUnprocessedCorrespondenceSteps }) => {
    test.slow();
    await ctscActionUnprocessedCorrespondenceSteps.createActionUnprocessedCorrespondenceTask(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignActionUnprocessedCorrespondence(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.completeActionUnprocessedCorrespondenceTask(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.verifyActionUnprocessedCorrespondenceTaskCompleted(caseId);
  });

  test('As a CTSC Admin when there are multiple CTSC - Action Unprocessed Correspondence tasks, the first is auto-closed on completion and I can manually close the others', async ({ ctscActionUnprocessedCorrespondenceSteps }) => {
    test.slow();
    await ctscActionUnprocessedCorrespondenceSteps.createMultipleActionUnprocessedCorrespondenceTasks(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignActionUnprocessedCorrespondence(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.completeActionUnprocessedCorrespondenceTask(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.markDuplicateUnprocessedCorrespondenceTasksAsDone();
  });
});