import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import { credentials } from '../../config/config';


let caseId: string;
test.describe('Work Allocation Feature', () => {
  test('As a CTSC Team Leader I can assign a Review Incomplete Appeal task to myself and complete it', async ({ reviewIncompleteApplicationSteps }) => {
    caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteApplicationSteps.assignTaskToSelf(caseId);
    await reviewIncompleteApplicationSteps.completeTask(caseId)
  });
});
