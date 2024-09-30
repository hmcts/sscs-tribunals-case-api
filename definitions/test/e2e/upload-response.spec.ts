import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";

let caseId : string;

test.describe('Upload response tests', {tag: ['@nightly-pipeline']}, async() => {

    test("As a caseworker review response submitted with any further info", async ({ uploadResponseSteps }) => {
        test.slow();
        await uploadResponseSteps.performUploadResponseWithFurtherInfoOnAPIPAndReviewResponse();
    });
    
    test("As a caseworker review response submitted without any further info", async ({ uploadResponseSteps }) => {
        test.slow();
        await uploadResponseSteps.performUploadResponseWithoutFurtherInfoOnATaxCredit();
    });
    
    test("As a caseworker review response submitted for an UC case", async({ uploadResponseSteps }) => {
        test.slow();
        let ucCaseId = await createCaseBasedOnCaseType("UC");
        await uploadResponseSteps.performUploadResponseOnAUniversalCredit(ucCaseId);
    });
})

test.describe('Upload response tests for PHE workflow', { tag: '@nightly-pipeline' }, async() => {

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a caseworker review PHE response submitted without any further info", async ({ uploadResponseSteps, reviewPHESteps }) => {
        test.slow();
        await uploadResponseSteps.performUploadResponseWithPHEOnAPIPAndReviewResponse(caseId);
        await reviewPHESteps.grantAnPHERequest(caseId);
        await reviewPHESteps.refuseAnPHERequest(caseId);
    });
    
})

test.describe('Upload response tests for UCB workflow', { tag: '@nightly-pipeline' }, async() => {

    test.beforeEach("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a caseworker review UCB response submitted without any further info", async ({ uploadResponseSteps, updateUCBSteps }) => {
        test.slow();
        await uploadResponseSteps.performUploadResponseWithUCBOnAPIP(caseId);
        await updateUCBSteps.verifyUpdatedUCBOption();
    });
    
})


test.describe.serial('Error scenarios', { tag: '@nightly-pipeline' }, async () => {

    test("Verify Upload response error scenario", async({ uploadResponseSteps }) => {
        test.slow();
        await uploadResponseSteps.verifyErrorsScenariosInUploadResponse();
    });

    test("Verify Upload response PHME error scenario", async({ uploadResponseSteps }) => {
        test.slow();
        await uploadResponseSteps.verifyPHMEErrorsScenariosInUploadResponse();
    });

    test("Verify Upload response Issue code error scenario", async({ uploadResponseSteps }) => {
        test.slow();
        await uploadResponseSteps.verifyIssueCodeErrorsScenariosInUploadResponse();
    });
})

test.describe.serial('WA - Review FTA response CTSC work allocation task initiation and completion tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;
    
    test.beforeAll("Case has to be Created",async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("As a CSTC Admin with case allocator role, view Review FTA response CTSC task", async ({
        uploadResponseSteps }) => {

        test.slow();
        await uploadResponseSteps.verifyCtscAdminWithCaseAllocatorRoleCanViewReviewFTAResponseTask(caseId);
    });

    test("As a CSTC Administrator without case allocator role, view and complete Review FTA Response CTSC task", async ({
        uploadResponseSteps }) => {

        test.slow();
        await uploadResponseSteps.verifyCtscAdminWithoutCaseAllocatorRoleCanCompleteReviewFTAResponseTask(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });

})

test.describe('WA - Review FTA Response CTSC task automatic cancellation when case is void', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;
    
    test.beforeAll("Case has to be Created", async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test("Review FTA Response task is cancelled automatically when case is void", async ({
        uploadResponseSteps}) => {

        test.slow();
        await uploadResponseSteps.verifyReviewFTAResponseTaskIsCancelledAutomaticallyWhenTheCaseIsVoid(caseId);
    });

    test.afterAll("Case has to be set to Dormant", async () => {
        await performAppealDormantOnCase(caseId);
    });
});

test.afterEach(async ({ page }, testInfo) => {
    if (testInfo.status !== testInfo.expectedStatus) {
        // Get a unique place for the screenshot.
        const screenshotPath = testInfo.outputPath(`failure.png`);
        // Add it to the report.
        testInfo.attachments.push({ name: 'screenshot', path: screenshotPath, contentType: 'image/png' });
        // Take the screenshot itself.
        await page.screenshot({ path: screenshotPath, timeout: 5000 });
    }
});

