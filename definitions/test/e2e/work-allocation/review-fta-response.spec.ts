import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';
import task from '../../pages/content/review.fta.response.task_en.json';


test.describe('Work Allocation - Review FTA response CTSC Task', { tag: ['@work-allocation', '@preview-regression', '@CI-2279'] }, () => {
  let caseId: string;

  test.beforeEach(async ({ workAllocationTaskSteps }) => {
    caseId = await createCaseBasedOnCaseType('PIP');
    await workAllocationTaskSteps.dwpUploadsFTAResponse(caseId);
  });

  test('As a CTSC Admin without case allocator role I can view the Review FTA response task', async ({ workAllocationTaskSteps }) => {
    test.slow();
    await workAllocationTaskSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewTask(caseId, task);
  });

  // test("As a CTSC Team Leader with case allocator role, assign Incomplete Appeal task to a CTSC admin", async ({ workAllocationTaskSteps }) => {
  //   test.slow();
  //   await workAllocationTaskSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewAndAssignReviewTask(caseId, task);
  // });
});
