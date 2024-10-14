import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

test.describe('Reinstatement request tests', {tag: ['@preview-regression', '@nightly-pipeline']}, async() => {
    let caseId : string;

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Grant - Reinstatement request", async ({ reinstatementSteps }) => {
        await reinstatementSteps.requestAndGrantAnReinstatement(caseId);
    });

    test("Refuse - Reinstatement request", async ({ reinstatementSteps }) => {
        await reinstatementSteps.requestAndRefuseAnReinstatement(caseId);
    });

    test.afterAll("Case has to be set to Dormant",async () => {
        // await performAppealDormantOnCase(caseId);
    });
}),



test.describe.serial('WA - Review Reinstatement Request Judge task initiation and completion tests by Salaried Judge', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Grant Reinstatement request", async ({ reinstatementSteps }) => {
        test.slow();
        await reinstatementSteps.requestAndGrantAnReinstatement(caseId);
    });

    test("As a Salaried Judge, view the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test("As a Salaried Judge, complete the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanCompleteTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})


test.describe.serial('WA - Review Reinstatement Request Judge task initiation and completion tests by Fee-Paid Judge', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Refuse Reinstatement request", async ({ reinstatementSteps }) => {
        test.slow();
        await reinstatementSteps.requestAndRefuseAnReinstatement(caseId);
    });

    test("As a Fee-Paid Judge, view the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifyFeePaidJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test("As a Fee-Paid Judge, complete the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifyFeePaidJudgeCanCompleteTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})


test.describe.serial('WA - Review Reinstatement Request Judge task - Reassign to TCW tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Refuse Reinstatement request", async ({ reinstatementSteps }) => {
        test.slow();
        await reinstatementSteps.requestAndRefuseAnReinstatement(caseId);
    });

    test("As a Judge, view the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test("As a Judge, assign the Review Reinstatement Request task to Tribunal Caseworker", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanReassignTheReviewReinstatementRequestTaskToTcw(caseId);
    });

    test("As a Tribunal Caseworker, complete the Review Reinstatement Request task ", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifyTcwCanCompleteTheAssignedReviewReinstatementRequestTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})

test.describe.serial('WA - Judge completes Review Reinstatement Request task manually', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Grant Reinstatement request", async ({ reinstatementSteps }) => {
        test.slow();
        await reinstatementSteps.requestAndGrantAnReinstatement(caseId);
    });

    test("As a Judge, view the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanViewTheUnassignedReviewReinstatementRequestTask(caseId);
    });

    test("As a Judge, complete the Review Reinstatement Request task manually", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanCompleteTheReviewReinstatementRequestTaskManually(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
})

test.describe.serial('WA - Review Reinstatement Request Judge task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll("Create case", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Refuse Reinstatement request", async ({ reinstatementSteps }) => {
        test.slow();
        await reinstatementSteps.requestAndRefuseAnReinstatement(caseId);
    });

    test("As a Judge, view the Review Reinstatement Request - Judge task", async ({
        reinstatementSteps }) => {

        test.slow();
        await reinstatementSteps.verifySalariedJudgeCanViewAndSelfAssignTheReviewReinstatementRequestTask(caseId);
    });

    test("Review Reinstatement Request - Judge task is cancelled automatically when case is void", async ({
        reinstatementSteps}) => {

        test.slow();
        await reinstatementSteps.verifyReviewReinstatementRequestTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});
