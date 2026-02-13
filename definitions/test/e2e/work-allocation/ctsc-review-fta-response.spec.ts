import { test } from '../../lib/steps.factory';
import createCaseBasedOnCaseType from '../../api/client/sscs/factory/appeal.type.factory';

test.describe('Work Allocation - CTSC - Review FTA response Task',{tag: [ '@work-allocation', '@preview-regression']}, async () => {
    let caseId: string;

    test.beforeEach('Case has to be Created', async ({ uploadResponseSteps }) => {
      caseId = await createCaseBasedOnCaseType('PIP');
      await uploadResponseSteps.uploadResponseWithFurtherInfoAsDwpCaseWorker(caseId);
    });

    test('As a CSTC Admin with case allocator role, view Review FTA response CTSC task', async ({ ctscReviewFtaResponseSteps }) => {
      test.slow();
      await ctscReviewFtaResponseSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewReviewFTAResponseTask(caseId);
    });

    test('As a CSTC Admin without case allocator role, view and complete Review FTA Response CTSC task', async ({ ctscReviewFtaResponseSteps }) => {
      test.slow();
      await ctscReviewFtaResponseSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewFTAResponseTask(caseId);
    });

    test('Review FTA Response task is cancelled automatically when case is void', async ({ ctscReviewFtaResponseSteps }) => {
          test.slow();
          await ctscReviewFtaResponseSteps.verifyReviewFTAResponseTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });
});

test.describe('Work Allocation - CTSC - Review FTA response Task when PIP has an Urgent flag & with further info set to No', {tag:'@work-allocation'}, async () => {
    let caseId: string;

    test.beforeEach('Case has to be Created', async ({ uploadResponseSteps }) => {
      caseId = await createCaseBasedOnCaseType('PIP');
      await uploadResponseSteps.uploadResponseWithoutFurtherInfoAsDwpCaseWorkerAndMarkCaseAsUrgent(caseId);
    });
    
    test('As a CSTC Admin with case allocator role, view Review FTA response CTSC task', async ({ ctscReviewFtaResponseSteps }) => {
        test.slow();
        await ctscReviewFtaResponseSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewReviewFTAResponseTask(caseId);
    });

});