import { test } from "../../lib/steps.factory";
import createCaseBasedOnCaseType from "../../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../../api/client/sscs/appeal.event";

test.describe.serial('WA - Review Listing Error CTSC task initiation and completion tests', { tag: '@work-allocation' }, async () => {
    let caseId: string;

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a CTSC Admin without case allocator role, review listing error task", async ({ ctscReviewListingErrorSteps }) => {
        test.slow();
        await ctscReviewListingErrorSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanViewReviewListingErrorTask(caseId);
    });

    test("As a CTSC Admin with case allocator role, review listing error task", async ({ ctscReviewListingErrorSteps }) => {
        test.slow();
        await ctscReviewListingErrorSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewReviewListingErrorTask(caseId);
    });

    test("As a CTSC Administrator, complete listing error task", async ({ ctscReviewListingErrorSteps }) => {
        test.slow();
        await ctscReviewListingErrorSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewListingErrorTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

test.describe('WA - Review Listing Error CTSC task automatic cancellation when case is void', { tag: '@work-allocation' }, async () => {
    let caseId: string;

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Review listing error task is cancelled automatically when case is void", async ({ ctscReviewListingErrorSteps }) => {
        test.slow();
        await ctscReviewListingErrorSteps.verifyReviewListingErrorTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});