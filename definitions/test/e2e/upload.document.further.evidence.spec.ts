import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import performAppealDormantOnCase from '../api/client/sscs/appeal.event';


test.describe.serial('WA - Upload document FE - Action Unprocessed Correspondence CTSC task initiation and completion tests', {
    tag: '@work-allocation'
}, async () => {

    let caseId : string;

    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('As a CTSC Case allocator, allocate task to CTSC Admin', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        await uploadDocumentFurtherEvidenceSteps.allocateCaseToCtscUser(caseId);
    });

    test('As a CTSC Admin, upload document further evidence', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, false);
    });

    test('CTSC Admin as allocated case worker, views the Action Unprocessed Correspondence CTSC task', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test('CTSC Admin as allocated case worker, completes the Action Unprocessed Correspondence CTSC task', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test.afterAll('Case has to be set to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});


test.describe.serial('WA - Upload document FE - Action Unprocessed Correspondence CTSC task cancellation', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;

    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('As a CTSC Admin, upload document further evidence', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId);
    });

    test('CTSC Admin as allocated case worker, views the Action Unprocessed Correspondence CTSC task', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedActionUnprocessedCorrespondenceTask(caseId);
    });

    test('CTSC Admin cancels the unassigned Action Unprocessed Correspondence CTSC task manually', async ({
        uploadDocumentFurtherEvidenceSteps}) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyUnassignedActionUnprocessedCorrespondenceTaskCanBeCancelledManuallyByCtscAdmin(caseId);
    });

    test.afterAll('Case has to be set to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});


test.describe.serial('WA - Review Bi-Lingual Document CTSC task initiation and completion tests', {
    tag: '@work-allocation'
}, async () => {

    let caseId : string;

    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('Allocate case to CTSC Admin and update language preference to Welsh', async ({
        uploadDocumentFurtherEvidenceSteps,
        updateLanguagePreferenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.allocateCaseToCtscUser(caseId);
        await updateLanguagePreferenceSteps.performUpdateLanguagePreference(caseId, false);
    });

    test('As a CTSC Admin, upload document further evidence', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, false);
    });

    test('As a CTSC Admin, view the Review Bi-Lingual Document CTSC task', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminAsAllocatedCaseWorkerCanViewTheAutomaticallyAssignedReviewBilingualDocumentTask(caseId);
    });

    test('CTSC Admin as allocated case worker, completes the Review Bi-Lingual Document CTSC task', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminAsAllocatedCaseWorkerCanCompleteTheAssignedReviewBilingualDocumentTask(caseId);
    });

    test.afterAll('Case has to be set to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});


test.describe.serial('WA - Review Bi-Lingual Document CTSC task cancellation tests', {
    tag: '@work-allocation'
}, async() => {

    let caseId : string;
    
    test.beforeAll('Create case', async () => {
        caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('As a CTSC Admin, update language preference to Welsh', async ({
        updateLanguagePreferenceSteps }) => {

        await updateLanguagePreferenceSteps.performUpdateLanguagePreference(caseId, true);
    });

    test('As a CTSC Admin, upload document further evidence', async ({
        uploadDocumentFurtherEvidenceSteps }) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.performUploadDocumentFurtherEvidence(caseId, false);
    });

    test('CTSC Admin views Review Bi-Lingual Document CTSC task and cancels Welsh translations', async ({
        uploadDocumentFurtherEvidenceSteps}) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyCtscAdminCanViewBilingualDocumentTaskAndCancelWelshTranslations(caseId);
    });

    test('CTSC Admin verifies Review Bi-Lingual Document CTSC task is removed from the tasks list', async ({
        uploadDocumentFurtherEvidenceSteps}) => {

        test.slow();
        await uploadDocumentFurtherEvidenceSteps.verifyUnassignedReviewBilingualDocumentTaskIsRemovedFromTheTasksList(caseId);
    });

    test.afterAll('Case has to be set to Dormant', async () => {
        await performAppealDormantOnCase(caseId);
    });
});