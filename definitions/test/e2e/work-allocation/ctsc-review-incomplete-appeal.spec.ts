import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';

test.describe('Work Allocation - Review Incomplete Appeal CTSC Task', { tag: ['@work-allocation', '@preview-regression'] }, async () => {
  let caseId: string;

  test('As a CTSC Admin without case allocator role I can view the Review Incomplete Appeal task', async ({ reviewIncompleteAppealSteps }) => {
    test.slow();
    caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteAppealSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewIncompleteAppealTask(caseId);
  });

  test("As a CTSC Team Leader with case allocator role, assign Incomplete Appeal task to a CTSC admin", async ({ reviewIncompleteAppealSteps }) => {
    test.slow();
    caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteAppealSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignReviewIncompleteAppealTask(caseId);
  });

  test('As a CTSC Team Leader with case allocator role I can assign a Review Incomplete Appeal task to myself and complete it', async ({ reviewIncompleteAppealSteps }) => {
    test.slow();
    caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteAppealSteps.assignTaskToSelf(caseId);
    await reviewIncompleteAppealSteps.completeReviewIncompleteAppealTask(caseId)
  });

  test("Review Incomplete Appeal task is cancelled automatically when case is void", async ({ reviewIncompleteAppealSteps }) => {
    test.slow();
    caseId = await createCaseBasedOnCaseType('PIPINCOMPLETE');
    await reviewIncompleteAppealSteps.verifyReviewIncompleteAppealTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
  });
});
