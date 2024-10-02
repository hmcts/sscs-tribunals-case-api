import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";


let caseId : string;

test.describe.serial('WA - Referred by Admin task initiation', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Allocate case to legal ops role and perform Send to interloc event", async ({ sendToInterlocSteps , referredByAdminSteps }) => {
        
        await referredByAdminSteps.allocateCaseToLegalOpsRole(caseId);
        await sendToInterlocSteps.performSendToInterloc(caseId);
    });

    test("As a TCW can view the auto assigned Referred by Admin task", async ({ referredByAdminSteps }) => {
        test.slow();
        await referredByAdminSteps.verifyTcwWithoutCaseAllocatorRoleCanViewReviewReferredByAdminTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Referred by Admin task completion tests', {tag: '@work-allocation'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Allocate case to legal ops role and perform Send to interloc event", async ({ sendToInterlocSteps }) => {
        
        await sendToInterlocSteps.performSendToInterloc(caseId);
    });

    test("As a TCW with case allocator role, assign Referred By Admin task to another TCW", async ({
        referredByAdminSteps}) => {

        test.slow();
        await referredByAdminSteps.verifyTcwWithCaseAllocatorRoleCanViewAndAssignReferredByAdminTask(caseId);
    });

    test("As a TCW, view and complete the assigned Referred by Admin task", async ({
        referredByAdminSteps}) => {

        test.slow();
        await referredByAdminSteps.verifyTcwAsAnAssignedUserForReferredByAdminTaskCanViewAndCompleteTheTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Referred by Admin task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Review Referred By Admin task is cancelled automatically when case is void", async ({ sendToInterlocSteps , referredByAdminSteps }) => {
        
        await sendToInterlocSteps.performSendToInterloc(caseId);
        test.slow();
        await referredByAdminSteps.verifyReferredByAdminTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

