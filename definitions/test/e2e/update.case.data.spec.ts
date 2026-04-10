import { test } from '../lib/steps.factory';
import { credentials } from "../config/config";
import createCaseBasedOnCaseType from '../api/client/sscs/factory/appeal.type.factory';
import updateData from "../pages/content/update.casedata_en.json";

let caseId: string;

test.describe('Update case data test', async () => {

    test.beforeEach('Case has to be Created', async () => {
      caseId = await createCaseBasedOnCaseType('PIP');
    });

    test('PIP Case - Update', { tag: '@nightly-pipeline' }, async ({ createUpdateToCaseDataSteps }) => {
        await createUpdateToCaseDataSteps.updateToNonIBCCaseDataEvent(caseId, updateData);
        await createUpdateToCaseDataSteps.verifyNonIBCAppealDetailsTab(updateData);
    });
});
