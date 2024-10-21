import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";


let caseId : string;

test.describe.serial('WA - Referred by Judge task initiation', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Allocate case to legal ops role and perform Send case to TCW event", async ({ sendCaseToTcwSteps , referredByJudgeSteps }) => {
        
        await referredByJudgeSteps.allocateCaseToLegalOpsRole(caseId);
        await sendCaseToTcwSteps.performSendCaseToTcw(caseId);
    });

    test("As a TCW can view the auto assigned Referred by Judge task", async ({ referredByJudgeSteps }) => {
        test.slow();
        await referredByJudgeSteps.verifyTcwWithoutCaseAllocatorRoleCanViewReviewReferredByJudgeTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Referred by Judge task completion tests', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a TCW with case allocator role, assign Referred by Judge task to another TCW", async ({ sendCaseToTcwSteps , referredByJudgeSteps }) => {
        
        await sendCaseToTcwSteps.performSendCaseToTcw(caseId);
        test.slow();
        await referredByJudgeSteps.verifyTcwWithCaseAllocatorRoleCanViewAndAssignReferredByJudgeTask(caseId);
    });

    test("As a TCW, view and complete the assigned Referred by Judge task", async ({
        referredByJudgeSteps}) => {

        test.slow();
        await referredByJudgeSteps.verifyTcwAsAnAssignedUserForReferredByJudgeTaskCanViewAndCompleteTheTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Referred by Judge task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Review Referred by Judge task is cancelled automatically when case is void", async ({ sendCaseToTcwSteps , referredByJudgeSteps }) => {
        
        await sendCaseToTcwSteps.performSendCaseToTcw(caseId);
        test.slow();
        await referredByJudgeSteps.verifyReferredByJudgeTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

