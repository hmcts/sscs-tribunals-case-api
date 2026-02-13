import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import { AppealDormant } from '../../fixtures/steps/appeal.dormant';
import { credentials } from '../../config/config';

let caseId: string;

test.describe('Work Allocation - CTSC - Action Unprocessed Correspondence - Dormant/Post Hearing', { tag: ['@work-allocation'] }, async () => {
  
   const isDormant = true;

   test.beforeEach(async ({ page }) => {
    caseId = await createCaseBasedOnCaseType('PIP');

    // Move the case to Dormant state
    const appealDormant = new AppealDormant(page);
    await appealDormant.performAppealDormant(caseId);
  });

  test("Assign and complete CTSC - Action Unprocessed Correspondence - Dormant/Post Hearing", async ({ ctscActionUnprocessedCorrespondenceSteps }) => {
    test.slow();
    
    // Create the task
    await ctscActionUnprocessedCorrespondenceSteps.createActionUnprocessedCorrespondenceTask(caseId, isDormant);

    // Assign and complete the task
    await ctscActionUnprocessedCorrespondenceSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignActionUnprocessedCorrespondence(caseId, isDormant);
    await ctscActionUnprocessedCorrespondenceSteps.completeActionUnprocessedCorrespondenceTask(caseId);

    // Verify task completion
    await ctscActionUnprocessedCorrespondenceSteps.verifyActionUnprocessedCorrespondenceTaskCompleted(caseId);
  });

   test('As a CTSC Admin when there are multiple CTSC - Action Unprocessed Correspondence - Dormant/Post Hearing, the first is auto-closed on completion and I can manually close the others', async ({ ctscActionUnprocessedCorrespondenceSteps }) => {
    test.slow();

    //Create multiple tasks
    await ctscActionUnprocessedCorrespondenceSteps.createMultipleActionUnprocessedCorrespondenceTasks(caseId, 3, isDormant);
    await ctscActionUnprocessedCorrespondenceSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignActionUnprocessedCorrespondence(caseId, isDormant);
    await ctscActionUnprocessedCorrespondenceSteps.completeActionUnprocessedCorrespondenceTask(caseId);
    await ctscActionUnprocessedCorrespondenceSteps.markDuplicateUnprocessedCorrespondenceTasksAsDone(isDormant);
  });
});