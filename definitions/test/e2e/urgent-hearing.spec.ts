import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";


test.describe("Urgent hearing test",  async() => {

    let caseId : string;

    test.beforeEach("Case has to be Created", {tag: ['@regression', '@nightly-pipeline']}, async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });
    
    test("Grant - Urgent hearing request", {tag: ['@regression', '@nightly-pipeline']}, async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAndGrantAnUrgentHearing(caseId);
    });
    
    test("Refuse - Urgent hearing request", {tag: ['@regression', '@nightly-pipeline']},async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAndRefuseAnUrgentHearing(caseId);
    });

    test("Welsh - Urgent hearing request", {tag: ['@todo-test-not-working', '@nightly-pipeline']}, async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearingForAWelshCase();
    });
    
    test("Error scenario - Upload encrypted file in Action further evidence event", {tag: ['@preview-regression', '@nightly-pipeline']}, async({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.uploadEncryptedFiles(caseId);
    });
    
     test.afterAll("Case has to be set to Dormant",async () => {
        // await performAppealDormantOnCase(caseId);
     });
});

test.describe.serial('WA - Review Urgent Hearing Request - Judge task initiation and completion tests by Interlocutory Judge', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.allocateCaseToInterlocutoryJudge(caseId);
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As an Interlocutory Judge, view the auto assigned Review Urgent Hearing Request task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifyInterlocutoryJudgeCanViewTheAssignedReviewUrgentHearingRequestTask(caseId);
    });

    test("As an Interlocutory Judge, complete the auto assigned Review Urgent Hearing Request task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifyInterlocutoryJudgeCanCompleteTheAssignedReviewUrgentHearingRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})

test.describe.serial('WA - Review Urgent Hearing Request - Judge task initiation and completion tests by Salaried Judge', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As a Salaried Judge, view the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test("As a Salaried Judge, complete the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanCompleteTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})


test.describe.serial('WA - Review Urgent Hearing Request - Judge task initiation and completion tests by Fee-Paid Judge', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As a Fee-Paid Judge, view the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifyFeePaidJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test("As a Fee-Paid Judge, complete the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifyFeePaidJudgeCanCompleteTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})


test.describe.serial('WA - Review Urgent Hearing Request Judge task - Reassign to TCW tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As a Judge, view the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test("As a Judge, assign the Review Urgent Hearing Request task to Tribunal Caseworker", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanReassignTheReviewUrgentHearingRequestTaskToTcw(caseId);
    });

    test("As a Tribunal Caseworker, complete the Review Urgent Hearing Request task ", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifyTcwCanCompleteTheAssignedReviewUrgentHearingRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})

test.describe.serial('WA - Judge completes Review Urgent Hearing Request task manually', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As a Judge, view the Review Urgent Hearing Request task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanViewTheUnassignedReviewUrgentHearingRequestTask(caseId);
    });

    test("As a Judge, complete the Review Urgent Hearing Request task manually", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanCompleteTheReviewUrgentHearingRequestTaskManually(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})

test.describe.serial('WA - Review Urgent Hearing Request Judge task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Request an urgent hearing", async ({ urgentHearingSteps }) => {
        test.slow();
        await urgentHearingSteps.requestAnUrgentHearing(caseId);
    });

    test("As a Judge, view the Review Urgent Hearing Request - Judge task", async ({
        urgentHearingSteps }) => {

        test.slow();
        await urgentHearingSteps.verifySalariedJudgeCanViewAndSelfAssignTheReviewUrgentHearingRequestTask(caseId);
    });

    test("Review Urgent Hearing Request - Judge task is cancelled automatically when case is void", async ({
        urgentHearingSteps}) => {

        test.slow();
        await urgentHearingSteps.verifyReviewUrgentHearingRequestTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});
