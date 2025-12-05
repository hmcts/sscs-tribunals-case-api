import { test } from '../lib/steps.factory';
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';

let caseId: string;

test.describe('Update listing requirement test', () => {

    test.beforeEach('Case has to be Created', async () => {
      caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('PIP Case - Update & Verify Joh Tiers/Duration', { tag: '@nightly-pipeline' }, async ({ updateListingRequirementSteps }) => {
        await updateListingRequirementSteps.performUploadResponse(caseId);
        await updateListingRequirementSteps.updateAndVerifyJoHTiers();
    });
});
