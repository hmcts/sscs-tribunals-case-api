import { test } from "../lib/steps.factory";
import createCaseBasedOnCaseType from "../api/client/sscs/factory/appeal.type.factory";
import performAppealDormantOnCase from "../api/client/sscs/appeal.event";


test.describe.serial('WA - Supplementary Response - Action Unprocessed Correspondence CTSC task initiation and completion tests', {
    tag: '@work-allocation'
}, async () => {

    let caseId : string;

    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('As a CTSC allocator, allocate case to CTSC Admin', async ({
        supplementaryResponseSteps }) => {

        await supplementaryResponseSteps.allocateCaseToCtscUser(caseId);
    });

    test('As a DWP user, provide supplementary response', async ({
        supplementaryResponseSteps }) => {

        test.slow();
        await supplementaryResponseSteps.performSupplementaryResponse(caseId);
    });

    test('CTSC Admin as allocated case worker, views the Action Unprocessed Correspondence task', async ({
        supplementaryResponseSteps }) => {

        test.slow();
        await supplementaryResponseSteps.verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test('CTSC Admin as allocated case worker, completes the Action Unprocessed Correspondence task', async ({
        supplementaryResponseSteps }) => {

        test.slow();
        await supplementaryResponseSteps.verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test.afterAll('Set case state to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});

test.describe.serial('WA - Supplementary Response - Action Unprocessed Correspondence CTSC task cancellation tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;
    
    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('As a CTSC allocator, allocate case to CTSC Admin', async ({
        supplementaryResponseSteps }) => {

        await supplementaryResponseSteps.allocateCaseToCtscUser(caseId);
    });

    test('As a DWP user, provide supplementary response', async ({
        supplementaryResponseSteps }) => {

        test.slow();
        await supplementaryResponseSteps.performSupplementaryResponse(caseId);
    });

    test('CTSC Admin as allocated case worker, views the Action Unprocessed Correspondence task', async ({
        supplementaryResponseSteps }) => {

        test.slow();
        await supplementaryResponseSteps.verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test('CTSC Admin as allocated case worker, cancels the Action Unprocessed Correspondence CTSC task manually', async ({
        supplementaryResponseSteps}) => {

        test.slow();
        await supplementaryResponseSteps.verifyActionUnprocessedCorrespondenceTaskCanBeCancelledManuallyByAllocatedCtscAdmin(caseId);
    });

    test.afterAll('Set case state to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});