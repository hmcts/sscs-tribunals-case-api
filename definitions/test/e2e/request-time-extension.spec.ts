import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";
let caseId : string;


test.describe("Request time extension test", {tag: '@nightly-pipeline'}, async() => {

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });
    
    test("Verify DWP user can succesfully request extension on a appeal", async ({ requestTimeExtensionSteps }) => {
        await requestTimeExtensionSteps.performAndVerifyRequestTimeExtension(caseId);
    });
    
    
     test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
     });
        
});
test.describe.serial('WA - Review FTA Time Extension Request task initiation and completion tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a TCW without case allocator role, Review FTA Time Extension Request task", async ({
        requestTimeExtensionSteps}) => {

        test.slow();
        await requestTimeExtensionSteps.verifyTcwWithoutCaseAllocatorRoleCanViewReviewRequestTimeExtensionTask(caseId);
    });

    test("As a TCW with case allocator role, assign Review FTA Time Extension Request to another TCW", async ({
        requestTimeExtensionSteps}) => {

        test.slow();
        await requestTimeExtensionSteps.verifyTcwWithCaseAllocatorRoleCanViewAndAssignRequestTimeExtensionTask(caseId);
    });

    test("As a TCW, view and complete the assigned Review FTA Time Extension Request task", async ({
        requestTimeExtensionSteps}) => {

        test.slow();
        await requestTimeExtensionSteps.verifyTcwAsAnAssignedUserForReviewFtaTimeExtensionRequestTaskCanViewAndCompleteTheTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
         await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Review FTA Time Extension Request task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a TCW without case allocator role, Review Request Time Extension task", async ({requestTimeExtensionSteps}) => {
        await requestTimeExtensionSteps.performAndVerifyRequestTimeExtension(caseId);
    });

    test("Review Request Time Extension task is cancelled automatically when case is void", async ({requestTimeExtensionSteps}) => {
        await requestTimeExtensionSteps.verifyRequestTimeExtensionTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

