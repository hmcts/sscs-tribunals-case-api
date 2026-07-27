import { test } from "../../lib/steps.factory";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";

let caseId: string;

test.describe.serial('Work Allocation - CTSC - Review Admin action task tests', { tag: ['@work-allocation'] }, async () => {

    test("As a CSTC Admin without case allocator role, review admin action task", async ({ ctscReviewAdminActionSteps }) => {
        test.slow();
        caseId = await createCaseBasedOnCaseType('PIP');
        await ctscReviewAdminActionSteps.createReviewAdminActionTask(caseId);
        await ctscReviewAdminActionSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewAdminActionTask(caseId);
    });

    test("As a CSTC Admin with case allocator role, review admin action task", async ({ ctscReviewAdminActionSteps }) => {
        test.slow();
        await ctscReviewAdminActionSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewReviewAdminActionTask(caseId);
    });

    test("As a CSTC Administrator, complete review admin action task", async ({ ctscReviewAdminActionSteps }) => {
        test.slow();
        await ctscReviewAdminActionSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewAdminActionTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

test("Review Admin Action task is cancelled automatically when case is void", { tag: ['@work-allocation'] }, async ({ ctscReviewAdminActionSteps }) => {
    test.slow();
    caseId = await createCaseBasedOnCaseType('PIP');
    await ctscReviewAdminActionSteps.createReviewAdminActionTask(caseId);
    await ctscReviewAdminActionSteps.verifyReviewAdminActionTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
});