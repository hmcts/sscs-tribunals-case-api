import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

test.describe('WA - Allocate Case Roles and Create Bundle RPC task initiation and completion tests', {
    tag: '@work-allocation'
}, async () => {

    test.describe.serial('WA - Task initiation and completion by Regional Center Admin', async () => { 

        let caseId : string;

        test.beforeAll("Case has to be Created",async () => {
            caseId = await createCaseBasedOnCaseType('PIP');
        });

        test("Prepare case for hearing", async ({
            uploadResponseSteps,
            prepareCaseForHearingSteps}) => {

            test.slow();
            await uploadResponseSteps.uploadResponseWithoutFurtherInfoAsDwpCaseWorker(caseId);
            await prepareCaseForHearingSteps.prepareCaseForHearing(caseId, true);
        });

        test("Regional Center Admin, views and completes the Allocate Case Roles and Create Bundle task", async ({
            prepareCaseForHearingSteps }) => {

            test.slow();
            await prepareCaseForHearingSteps.verifyRegionalCenterAdminCanViewAndCompleteTheAllocateCaseRolesAndCreateBundleTask(caseId);
        });

        test.afterAll("Case has to be set to Dormant", async () => {
            await performAppealDormantOnCase(caseId);
        });
    });

    test.describe.serial('WA - Task initiation and completion by Regional Center Team Leader', async () => { 

        let caseId : string;

        test.beforeAll("Case has to be Created",async () => {
            caseId = await createCaseBasedOnCaseType('PIP');
        });

        test("Prepare case for hearing", async ({
            uploadResponseSteps,
            prepareCaseForHearingSteps}) => {

            test.slow();
            await uploadResponseSteps.uploadResponseWithoutFurtherInfoAsDwpCaseWorker(caseId);
            await prepareCaseForHearingSteps.prepareCaseForHearing(caseId, true);
        });

        test("Regional Center Team Leader, views and completes the Allocate Case Roles and Create Bundle task", async ({
            prepareCaseForHearingSteps }) => {

            test.slow();
            await prepareCaseForHearingSteps.verifyRegionalCenterTeamLeaderCanViewAndCompleteTheAllocateCaseRolesAndCreateBundleTask(caseId);
        });

        test.afterAll("Case has to be set to Dormant", async () => {
            await performAppealDormantOnCase(caseId);
        });
    });
});

test.describe.serial('WA - Allocate Case Roles and Create Bundle RPC task automatic cancellation when case is struck out', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;
    
    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Prepare case for hearing", async ({
        uploadResponseSteps,
        prepareCaseForHearingSteps}) => {

        test.slow();
        await uploadResponseSteps.uploadResponseWithoutFurtherInfoAsDwpCaseWorker(caseId);
        await prepareCaseForHearingSteps.prepareCaseForHearing(caseId, true);
    });

    test("Regional Center Admin, views and self assigns the Allocate Case Roles and Create Bundle task", async ({
        prepareCaseForHearingSteps }) => {

        test.slow();
        await prepareCaseForHearingSteps.verifyRegionalCenterAdminCanViewTheAllocateCaseRolesAndCreateBundleTask(caseId);
    });

    test("Allocate Case Roles and Create Bundle task is cancelled automatically when case is struck out", async ({
        prepareCaseForHearingSteps}) => {

        test.slow();
        await prepareCaseForHearingSteps.verifyAllocateCaseRolesAndCreateBundleIsCancelledAutomaticallyWhenTheCaseIsStruckOut(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});