import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';


test.describe('Work Allocation Feature', { tag: '@work-allocation' }, async () => {
  test('As a CTSC Team Leader I can assign a Review Incomplete Appeal task to myself and complete it', async ({ reviewIncompleteApplicationSteps }) => {
    let caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteApplicationSteps.assignTaskToSelf(caseId);
    await reviewIncompleteApplicationSteps.completeTask(caseId)
  });
});
